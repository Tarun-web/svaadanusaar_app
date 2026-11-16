package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ScheduledEvaluationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.ProteinCalculator;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /** SQLState for a unique-constraint violation (standard, PostgreSQL included). */
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

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
        boolean firstInsert = profile.getId() == null;

        // The caller only initialises when it saw no row (or a NOT_STARTED
        // one). A row that is already past NOT_STARTED here was initialised by
        // a concurrent request after the caller's read; resetting it would
        // silently destroy that progress, so report a conflict instead.
        if (!firstInsert
                && profile.getAdaptationStage() != null
                && profile.getAdaptationStage() != ProteinAdaptationStage.NOT_STARTED) {
            throw new ObjectOptimisticLockingFailureException(
                    ProteinAdaptationProfile.class, profile.getId());
        }

        profile.setCurrentProteinGrams(currentProteinGrams);
        profile.setLongTermTargetGrams(longTermTarget);
        profile.setCurrentTargetGrams(startingTarget);
        profile.setAdaptationStage(stage);
        profile.setDaysAtCurrentTarget(0);
        profile.setToleranceStatus(ProteinToleranceStatus.UNKNOWN);
        profile.setLastTargetChangeAt(LocalDate.now());

        ProteinAdaptationProfile saved = repository.save(profile);
        if (firstInsert) {
            flushFirstInsert();
        }
        return saved;
    }

    /**
     * Forces the INSERT of a brand-new profile now, inside this method,
     * rather than at commit time where it would escape as a bare driver error.
     *
     * <p>If two first calculations for the same user race, both see no row and
     * both insert; the loser violates the unique {@code health_profile_id}
     * constraint. That loser did nothing wrong and must not receive a 500, and
     * it must never overwrite the winner's row. A rolled-back transaction
     * cannot be retried in place, so the loss is reported as a conflict
     * (HTTP 409 via the existing optimistic-lock mapping); the client's retry
     * then finds the winner's row. Only a unique violation (SQLState 23505) is
     * translated; any other integrity failure propagates unchanged.</p>
     */
    private void flushFirstInsert() {
        try {
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw new ObjectOptimisticLockingFailureException(
                        "Concurrent first-time protein adaptation initialisation", ex);
            }
            throw ex;
        }
    }

    private static boolean isUniqueViolation(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DuplicateKeyException) {
                return true;
            }
            if (t instanceof SQLException sql && UNIQUE_VIOLATION_SQLSTATE.equals(sql.getSQLState())) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    // ================================================================
    // synchroniseLongTermTarget()
    // ================================================================

    /**
     * Brings an existing profile's long-term target in line with a freshly
     * calculated one, without resetting the user's adaptation progress.
     *
     * <p>Rules (applied only when the long-term target actually changed):</p>
     * <ul>
     *   <li>{@code NOT_STARTED} — no ramp is possible, so the current
     *       target simply follows the new long-term target.</li>
     *   <li>current &gt; new long-term — the current target is clamped to
     *       the new long-term target and the stage becomes
     *       {@code TARGET_REACHED}; the day counter restarts because the
     *       target value changed.</li>
     *   <li>current == new long-term — stage becomes {@code TARGET_REACHED}.</li>
     *   <li>{@code TARGET_REACHED} and current &lt; new long-term — stage
     *       becomes {@code ADAPTING} and the day counter restarts (it was
     *       frozen while the target was reached).</li>
     *   <li>otherwise ({@code STARTING}/{@code ADAPTING}, current &lt; new) —
     *       only the long-term target changes.</li>
     * </ul>
     *
     * <p>Tolerance status is never modified. Nothing is written when the
     * long-term target is unchanged.</p>
     *
     * @param profile         the existing adaptation profile
     * @param proteinTargetResult the freshly calculated long-term target
     */
    @Transactional
    public void synchroniseLongTermTarget(
            ProteinAdaptationProfile profile,
            ProteinTargetResult proteinTargetResult) {

        BigDecimal newLongTerm = proteinTargetResult.longTermTargetGrams();

        if (profile.getLongTermTargetGrams().compareTo(newLongTerm) == 0) {
            return;
        }

        BigDecimal current = profile.getCurrentTargetGrams();
        ProteinAdaptationStage stage = profile.getAdaptationStage();

        profile.setLongTermTargetGrams(newLongTerm);

        if (stage == ProteinAdaptationStage.NOT_STARTED) {
            profile.setCurrentTargetGrams(newLongTerm);

        } else if (current.compareTo(newLongTerm) > 0) {
            profile.setCurrentTargetGrams(newLongTerm);
            profile.setAdaptationStage(ProteinAdaptationStage.TARGET_REACHED);
            profile.setDaysAtCurrentTarget(0);
            profile.setLastTargetChangeAt(LocalDate.now());

        } else if (current.compareTo(newLongTerm) == 0) {
            profile.setAdaptationStage(ProteinAdaptationStage.TARGET_REACHED);

        } else if (stage == ProteinAdaptationStage.TARGET_REACHED) {
            profile.setAdaptationStage(ProteinAdaptationStage.ADAPTING);
            profile.setDaysAtCurrentTarget(0);
            profile.setLastTargetChangeAt(LocalDate.now());
        }

        repository.save(profile);
    }

    // ================================================================
    // evaluate()
    // ================================================================

    /**
     * Evaluates the current adaptation state and — if progression is
     * permitted — advances the target by one increment.
     *
     * <p>The user's <em>current</em> medical conditions are read from the
     * profile's health profile, so a condition that was added or removed
     * since the last call is honoured. A profile with no health profile
     * attached is treated as having no conditions.</p>
     *
     * @param profile the existing adaptation profile to evaluate
     * @return a result describing the current state and any next action
     * @see #evaluate(ProteinAdaptationProfile, Collection)
     */
    @Transactional
    public ProteinAdaptationResult evaluate(ProteinAdaptationProfile profile) {
        return evaluate(profile, resolveMedicalConditions(profile));
    }

    /**
     * Evaluates the current adaptation state using medical conditions the
     * caller has already loaded (for example in one batch query).
     *
     * <p><strong>Clinical-review exclusion:</strong> when any condition
     * requires clinical review under
     * {@link ProteinCalculator#requiresClinicalReview} (currently kidney or
     * liver disease), the profile is <em>not</em> progressed. No field is
     * changed and nothing is saved; the result reports the existing state
     * with {@code requiresClinicalReview = true}.</p>
     *
     * <p>This method is the heart of the ramp-up logic.  It is typically
     * called daily by a scheduled job or whenever the user logs food/symptoms.</p>
     *
     * <p>The returned {@link ProteinAdaptationResult} contains the
     * current recommended target (write to
     * {@code NutritionTarget.proteinGrams}) and any warnings.</p>
     *
     * @param profile           the existing adaptation profile to evaluate
     * @param medicalConditions the user's current conditions; {@code null}
     *                          or empty means none
     * @return a result describing the current state and any next action
     */
    @Transactional
    public ProteinAdaptationResult evaluate(
            ProteinAdaptationProfile profile,
            Collection<MedicalCondition> medicalConditions) {

        // ── Clinical review: never progress automatically ─────────────
        if (ProteinCalculator.anyRequireClinicalReview(medicalConditions)) {
            return buildClinicalReviewBlockedResult(profile);
        }

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
        repository.save(profile);
        return advanceTarget(profile, LocalDate.now(), warnings);
    }

    /**
     * Raises the current target by one increment, clamped to the long-term
     * target, and resets the day counter. Mutates {@code profile} only; the
     * caller is responsible for saving it.
     *
     * <p>Shared by {@link #evaluate} and {@link #evaluateScheduled} so the
     * progression rule exists in exactly one place.</p>
     */
    private ProteinAdaptationResult advanceTarget(
            ProteinAdaptationProfile profile,
            LocalDate today,
            List<ProteinWarning> warnings) {

        BigDecimal longTermTarget = profile.getLongTermTargetGrams();
        BigDecimal newTarget = profile.getCurrentTargetGrams()
                .add(DEFAULT_INCREMENT_GRAMS);

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
        profile.setLastTargetChangeAt(today);

        boolean rampUpStillRequired =
                newTarget.compareTo(longTermTarget) < 0;

        return buildResult(
                profile,
                rampUpStillRequired,
                rampUpStillRequired
                        ? today.plusDays(MIN_DAYS_BEFORE_INCREMENT)
                        : null,
                warnings
        );
    }

    // ================================================================
    // evaluateScheduled()
    // ================================================================

    /**
     * One idempotent tick of the scheduled ramp-up process.
     *
     * <p>Unlike {@link #evaluate}, which is a <em>manual</em> evaluation that
     * never counts days, this method counts at most one day per profile per
     * calendar date and advances the target when the minimum period has
     * elapsed.</p>
     *
     * <h3>Order of checks</h3>
     * <ol>
     *   <li>{@code lastEvaluatedOn >= today} → {@code ALREADY_EVALUATED_TODAY};
     *       nothing changes. A future date is treated the same way, so a clock
     *       correction can never double-count.</li>
     *   <li>Stage is {@code NOT_STARTED} or {@code TARGET_REACHED} →
     *       {@code SKIPPED_NOT_RAMPING}; nothing changes.</li>
     *   <li>Clinical review required (see
     *       {@link ProteinCalculator#anyRequireClinicalReview}) →
     *       {@code SKIPPED_CLINICAL_REVIEW}; nothing is written, so the
     *       target, stage, tolerance, counter and marker are preserved.</li>
     *   <li>Tolerance {@code MILD_ISSUES} or {@code SIGNIFICANT_ISSUES} →
     *       {@code HELD_TOLERANCE}; no day is counted and nothing is
     *       written.</li>
     *   <li>Otherwise one day is counted and {@code lastEvaluatedOn} is set.
     *       When the counter reaches {@link #MIN_DAYS_BEFORE_INCREMENT} the
     *       target advances by one step (never beyond the long-term
     *       target).</li>
     * </ol>
     *
     * <h3>Atomicity and concurrency</h3>
     * <p>The counter, the marker and any target change are written by a
     * single {@code save} inside one transaction. {@code BaseEntity}'s
     * {@code @Version} makes a concurrent tick for the same profile fail
     * with an optimistic-locking exception instead of counting twice; the
     * caller should treat that as "already handled".</p>
     *
     * @param profile           the profile to tick
     * @param medicalConditions the user's current conditions, normally from
     *                          a batch lookup; {@code null} means none
     * @param today             the calendar date being processed
     * @return the outcome and the profile's state afterwards
     */
    @Transactional
    public ScheduledEvaluationResult evaluateScheduled(
            ProteinAdaptationProfile profile,
            Collection<MedicalCondition> medicalConditions,
            LocalDate today) {

        return evaluateScheduled(profile, medicalConditions, today,
                resolveLatestReportedIntake(profile));
    }

    /**
     * Same as {@link #evaluateScheduled(ProteinAdaptationProfile, Collection, LocalDate)}
     * with the user's latest reported intake supplied by the caller.
     *
     * <p>Use this overload when the profile is detached (for example loaded by
     * a batch query): the profile's lazy {@code healthProfile} is then never
     * touched. The intake only affects the warnings in the returned result;
     * it never changes the stored baseline, target, stage or counter.</p>
     *
     * @param latestReportedIntakeGrams the latest user-reported intake, or
     *                                  {@code null} when unknown
     */
    @Transactional
    public ScheduledEvaluationResult evaluateScheduled(
            ProteinAdaptationProfile profile,
            Collection<MedicalCondition> medicalConditions,
            LocalDate today,
            BigDecimal latestReportedIntakeGrams) {

        // 1. Already evaluated today (or marker is in the future)
        LocalDate lastEvaluatedOn = profile.getLastEvaluatedOn();
        if (lastEvaluatedOn != null && !lastEvaluatedOn.isBefore(today)) {
            return scheduledResult(
                    ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY,
                    toResult(profile, latestReportedIntakeGrams));
        }

        // 2. Nothing to ramp
        ProteinAdaptationStage stage = profile.getAdaptationStage();
        if (stage != ProteinAdaptationStage.STARTING
                && stage != ProteinAdaptationStage.ADAPTING) {
            return scheduledResult(
                    ScheduledEvaluationOutcome.SKIPPED_NOT_RAMPING,
                    toResult(profile, latestReportedIntakeGrams));
        }

        // 3. Clinical review — existing guard, nothing is written
        if (ProteinCalculator.anyRequireClinicalReview(medicalConditions)) {
            return scheduledResult(
                    ScheduledEvaluationOutcome.SKIPPED_CLINICAL_REVIEW,
                    buildClinicalReviewBlockedResult(profile));
        }

        // 4. Tolerance hold — do not accumulate days, nothing is written
        ProteinToleranceStatus tolerance = profile.getToleranceStatus();
        if (tolerance == ProteinToleranceStatus.MILD_ISSUES
                || tolerance == ProteinToleranceStatus.SIGNIFICANT_ISSUES) {
            return scheduledResult(
                    ScheduledEvaluationOutcome.HELD_TOLERANCE,
                    toleranceHoldResult(profile, tolerance));
        }

        // 5. Count exactly one day and stamp the marker
        int days = profile.getDaysAtCurrentTarget() + 1;
        profile.setDaysAtCurrentTarget(days);
        profile.setLastEvaluatedOn(today);

        if (days < MIN_DAYS_BEFORE_INCREMENT) {
            repository.save(profile);
            return scheduledResult(
                    ScheduledEvaluationOutcome.DAY_COUNTED,
                    toResult(profile, latestReportedIntakeGrams));
        }

        // Minimum period elapsed — advance one step (clamped), same save
        ProteinAdaptationResult advanced =
                advanceTarget(profile, today, new ArrayList<>());
        repository.save(profile);
        return scheduledResult(
                ScheduledEvaluationOutcome.TARGET_ADVANCED, advanced);
    }

    private ScheduledEvaluationResult scheduledResult(
            ScheduledEvaluationOutcome outcome,
            ProteinAdaptationResult adaptation) {

        return new ScheduledEvaluationResult(outcome, adaptation);
    }

    /**
     * Builds the result for a profile held by GI tolerance, with the same
     * warning that {@link #evaluate} emits. Does not modify the profile.
     */
    private ProteinAdaptationResult toleranceHoldResult(
            ProteinAdaptationProfile profile,
            ProteinToleranceStatus tolerance) {

        List<ProteinWarning> warnings = new ArrayList<>();
        if (tolerance == ProteinToleranceStatus.SIGNIFICANT_ISSUES) {
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.SIGNIFICANT_GI_SYMPTOMS,
                    "Significant GI symptoms have been reported. Protein target is being held. " +
                    "Please review protein sources, meal distribution, fibre intake, and fluid intake. " +
                    "If symptoms persist, consult a registered dietitian or medical professional."
            ));
        } else {
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.TOLERANCE_HOLD,
                    "Mild GI symptoms reported. Protein target is being held at " +
                    profile.getCurrentTargetGrams().toPlainString() + " g/day. " +
                    "Consider reviewing protein sources and distribution across meals."
            ));
        }
        return buildResult(profile, true, null, warnings);
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
     * <p>Only increments when the stage is {@code STARTING} or
     * {@code ADAPTING} — stops counting once the target is reached.</p>
     *
     * <p><strong>Not idempotent and not for scheduled use.</strong> This
     * method has no per-day guard, ignores tolerance and clinical review, and
     * does not set {@code lastEvaluatedOn}. The scheduled process must call
     * {@link #evaluateScheduled} instead.</p>
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
        return toResult(profile, resolveLatestReportedIntake(profile));
    }

    /**
     * Same as {@link #toResult(ProteinAdaptationProfile)} but with the
     * user's latest reported intake supplied by the caller.
     *
     * <p>The {@code CURRENT_INTAKE_UNKNOWN} warning follows
     * {@code latestReportedIntakeGrams}, <em>not</em> the stored adaptation
     * baseline. The profile is never modified, so the baseline, target,
     * stage and day counter are unaffected by the reported intake.</p>
     *
     * @param profile                  the adaptation profile
     * @param latestReportedIntakeGrams the latest user-reported intake;
     *                                 {@code null} when unknown or cleared
     */
    public ProteinAdaptationResult toResult(
            ProteinAdaptationProfile profile,
            BigDecimal latestReportedIntakeGrams) {

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

        if (latestReportedIntakeGrams == null) {
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
     * Reads the user's latest reported intake from the health profile
     * ({@code currentNutritionProfile.currentProteinGrams}). Returns
     * {@code null} when any link in the chain is absent.
     */
    private BigDecimal resolveLatestReportedIntake(
            ProteinAdaptationProfile profile) {

        HealthProfile healthProfile = profile.getHealthProfile();
        if (healthProfile == null
                || healthProfile.getCurrentNutritionProfile() == null) {
            return null;
        }
        return healthProfile.getCurrentNutritionProfile().getCurrentProteinGrams();
    }

    /**
     * Reads the user's current medical conditions through the profile's
     * health profile. Returns an empty set when any link in the chain is
     * absent.
     */
    private Set<MedicalCondition> resolveMedicalConditions(
            ProteinAdaptationProfile profile) {

        HealthProfile healthProfile = profile.getHealthProfile();
        if (healthProfile == null || healthProfile.getMedicalProfile() == null) {
            return Set.of();
        }

        Set<MedicalCondition> conditions =
                healthProfile.getMedicalProfile().getMedicalConditions();
        return conditions != null ? conditions : Set.of();
    }

    /**
     * Builds the result returned when clinical review blocks progression.
     * Reports the stored state unchanged and persists nothing.
     */
    private ProteinAdaptationResult buildClinicalReviewBlockedResult(
            ProteinAdaptationProfile profile) {

        ProteinAdaptationStage stage = profile.getAdaptationStage();
        boolean rampPending = stage != ProteinAdaptationStage.TARGET_REACHED
                && stage != ProteinAdaptationStage.NOT_STARTED;

        return new ProteinAdaptationResult(
                profile.getCurrentTargetGrams(),
                profile.getLongTermTargetGrams(),
                profile.getCurrentProteinGrams(),
                stage,
                rampPending,
                null,
                profile.getToleranceStatus(),
                List.of(new ProteinWarning(
                        ProteinWarningCode.CLINICAL_REVIEW_REQUIRED,
                        "Protein target progression is paused because a medical " +
                        "condition requires clinical review. The current target is " +
                        "held until it has been reviewed by a qualified professional."
                )),
                true
        );
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
