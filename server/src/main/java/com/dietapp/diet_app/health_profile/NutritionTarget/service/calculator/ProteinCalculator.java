package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates the user's long-term physiological protein target.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Determine the correct protein-per-kg multiplier for the goal
 *       and training context.</li>
 *   <li>Choose between lean body mass (LBM) and total body weight
 *       as the calculation basis.</li>
 *   <li>Apply medical safety checks and flag
 *       {@code CLINICAL_REVIEW_REQUIRED} where appropriate.</li>
 *   <li>Validate input and reject malformed profiles early.</li>
 * </ul>
 *
 * <h3>NOT responsible for</h3>
 * <ul>
 *   <li>Database persistence.</li>
 *   <li>Adaptation state or GI symptom tracking.</li>
 *   <li>Meal generation or food selection.</li>
 *   <li>Protein bioavailability adjustment — quality is evaluated
 *       at the food/recipe layer, never here.</li>
 *   <li>Calculating TDEE, BMR, or calorie targets.</li>
 * </ul>
 *
 * <h3>Bioavailability guarantee</h3>
 * <p>No bioavailability factor is applied to reduce the protein target.
 * The output represents the <em>dietary protein requirement</em>.
 * Protein quality evaluation happens downstream when actual foods are
 * selected for the meal plan.</p>
 */
@Component
public class ProteinCalculator {

    // ================================================================
    // Protein-per-kg multipliers (g protein / kg body-weight or LBM)
    // All values are application defaults — centralised here.
    // ================================================================

    /** General health / maintenance baseline. */
    static final BigDecimal MULTIPLIER_GENERAL_HEALTH     = new BigDecimal("1.2");

    /** Fat loss without resistance training. */
    static final BigDecimal MULTIPLIER_FAT_LOSS_NO_RT     = new BigDecimal("1.6");

    /** Fat loss with resistance training. */
    static final BigDecimal MULTIPLIER_FAT_LOSS_RT        = new BigDecimal("1.8");

    /** Muscle gain. */
    static final BigDecimal MULTIPLIER_MUSCLE_GAIN        = new BigDecimal("1.8");

    /** Body recomposition. */
    static final BigDecimal MULTIPLIER_BODY_RECOMPOSITION = new BigDecimal("2.0");

    /** Weight gain without resistance training. */
    static final BigDecimal MULTIPLIER_WEIGHT_GAIN_NO_RT  = new BigDecimal("1.4");

    /** Weight gain with resistance training. */
    static final BigDecimal MULTIPLIER_WEIGHT_GAIN_RT     = new BigDecimal("1.8");

    /** Sports performance. */
    static final BigDecimal MULTIPLIER_SPORTS_PERFORMANCE = new BigDecimal("1.8");

    // ================================================================
    // Safety boundaries
    // ================================================================

    /**
     * Upper bound for a valid body-fat percentage.
     * Values above this are considered unreliable and are ignored.
     */
    static final BigDecimal MAX_VALID_BODY_FAT_PCT = new BigDecimal("60");

    /**
     * Lower bound for a valid body-fat percentage.
     */
    static final BigDecimal MIN_VALID_BODY_FAT_PCT = BigDecimal.ZERO;

    /**
     * Absolute maximum protein target produced by this calculator (g/day).
     * Prevents extreme values from malformed inputs.
     */
    static final BigDecimal MAX_PROTEIN_GRAMS = new BigDecimal("400");

    /**
     * Absolute minimum protein target (g/day).
     * Ensures no negative or near-zero value escapes.
     */
    static final BigDecimal MIN_PROTEIN_GRAMS = new BigDecimal("30");

    /**
     * Every {@link MedicalCondition} for which
     * {@link #requiresClinicalReview(MedicalCondition)} is {@code true}.
     *
     * <p>Derived from that method so database queries and in-memory checks
     * always share one rule.</p>
     */
    public static final Set<MedicalCondition> CLINICAL_REVIEW_CONDITIONS =
            Collections.unmodifiableSet(
                    Arrays.stream(MedicalCondition.values())
                            .filter(ProteinCalculator::requiresClinicalReview)
                            .collect(Collectors.toCollection(
                                    () -> EnumSet.noneOf(MedicalCondition.class))));

    // ================================================================
    // calculate()
    // ================================================================

    /**
     * Calculates the long-term daily protein target for the given input.
     *
     * @param input validated protein calculation input — must not be {@code null}
     * @return a {@link ProteinTargetResult} containing the target, basis, and warnings
     * @throws IllegalArgumentException if the input is malformed
     */
    public ProteinTargetResult calculate(ProteinCalculationInput input) {

        validateInput(input);

        List<ProteinWarning> warnings = new ArrayList<>();

        // ── 1. Medical safety check ──────────────────────────────────
        CalorieSafetyStatus safetyStatus = CalorieSafetyStatus.SAFE;
        boolean requiresClinicalReview   = false;

        Set<MedicalCondition> conditions =
                input.medicalConditions() != null
                        ? input.medicalConditions()
                        : Set.of();

        for (MedicalCondition condition : conditions) {
            if (requiresClinicalReview(condition)) {
                requiresClinicalReview = true;
                safetyStatus = CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED;
                warnings.add(new ProteinWarning(
                        ProteinWarningCode.CLINICAL_REVIEW_REQUIRED,
                        "Medical condition [" + condition.name() + "] requires " +
                        "individualised clinical protein management. " +
                        "The calculated target is provided for reference only."
                ));
            }
            // Always emit a condition-specific warning for any relevant condition
            ProteinWarning conditionWarning = medicalConditionWarning(condition);
            if (conditionWarning != null) {
                warnings.add(conditionWarning);
            }
        }

        // ── 2. Determine weight basis (LBM vs total weight) ──────────
        WeightBasis basis = resolveWeightBasis(input, warnings);

        // ── 3. Determine protein-per-kg multiplier ───────────────────
        BigDecimal multiplier = resolveMultiplier(input);

        // ── 4. Calculate raw target ───────────────────────────────────
        BigDecimal rawTarget = basis.weightKg()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // ── 5. Safety clamp ───────────────────────────────────────────
        BigDecimal target = rawTarget;

        if (rawTarget.compareTo(MAX_PROTEIN_GRAMS) > 0) {
            target = MAX_PROTEIN_GRAMS;
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.ADJUSTED);
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.TARGET_CLAMPED_MAX,
                    "Calculated protein target (" + rawTarget.toPlainString() +
                    " g) exceeded the maximum safety ceiling of " +
                    MAX_PROTEIN_GRAMS.toPlainString() + " g and has been clamped."
            ));
        }

        if (target.compareTo(MIN_PROTEIN_GRAMS) < 0) {
            target = MIN_PROTEIN_GRAMS;
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.ADJUSTED);
            warnings.add(new ProteinWarning(
                    ProteinWarningCode.TARGET_CLAMPED_MIN,
                    "Calculated protein target was below the minimum safe floor of " +
                    MIN_PROTEIN_GRAMS.toPlainString() + " g and has been adjusted."
            ));
        }

        // ── 6. Build result ───────────────────────────────────────────
        return new ProteinTargetResult(
                target,
                basis.label(),
                basis.weightKg(),
                multiplier,
                safetyStatus,
                requiresClinicalReview,
                List.copyOf(warnings)
        );
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Resolves the weight basis for the calculation.
     *
     * <p>Uses LBM when a valid body-fat percentage is present;
     * otherwise falls back to total body weight.</p>
     */
    private WeightBasis resolveWeightBasis(
            ProteinCalculationInput input,
            List<ProteinWarning> warnings) {

        BigDecimal bodyFat = input.bodyFatPercentage();

        if (bodyFat != null) {
            if (bodyFat.compareTo(MIN_VALID_BODY_FAT_PCT) < 0
                    || bodyFat.compareTo(MAX_VALID_BODY_FAT_PCT) > 0) {

                warnings.add(new ProteinWarning(
                        ProteinWarningCode.BODY_FAT_INVALID_IGNORED,
                        "Body-fat percentage (" + bodyFat.toPlainString() +
                        "%) is outside the valid range [0–60%] and has been ignored. " +
                        "Calculation will use total body weight."
                ));
            } else {
                // LBM = weight × (1 − bodyFat%)
                BigDecimal fatFraction = bodyFat.divide(
                        BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                BigDecimal lbm = input.weightKg()
                        .multiply(BigDecimal.ONE.subtract(fatFraction))
                        .setScale(2, RoundingMode.HALF_UP);
                return new WeightBasis(lbm, "LBM");
            }
        }

        return new WeightBasis(input.weightKg(), "BODY_WEIGHT");
    }

    /**
     * Selects the protein-per-kg multiplier for the given goal and
     * resistance-training status.
     */
    private BigDecimal resolveMultiplier(ProteinCalculationInput input) {
        return switch (input.goal()) {
            case FAT_LOSS ->
                    input.resistanceTraining()
                            ? MULTIPLIER_FAT_LOSS_RT
                            : MULTIPLIER_FAT_LOSS_NO_RT;

            case MUSCLE_GAIN ->
                    MULTIPLIER_MUSCLE_GAIN;

            case BODY_RECOMPOSITION ->
                    MULTIPLIER_BODY_RECOMPOSITION;

            case WEIGHT_GAIN ->
                    input.resistanceTraining()
                            ? MULTIPLIER_WEIGHT_GAIN_RT
                            : MULTIPLIER_WEIGHT_GAIN_NO_RT;

            case SPORTS_PERFORMANCE ->
                    MULTIPLIER_SPORTS_PERFORMANCE;

            case MAINTENANCE, GENERAL_HEALTH ->
                    MULTIPLIER_GENERAL_HEALTH;
        };
    }

    /**
     * Returns {@code true} when the condition requires clinical review
     * before a protein target can be safely prescribed.
     *
     * <p>The list is intentionally conservative. When in doubt, flag
     * for clinical review rather than silently prescribing a target.</p>
     */
    public static boolean requiresClinicalReview(MedicalCondition condition) {
        return switch (condition) {
            case KIDNEY_DISEASE, LIVER_DISEASE -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} when any of the given conditions requires
     * clinical review. {@code null} or empty means no known conditions.
     */
    public static boolean anyRequireClinicalReview(
            Collection<MedicalCondition> conditions) {

        return conditions != null
                && conditions.stream()
                        .anyMatch(ProteinCalculator::requiresClinicalReview);
    }

    /**
     * Builds a condition-specific warning message, or returns {@code null}
     * when the condition has no protein-specific guidance to emit.
     */
    private ProteinWarning medicalConditionWarning(MedicalCondition condition) {
        return switch (condition) {
            case KIDNEY_DISEASE ->
                    new ProteinWarning(
                            ProteinWarningCode.KIDNEY_DISEASE_DETECTED,
                            "Kidney disease is present. High protein intake can increase " +
                            "renal workload. Please consult a nephrologist or registered " +
                            "dietitian before using this protein target."
                    );
            case LIVER_DISEASE ->
                    new ProteinWarning(
                            ProteinWarningCode.LIVER_DISEASE_DETECTED,
                            "Liver disease is present. Protein requirements and tolerances " +
                            "vary significantly by condition severity. Clinical supervision " +
                            "is required."
                    );
            case CELIAC ->
                    new ProteinWarning(
                            ProteinWarningCode.CELIAC_DISEASE_DETECTED,
                            "Celiac disease is present. Protein quantity is not inherently " +
                            "restricted by celiac disease. Please ensure protein sources " +
                            "are gluten-free. Food selection will be handled at the diet layer."
                    );
            case HYPERTHYROID ->
                    new ProteinWarning(
                            ProteinWarningCode.HYPERTHYROID_DETECTED,
                            "Hyperthyroidism is present. Protein metabolism may be affected. " +
                            "Clinical safety evaluation has been flagged."
                    );
            default ->
                    // No protein-specific guidance for other conditions
                    null;
        };
    }

    /**
     * Returns the more severe of two safety statuses.
     */
    private CalorieSafetyStatus worstOf(
            CalorieSafetyStatus a, CalorieSafetyStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /**
     * Validates the input and throws {@link IllegalArgumentException}
     * with a descriptive message on the first violation found.
     */
    private void validateInput(ProteinCalculationInput input) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "ProteinCalculationInput must not be null."
            );
        }

        if (input.weightKg() == null) {
            throw new IllegalArgumentException(
                    "Weight is required for protein calculation."
            );
        }

        if (input.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Weight must be greater than 0. Received: " + input.weightKg()
            );
        }

        if (input.goal() == null) {
            throw new IllegalArgumentException(
                    "Goal is required for protein calculation."
            );
        }

        if (input.currentProteinGrams() != null
                && input.currentProteinGrams().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Current protein intake cannot be negative. Received: "
                    + input.currentProteinGrams()
            );
        }
    }

    // ================================================================
    // Value holder
    // ================================================================

    /**
     * Internal record holding the resolved weight basis.
     *
     * @param weightKg the weight value to use in the calculation
     * @param label    {@code "LBM"} or {@code "BODY_WEIGHT"}
     */
    private record WeightBasis(BigDecimal weightKg, String label) {
    }
}
