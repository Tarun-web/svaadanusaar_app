package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the ongoing protein adaptation journey for a user.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Determine the starting protein target from the long-term target
 *       and the user's current intake.</li>
 *   <li>Decide the current recommended target at any given moment.</li>
 *   <li>Progress toward the long-term target in measured increments.</li>
 *   <li>Hold the target when GI tolerance is poor.</li>
 *   <li>Persist the adaptation state in {@link ProteinAdaptationProfile}.</li>
 *   <li>Surface structured warnings for the user and the diet engine.</li>
 * </ul>
 *
 * <h3>NOT responsible for</h3>
 * <ul>
 *   <li>Calculating the long-term target — that is done by
 *       {@code ProteinCalculator}.</li>
 *   <li>Medical diagnosis or GI diagnosis.</li>
 *   <li>Generating meals or recipes.</li>
 *   <li>Modifying {@code HealthProfile} or {@code NutritionTarget}
 *       directly — the caller uses {@link ProteinAdaptationResult}
 *       to update those entities.</li>
 * </ul>
 *
 * <h3>Business rules (centralised)</h3>
 * <p>All ramp-up constants are defined as named {@code static final}
 * fields in this class so they can be adjusted in one place.</p>
 *
 * <h3>Progression gate</h3>
 * <p>Before increasing the target the following must ALL be true:</p>
 * <ol>
 *   <li>tolerance == GOOD or UNKNOWN</li>
 *   <li>days at current target &ge; {@link #MIN_DAYS_BEFORE_INCREMENT}</li>
 *   <li>current target &lt; long-term target</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class ProteinAdaptationService {

    // ================================================================
    // Business rules — change these to adjust the ramp-up policy
    // ================================================================

    /** Default ramp-up increment per step (grams). */
    public static final BigDecimal DEFAULT_INCREMENT_GRAMS = new BigDecimal("10");

    /**
     * Minimum days the user must spend at the current target before
     * the next increment is allowed.
     */
    public static final int MIN_DAYS_BEFORE_INCREMENT = 7;

    // Starting-target gap thresholds (grams)
    public static final BigDecimal GAP_SMALL  = new BigDecimal("15");
    public static final BigDecimal GAP_MEDIUM = new BigDecimal("30");
    public static final BigDecimal GAP_LARGE  = new BigDecimal("50");

    // Starting-target reductions relative to long-term target (grams)
    public static final BigDecimal REDUCTION_NONE   = BigDecimal.ZERO;
    public static final BigDecimal REDUCTION_SMALL  = new BigDecimal("10");
    public static final BigDecimal REDUCTION_MEDIUM = new BigDecimal("20");
    public static final BigDecimal REDUCTION_LARGE  = new BigDecimal("25");

    // ================================================================

    private final ProteinAdaptationProfileRepository repository;

    // ================================================================
    // initialise()
    // ================================================================

    /**
     * Creates or resets the {@link ProteinAdaptationProfile} for the
     * given health profile, using the provided long-term target result
     * and the user's current protein intake (may be {@code null}).
     *
     * <p>If a profile already exists it is updated in-place; this
     * allows re-calculation when the user's goal or weight changes.</p>
     *
     * @param healthProfile     the user's health profile
     * @param proteinTargetResult the result from {@code ProteinCalculator}
     * @param currentProteinGrams the user's current intake — {@code null} if unknown
     * @return the updated {@link ProteinAdaptationProfile} (already persisted)
     */
    @Transactional
    public ProteinAdaptationProfile initialise(
            HealthProfile healthProfile,
            ProteinTargetResult proteinTargetResult,
            BigDecimal currentProteinGrams) {

        BigDecimal longTermTarget = proteinTargetResult.longTermTargetGrams();

        // Determine starting target
        BigDecimal startingTarget = calculateStartingTarget(
                currentProteinGrams, longTermTarget);

        // Determine initial stage
        ProteinAdaptationStage stage = determineInitialStage(
                currentProteinGrams, startingTarget, longTermTarget);

        // Load or create the profile
        ProteinAdaptationProfile profile = repository
                .findByHealthProfile(healthProfile)
                .orElse(ProteinAdaptationProfile.builder()
                        .healthProfile(healthProfile)
                        .build());

        profile.setCurrentProteinGrams(currentProteinGrams);
        profile.setLongTermTargetGrams(longTermTarget);
        profile.setCurrentTargetGrams(startingTarget);
        profile.setAdaptationStage(stage);
        profile.setDaysAtCurrentTarget(0);
        profile.setToleranceStatus(ProteinToleranceStatus.UNKNOWN);
        profile.setLastTargetChangeAt(LocalDate.now());

        return repository.save(profile);
    }

    // ================================================================
    // evaluate()
    // ================================================================

    /**
     * Evaluates the current adaptation state and — if progression is
     * permitted — advances the target by one increment.
     *
     * <p>This method is the heart of the ramp-up logic.  It is typically
     * called daily by a scheduled job or whenever the user logs food/symptoms.</p>
     *
     * <p>The returned {@link ProteinAdaptationResult} contains the
     * current recommended target (write to
     * {@code NutritionTarget.proteinGrams}) and any warnings.</p>
     *
     * @param profile the existing adaptation profile to evaluate
     * @return a result describing the current state and any next action
     */
    @Transactional
    public ProteinAdaptationResult evaluate(ProteinAdaptationProfile profile) {

        List<ProteinWarning> warnings = new ArrayList<>();

        BigDecimal longTermTarget  = profile.getLongTermTargetGrams();
        BigDecimal currentTarget   = profile.getCurrentTargetGrams();
        ProteinToleranceStatus tol = profile.getToleranceStatus();

        // ── Already at target ─────────────────────────────────────────
        if (currentTarget.compareTo(longTermTarget) >= 0) {
            profile.setAdaptationStage(ProteinAdaptationStage.TARGET_REACHED);
            repository.save(profile);

            warnings.add(new ProteinWarning(
                    ProteinWarningCode.TARGET_ALREADY_REACHED,
                    "Long-term protein target has been reached. No further ramp-up needed."
            ));

            return buildResult(profile, false, null, warnings);
        }

        // ── Tolerance blocks progression ──────────────────────────────
        if (tol == ProteinToleranceStatus.SIGNIFICANT_ISSUES) {
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.SIGNIFICANT_GI_SYMPTOMS,
                    "Significant GI symptoms have been reported. Protein target is being held. " +
                    "Please review protein sources, meal distribution, fibre intake, and fluid intake. " +
                    "If symptoms persist, consult a registered dietitian or medical professional."
            ));
            return buildResult(profile, true, null, warnings);
        }

        if (tol == ProteinToleranceStatus.MILD_ISSUES) {
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.TOLERANCE_HOLD,
                    "Mild GI symptoms reported. Protein target is being held at " +
                    currentTarget.toPlainString() + " g/day. " +
                    "Consider reviewing protein sources and distribution across meals."
            ));
            return buildResult(profile, true, null, warnings);
        }

        // ── Minimum adaptation period not yet elapsed ─────────────────
        int daysHeld = profile.getDaysAtCurrentTarget();
        if (daysHeld < MIN_DAYS_BEFORE_INCREMENT) {
            LocalDate nextEligible = profile.getLastTargetChangeAt() != null
                    ? profile.getLastTargetChangeAt().plusDays(MIN_DAYS_BEFORE_INCREMENT)
                    : LocalDate.now().plusDays(MIN_DAYS_BEFORE_INCREMENT - daysHeld);

            return buildResult(profile, true, nextEligible, warnings);
        }

        // ── Advance the target by one increment ───────────────────────
        BigDecimal newTarget = currentTarget.add(DEFAULT_INCREMENT_GRAMS);

        // Clamp to long-term target — never overshoot
        if (newTarget.compareTo(longTermTarget) >= 0) {
            newTarget = longTermTarget;
            profile.setAdaptationStage(ProteinAdaptationStage.TARGET_REACHED);
        } else {
            profile.setAdaptationStage(ProteinAdaptationStage.ADAPTING);
        }

        profile.setCurrentTargetGrams(newTarget);
        profile.setDaysAtCurrentTarget(0);
        profile.setToleranceStatus(ProteinToleranceStatus.UNKNOWN);
        profile.setLastTargetChangeAt(LocalDate.now());

        repository.save(profile);

        boolean rampUpStillRequired =
                newTarget.compareTo(longTermTarget) < 0;

        return buildResult(
                profile,
                rampUpStillRequired,
                rampUpStillRequired
                        ? LocalDate.now().plusDays(MIN_DAYS_BEFORE_INCREMENT)
                        : null,
                warnings
        );
    }

    // ================================================================
    // recordTolerance()
    // ================================================================

    /**
     * Records updated GI tolerance for the user's current protein target.
     *
     * <p>This is called when the user (or the diet engine) reports symptoms.
     * The tolerance status is persisted and will gate any future progression.</p>
     *
     * @param profile         the existing adaptation profile
     * @param toleranceStatus the new tolerance status
     * @return the updated profile
     */
    @Transactional
    public ProteinAdaptationProfile recordTolerance(
            ProteinAdaptationProfile profile,
            ProteinToleranceStatus toleranceStatus) {

        profile.setToleranceStatus(toleranceStatus);
        return repository.save(profile);
    }

    // ================================================================
    // incrementDaysAtCurrentTarget()
    // ================================================================

    /**
     * Increments the day counter for the current protein target.
     *
     * <p>Intended to be called by a daily scheduled job.
     * Only increments when the stage is {@code STARTING} or
     * {@code ADAPTING} — stops counting once the target is reached.</p>
     *
     * @param profile the existing adaptation profile
     * @return the updated profile
     */
    @Transactional
    public ProteinAdaptationProfile incrementDaysAtCurrentTarget(
            ProteinAdaptationProfile profile) {

        if (profile.getAdaptationStage() == ProteinAdaptationStage.STARTING
                || profile.getAdaptationStage() == ProteinAdaptationStage.ADAPTING) {

            profile.setDaysAtCurrentTarget(
                    profile.getDaysAtCurrentTarget() + 1
            );
            return repository.save(profile);
        }
        return profile;
    }

    // ================================================================
    // toResult()
    // ================================================================

    /**
     * Converts an existing {@link ProteinAdaptationProfile} to a
     * {@link ProteinAdaptationResult} without modifying the profile.
     *
     * <p>Useful for read-only queries (e.g. fetching the current state
     * for the API response without triggering a progression check).</p>
     */
    public ProteinAdaptationResult toResult(ProteinAdaptationProfile profile) {

        List<ProteinWarning> warnings = new ArrayList<>();

        boolean rampUp = profile.getAdaptationStage() != ProteinAdaptationStage.TARGET_REACHED
                && profile.getAdaptationStage() != ProteinAdaptationStage.NOT_STARTED;

        LocalDate nextEligible = null;
        ProteinToleranceStatus tol2 = profile.getToleranceStatus();
        if (rampUp
                && profile.getLastTargetChangeAt() != null
                && (tol2 == ProteinToleranceStatus.GOOD
                        || tol2 == ProteinToleranceStatus.UNKNOWN)) {

            int remaining = MIN_DAYS_BEFORE_INCREMENT - profile.getDaysAtCurrentTarget();
            if (remaining > 0) {
                nextEligible = LocalDate.now().plusDays(remaining);
            }
        }

        if (profile.getCurrentProteinGrams() == null) {
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.CURRENT_INTAKE_UNKNOWN,
                    "Current protein intake is unknown. " +
                    "The adaptation schedule will be refined once intake is estimated " +
                    "from food logs."
            ));
        }

        return buildResult(profile, rampUp, nextEligible, warnings);
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Calculates the starting target based on the gap between
     * current intake and long-term target.
     *
     * <p>If current intake is unknown, the long-term target is used
     * directly. No value is invented.</p>
     *
     * <pre>
     *   gap &le; 15 g      → start = longTerm  (no reduction)
     *   15 &lt; gap &le; 30 → start = longTerm - 10 g
     *   30 &lt; gap &le; 50 → start = longTerm - 20 g
     *   gap &gt; 50 g       → start = longTerm - 25 g
     * </pre>
     *
     * <p>The starting target is always &ge; current intake.</p>
     */
    public static BigDecimal calculateStartingTarget(
            BigDecimal currentProteinGrams,
            BigDecimal longTermTarget) {

        if (currentProteinGrams == null) {
            // Cannot calculate a ramp — start at the long-term target
            return longTermTarget;
        }

        BigDecimal gap = longTermTarget.subtract(currentProteinGrams);

        // User already meets or exceeds the target
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return longTermTarget;
        }

        BigDecimal reduction;
        if (gap.compareTo(GAP_SMALL) <= 0) {
            reduction = REDUCTION_NONE;
        } else if (gap.compareTo(GAP_MEDIUM) <= 0) {
            reduction = REDUCTION_SMALL;
        } else if (gap.compareTo(GAP_LARGE) <= 0) {
            reduction = REDUCTION_MEDIUM;
        } else {
            reduction = REDUCTION_LARGE;
        }

        BigDecimal startingTarget = longTermTarget.subtract(reduction);

        // Starting target must never be below current intake
        if (startingTarget.compareTo(currentProteinGrams) < 0) {
            startingTarget = currentProteinGrams;
        }

        return startingTarget;
    }

    /**
     * Determines the initial adaptation stage.
     */
    private ProteinAdaptationStage determineInitialStage(
            BigDecimal currentProteinGrams,
            BigDecimal startingTarget,
            BigDecimal longTermTarget) {

        if (currentProteinGrams == null) {
            return ProteinAdaptationStage.NOT_STARTED;
        }

        if (startingTarget.compareTo(longTermTarget) >= 0) {
            return ProteinAdaptationStage.TARGET_REACHED;
        }

        return ProteinAdaptationStage.STARTING;
    }

    /**
     * Builds a {@link ProteinAdaptationResult} from the current state
     * of the profile.
     */
    private ProteinAdaptationResult buildResult(
            ProteinAdaptationProfile profile,
            boolean rampUpRequired,
            LocalDate nextEligibleIncreaseDate,
            List<ProteinWarning> warnings) {

        return new ProteinAdaptationResult(
                profile.getCurrentTargetGrams(),
                profile.getLongTermTargetGrams(),
                profile.getCurrentProteinGrams(),
                profile.getAdaptationStage(),
                rampUpRequired,
                nextEligibleIncreaseDate,
                profile.getToleranceStatus(),
                List.copyOf(warnings)
        );
    }
}
