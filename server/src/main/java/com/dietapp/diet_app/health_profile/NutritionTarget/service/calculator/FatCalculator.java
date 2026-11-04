package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.FatWarningCode;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calculates the user's daily fat target in grams.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Select the correct fat-per-kg multiplier for the given goal
 *       and training context.</li>
 *   <li>Compute fat grams and the caloric equivalent (fat × 9 kcal/g).</li>
 *   <li>Check macro feasibility — whether remaining calories after protein
 *       and fat are sufficient for carbohydrates (must be &gt; 0).</li>
 *   <li>Apply medical safety checks and surface warnings.</li>
 *   <li>Validate input and reject malformed inputs early.</li>
 * </ul>
 *
 * <h3>NOT responsible for</h3>
 * <ul>
 *   <li>Protein calculation — already handled by {@code ProteinCalculator}
 *       and {@code ProteinAdaptationService}.</li>
 *   <li>Carbohydrate calculation — {@code CarbohydrateCalculator} uses
 *       {@link FatCalculationResult#remainingCaloriesForCarbs()}.</li>
 *   <li>Fibre, water, TDEE, or BMR.</li>
 *   <li>Reducing protein to make fat fit — protein is a fixed upstream input.</li>
 *   <li>Database persistence.</li>
 *   <li>Meal generation or food selection.</li>
 *   <li>Bioavailability — fat quality is evaluated at the food/recipe layer.</li>
 * </ul>
 *
 * <h3>Macro priority order</h3>
 * <ol>
 *   <li>Target calories (fixed)</li>
 *   <li>Protein target (fixed upstream — never reduced here)</li>
 *   <li>Fat target (calculated here)</li>
 *   <li>Carbohydrates receive whatever calories remain</li>
 * </ol>
 *
 * <h3>Training demand tier</h3>
 * <p>For goals with a range (WEIGHT_GAIN, SPORTS_PERFORMANCE), the upper
 * multiplier is used deterministically when {@link WorkoutIntensity} is
 * {@code HIGH} or {@code ATHLETE}.  No random choice, no arbitrary scoring.</p>
 */
@Component
public class FatCalculator {

    // ================================================================
    // Fat-per-kg multipliers  (g fat / kg body-weight)
    // All values are application defaults — centralised here.
    // Change one constant to adjust the entire policy.
    // ================================================================

    /** General health — base fat target. */
    public static final BigDecimal MULTIPLIER_GENERAL_HEALTH     = new BigDecimal("0.8");

    /** Maintenance — same as general health. */
    public static final BigDecimal MULTIPLIER_MAINTENANCE        = new BigDecimal("0.8");

    /** Fat loss — both with and without resistance training. */
    public static final BigDecimal MULTIPLIER_FAT_LOSS           = new BigDecimal("0.7");

    /** Fat loss with resistance training — slightly higher to preserve hormones. */
    public static final BigDecimal MULTIPLIER_FAT_LOSS_RT        = new BigDecimal("0.8");

    /** Muscle gain. */
    public static final BigDecimal MULTIPLIER_MUSCLE_GAIN        = new BigDecimal("0.8");

    /** Body recomposition. */
    public static final BigDecimal MULTIPLIER_BODY_RECOMPOSITION = new BigDecimal("0.8");

    /** Weight gain — normal training demand. */
    public static final BigDecimal MULTIPLIER_WEIGHT_GAIN_NORMAL = new BigDecimal("0.8");

    /** Weight gain — higher training demand (HIGH / ATHLETE intensity). */
    public static final BigDecimal MULTIPLIER_WEIGHT_GAIN_HIGH   = new BigDecimal("1.0");

    /** Sports performance — normal training demand. */
    public static final BigDecimal MULTIPLIER_SPORTS_NORMAL      = new BigDecimal("0.8");

    /** Sports performance — higher training demand (HIGH / ATHLETE intensity). */
    public static final BigDecimal MULTIPLIER_SPORTS_HIGH        = new BigDecimal("1.0");

    // ================================================================
    // Caloric constants
    // ================================================================

    /** Kcal per gram of fat. */
    public static final BigDecimal KCAL_PER_GRAM_FAT     = new BigDecimal("9");

    /** Kcal per gram of protein (used for feasibility check). */
    public static final BigDecimal KCAL_PER_GRAM_PROTEIN = new BigDecimal("4");

    // ================================================================
    // Safety bounds
    // ================================================================

    /**
     * Absolute minimum fat target (g/day).
     * Below this, essential fatty-acid sufficiency cannot be assumed.
     */
    public static final BigDecimal MIN_FAT_GRAMS = new BigDecimal("20");

    /**
     * Absolute maximum fat target (g/day) produced by this calculator.
     * Prevents extreme values from malformed inputs.
     */
    public static final BigDecimal MAX_FAT_GRAMS = new BigDecimal("300");

    /**
     * Minimum calories remaining for carbohydrates before a
     * LOW_CARBOHYDRATE_HEADROOM warning is issued.
     */
    public static final BigDecimal LOW_CARB_HEADROOM_THRESHOLD = new BigDecimal("100");

    // ================================================================
    // calculate()
    // ================================================================

    /**
     * Calculates the daily fat target for the given input.
     *
     * @param input validated fat calculation input — must not be {@code null}
     * @return a {@link FatCalculationResult} containing the target, feasibility
     *         information, and any warnings
     * @throws IllegalArgumentException if the input fails validation
     */
    public FatCalculationResult calculate(FatCalculationInput input) {

        validateInput(input);

        List<FatWarning> warnings = new ArrayList<>();

        // ── 1. Medical safety check ───────────────────────────────────
        CalorieSafetyStatus safetyStatus  = CalorieSafetyStatus.SAFE;
        boolean requiresClinicalReview    = false;

        Set<MedicalCondition> conditions =
                input.medicalConditions() != null
                        ? input.medicalConditions()
                        : Set.of();

        for (MedicalCondition condition : conditions) {
            if (requiresClinicalReviewForFat(condition)) {
                requiresClinicalReview = true;
                safetyStatus = CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED;
                warnings.add(new FatWarning(
                        FatWarningCode.CLINICAL_REVIEW_REQUIRED,
                        "Medical condition [" + condition.name() + "] may require " +
                        "individualised dietary fat management. " +
                        "The calculated target is provided for reference only."
                ));
            }
            FatWarning conditionWarning = medicalConditionWarning(condition);
            if (conditionWarning != null) {
                warnings.add(conditionWarning);
            }
        }

        // ── 2. Determine fat-per-kg multiplier ───────────────────────
        MultiplierResult mr = resolveMultiplier(input);
        BigDecimal multiplier    = mr.multiplier();
        String calculationBasis  = mr.basis();

        // ── 3. Calculate raw fat target ───────────────────────────────
        BigDecimal rawFat = input.weightKg()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // ── 4. Safety clamp ───────────────────────────────────────────
        BigDecimal fatGrams = rawFat;

        if (rawFat.compareTo(MAX_FAT_GRAMS) > 0) {
            fatGrams = MAX_FAT_GRAMS;
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.ADJUSTED);
            warnings.add(new FatWarning(
                    FatWarningCode.TARGET_CLAMPED_MAX,
                    "Calculated fat target (" + rawFat.toPlainString() +
                    " g) exceeded the maximum ceiling of " +
                    MAX_FAT_GRAMS.toPlainString() + " g and has been clamped."
            ));
        }

        if (fatGrams.compareTo(MIN_FAT_GRAMS) < 0) {
            fatGrams = MIN_FAT_GRAMS;
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.ADJUSTED);
            warnings.add(new FatWarning(
                    FatWarningCode.TARGET_CLAMPED_MIN,
                    "Calculated fat target was below the minimum floor of " +
                    MIN_FAT_GRAMS.toPlainString() + " g and has been adjusted."
            ));
        }

        // ── 5. Caloric equivalent ─────────────────────────────────────
        BigDecimal fatCalories = fatGrams
                .multiply(KCAL_PER_GRAM_FAT)
                .setScale(2, RoundingMode.HALF_UP);

        // ── 6. Macro feasibility check ────────────────────────────────
        BigDecimal proteinCalories = input.proteinGrams()
                .multiply(KCAL_PER_GRAM_PROTEIN)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingCaloriesForCarbs = input.targetCalories()
                .subtract(proteinCalories)
                .subtract(fatCalories)
                .setScale(2, RoundingMode.HALF_UP);

        boolean feasible = remainingCaloriesForCarbs.compareTo(BigDecimal.ZERO) > 0;

        if (!feasible) {
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.WARNING);
            warnings.add(new FatWarning(
                    FatWarningCode.MACRO_COMBINATION_INFEASIBLE,
                    "After allocating protein (" + input.proteinGrams().toPlainString() +
                    " g = " + proteinCalories.toPlainString() + " kcal) and fat (" +
                    fatGrams.toPlainString() + " g = " + fatCalories.toPlainString() +
                    " kcal), the remaining calories for carbohydrates are " +
                    remainingCaloriesForCarbs.toPlainString() + " kcal. " +
                    "The macro combination is infeasible at the current calorie target (" +
                    input.targetCalories().toPlainString() + " kcal). " +
                    "Adjust calorie target or review macro priorities at the orchestration layer."
            ));
        } else if (remainingCaloriesForCarbs.compareTo(LOW_CARB_HEADROOM_THRESHOLD) < 0) {
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.ADJUSTED);
            warnings.add(new FatWarning(
                    FatWarningCode.LOW_CARBOHYDRATE_HEADROOM,
                    "Only " + remainingCaloriesForCarbs.toPlainString() +
                    " kcal remain for carbohydrates after protein and fat allocation. " +
                    "This may result in a very low carbohydrate intake."
            ));
        }

        // ── 7. Build result ───────────────────────────────────────────
        return new FatCalculationResult(
                fatGrams,
                fatCalories,
                remainingCaloriesForCarbs,
                multiplier,
                calculationBasis,
                feasible,
                safetyStatus,
                requiresClinicalReview,
                List.copyOf(warnings)
        );
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Resolves the fat-per-kg multiplier and a human-readable basis label.
     *
     * <p>For goals with a range (WEIGHT_GAIN, SPORTS_PERFORMANCE), the
     * higher tier is selected deterministically when workout intensity
     * is {@link WorkoutIntensity#HIGH} or {@link WorkoutIntensity#ATHLETE}.</p>
     */
    private MultiplierResult resolveMultiplier(FatCalculationInput input) {
        return switch (input.goal()) {
            case GENERAL_HEALTH ->
                    new MultiplierResult(MULTIPLIER_GENERAL_HEALTH, "GENERAL_HEALTH");

            case MAINTENANCE ->
                    new MultiplierResult(MULTIPLIER_MAINTENANCE, "MAINTENANCE");

            case FAT_LOSS ->
                    input.resistanceTraining()
                            ? new MultiplierResult(MULTIPLIER_FAT_LOSS_RT, "FAT_LOSS_WITH_RT")
                            : new MultiplierResult(MULTIPLIER_FAT_LOSS,    "FAT_LOSS");

            case MUSCLE_GAIN ->
                    new MultiplierResult(MULTIPLIER_MUSCLE_GAIN, "MUSCLE_GAIN");

            case BODY_RECOMPOSITION ->
                    new MultiplierResult(MULTIPLIER_BODY_RECOMPOSITION, "BODY_RECOMPOSITION");

            case WEIGHT_GAIN ->
                    isHighTrainingDemand(input.workoutIntensity())
                            ? new MultiplierResult(MULTIPLIER_WEIGHT_GAIN_HIGH,   "WEIGHT_GAIN_HIGH_DEMAND")
                            : new MultiplierResult(MULTIPLIER_WEIGHT_GAIN_NORMAL, "WEIGHT_GAIN_NORMAL");

            case SPORTS_PERFORMANCE ->
                    isHighTrainingDemand(input.workoutIntensity())
                            ? new MultiplierResult(MULTIPLIER_SPORTS_HIGH,   "SPORTS_HIGH_DEMAND")
                            : new MultiplierResult(MULTIPLIER_SPORTS_NORMAL, "SPORTS_NORMAL");
        };
    }

    /**
     * Returns {@code true} when the workout intensity indicates a higher
     * training demand that justifies the upper fat-per-kg multiplier.
     *
     * <p>Uses the existing {@link WorkoutIntensity} enum — no new enum created.</p>
     */
    private boolean isHighTrainingDemand(WorkoutIntensity intensity) {
        if (intensity == null) {
            return false;
        }
        return intensity == WorkoutIntensity.HIGH
                || intensity == WorkoutIntensity.ATHLETE;
    }

    /**
     * Returns {@code true} when the medical condition warrants clinical
     * review specifically for dietary fat management.
     */
    private boolean requiresClinicalReviewForFat(MedicalCondition condition) {
        // These conditions have direct clinical implications for dietary fat
        // management that go beyond automated defaults.
        return switch (condition) {
            case LIVER_DISEASE -> true;
            default -> false;
        };
    }

    /**
     * Builds a condition-specific fat warning, or returns {@code null}
     * when the condition has no fat-specific guidance to emit.
     */
    private FatWarning medicalConditionWarning(MedicalCondition condition) {
        return switch (condition) {
            case HIGH_CHOLESTEROL ->
                    new FatWarning(
                            FatWarningCode.HIGH_CHOLESTEROL_DETECTED,
                            "High cholesterol is present. The fat quantity target is calculated " +
                            "as usual, but the type and source of dietary fat (saturated vs " +
                            "unsaturated) should be carefully evaluated at the meal-planning layer."
                    );
            case FATTY_LIVER ->
                    new FatWarning(
                            FatWarningCode.FATTY_LIVER_DETECTED,
                            "Fatty liver is present. Overall dietary fat intake may warrant " +
                            "clinical guidance. The calculated target is a general reference only."
                    );
            case GOUT ->
                    new FatWarning(
                            FatWarningCode.GOUT_DETECTED,
                            "Gout is present. Dietary fat pattern may interact with uric acid " +
                            "metabolism. Food selection should be reviewed at the diet layer."
                    );
            case LIVER_DISEASE ->
                    new FatWarning(
                            FatWarningCode.CLINICAL_REVIEW_REQUIRED,
                            "Liver disease is present. Dietary fat intake may require " +
                            "individualised clinical management."
                    );
            default -> null;
        };
    }

    /**
     * Returns the more severe of two {@link CalorieSafetyStatus} values.
     */
    private CalorieSafetyStatus worstOf(
            CalorieSafetyStatus a, CalorieSafetyStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /**
     * Validates the input and throws {@link IllegalArgumentException}
     * with a clear message on the first violation found.
     */
    private void validateInput(FatCalculationInput input) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "FatCalculationInput must not be null."
            );
        }

        if (input.weightKg() == null) {
            throw new IllegalArgumentException(
                    "Weight is required for fat calculation."
            );
        }

        if (input.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Weight must be greater than 0. Received: " + input.weightKg()
            );
        }

        if (input.goal() == null) {
            throw new IllegalArgumentException(
                    "Goal is required for fat calculation."
            );
        }

        if (input.targetCalories() == null) {
            throw new IllegalArgumentException(
                    "Target calories are required for fat feasibility calculation."
            );
        }

        if (input.targetCalories().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Target calories must be greater than 0. Received: "
                    + input.targetCalories()
            );
        }

        if (input.proteinGrams() == null) {
            throw new IllegalArgumentException(
                    "Protein grams are required for fat feasibility calculation."
            );
        }

        if (input.proteinGrams().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Protein grams cannot be negative. Received: " + input.proteinGrams()
            );
        }
    }

    // ================================================================
    // Internal value holder
    // ================================================================

    /**
     * Internal record carrying a resolved multiplier and its label.
     */
    private record MultiplierResult(BigDecimal multiplier, String basis) {
    }
}
