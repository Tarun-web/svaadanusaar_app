package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CarbohydrateCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CarbohydrateWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CarbohydrateWarningCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the user's daily carbohydrate target in grams.
 *
 * <h3>Design principle — carbohydrates are the flexible macro</h3>
 * <p>Carbohydrates receive whatever calories remain after protein and fat
 * have been allocated.  There are no goal-specific carbohydrate multipliers
 * (no {@code carbs = bodyWeight × X} formula).  The formula is always:</p>
 * <pre>
 *   carbohydrateCalories = FatCalculationResult.remainingCaloriesForCarbs()
 *   carbohydrateGrams    = carbohydrateCalories / 4
 * </pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Convert remaining carbohydrate calories to grams.</li>
 *   <li>Clamp to zero when remaining calories are non-positive.</li>
 *   <li>Propagate upstream safety status — never downgrade it.</li>
 *   <li>Validate input.</li>
 * </ul>
 *
 * <h3>NOT responsible for</h3>
 * <ul>
 *   <li>Calculating protein, fat, TDEE, or calorie targets.</li>
 *   <li>Reducing protein or fat to make carbohydrates larger.</li>
 *   <li>Inventing additional calories.</li>
 *   <li>Fibre, water, meal distribution, or food selection.</li>
 *   <li>Medical diagnosis or disease-specific carbohydrate prescriptions.</li>
 *   <li>Bioavailability adjustment.</li>
 * </ul>
 *
 * <h3>Upstream safety contract</h3>
 * <p>The {@link CalorieSafetyStatus} from {@link FatCalculationResult}
 * is the floor for this result.  A {@code CLINICAL_REVIEW_REQUIRED} upstream
 * status will never become {@code SAFE} here.</p>
 *
 * <h3>Macro priority</h3>
 * <ol>
 *   <li>Target calories (established by {@code CalorieSafetyEngine})</li>
 *   <li>Protein (established by {@code ProteinAdaptationService})</li>
 *   <li>Fat (established by {@code FatCalculator})</li>
 *   <li><strong>Carbohydrates — this calculator</strong></li>
 * </ol>
 */
@Component
public class CarbohydrateCalculator {

    // ================================================================
    // Constants
    // ================================================================

    /** Kcal per gram of carbohydrate. */
    public static final BigDecimal KCAL_PER_GRAM_CARBOHYDRATE = new BigDecimal("4");

    /** Scale for all carbohydrate values (grams and calories). */
    private static final int SCALE = 2;

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ================================================================
    // calculate()
    // ================================================================

    /**
     * Calculates the daily carbohydrate target from the upstream
     * {@link FatCalculationResult}.
     *
     * @param fatResult the result of the upstream {@link FatCalculator} call;
     *                  must not be {@code null} and must contain a non-null
     *                  {@code remainingCaloriesForCarbs}
     * @return a {@link CarbohydrateCalculationResult} with grams, calories,
     *         feasibility, upstream safety state, and any warnings
     * @throws IllegalArgumentException if {@code fatResult} or
     *         {@code remainingCaloriesForCarbs} is {@code null}
     */
    public CarbohydrateCalculationResult calculate(FatCalculationResult fatResult) {

        validateInput(fatResult);

        List<CarbohydrateWarning> warnings = new ArrayList<>();

        BigDecimal remaining = fatResult.remainingCaloriesForCarbs();

        // ── 1. Propagate upstream safety state ────────────────────────
        //    Safety can only stay the same or worsen — never improve.
        CalorieSafetyStatus safetyStatus        = fatResult.safetyStatus();
        boolean requiresClinicalReview           = fatResult.requiresClinicalReview();

        if (requiresClinicalReview) {
            warnings.add(new CarbohydrateWarning(
                    CarbohydrateWarningCode.UPSTREAM_CLINICAL_REVIEW_REQUIRED,
                    "The upstream fat calculation requires clinical review. " +
                    "The carbohydrate target is calculated for reference only."
            ));
        }

        // ── 2. Handle negative headroom ───────────────────────────────
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add(new CarbohydrateWarning(
                    CarbohydrateWarningCode.UPSTREAM_INFEASIBLE,
                    "Protein and fat already exceed the target calorie budget " +
                    "(" + remaining.toPlainString() + " kcal remaining). " +
                    "Carbohydrates have been set to 0 g. " +
                    "Review the macro combination at the orchestration layer."
            ));
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.WARNING);
            return buildResult(
                    BigDecimal.ZERO, BigDecimal.ZERO, remaining,
                    false, safetyStatus, requiresClinicalReview, warnings);
        }

        // ── 3. Handle zero headroom ───────────────────────────────────
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add(new CarbohydrateWarning(
                    CarbohydrateWarningCode.ZERO_CARBOHYDRATE_HEADROOM,
                    "No calories remain for carbohydrates after protein and fat allocation. " +
                    "Carbohydrates have been set to 0 g. " +
                    "The current macro combination cannot support a practical carbohydrate allocation."
            ));
            safetyStatus = worstOf(safetyStatus, CalorieSafetyStatus.WARNING);
            return buildResult(
                    BigDecimal.ZERO, BigDecimal.ZERO, remaining,
                    false, safetyStatus, requiresClinicalReview, warnings);
        }

        // ── 4. Normal positive headroom ───────────────────────────────
        BigDecimal carbCalories = remaining.setScale(SCALE, ROUNDING);

        BigDecimal carbGrams = carbCalories
                .divide(KCAL_PER_GRAM_CARBOHYDRATE, SCALE, ROUNDING);

        return buildResult(
                carbGrams, carbCalories, remaining,
                true, safetyStatus, requiresClinicalReview, warnings);
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private CarbohydrateCalculationResult buildResult(
            BigDecimal carbGrams,
            BigDecimal carbCalories,
            BigDecimal remaining,
            boolean feasible,
            CalorieSafetyStatus safetyStatus,
            boolean requiresClinicalReview,
            List<CarbohydrateWarning> warnings) {

        return new CarbohydrateCalculationResult(
                carbGrams,
                carbCalories,
                remaining,
                feasible,
                safetyStatus,
                requiresClinicalReview,
                List.copyOf(warnings)
        );
    }

    /**
     * Returns the more severe of two {@link CalorieSafetyStatus} values.
     * Follows the same pattern used by {@code FatCalculator} and
     * {@code ProteinCalculator}.
     */
    private CalorieSafetyStatus worstOf(
            CalorieSafetyStatus a, CalorieSafetyStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /**
     * Validates the input and throws {@link IllegalArgumentException}
     * with a clear message on the first violation found.
     *
     * <p>Note: a negative {@code remainingCaloriesForCarbs} is NOT a
     * validation error — it is an expected business condition that is
     * handled gracefully in {@link #calculate}.</p>
     */
    private void validateInput(FatCalculationResult fatResult) {

        if (fatResult == null) {
            throw new IllegalArgumentException(
                    "FatCalculationResult must not be null."
            );
        }

        if (fatResult.remainingCaloriesForCarbs() == null) {
            throw new IllegalArgumentException(
                    "FatCalculationResult.remainingCaloriesForCarbs must not be null."
            );
        }

        if (fatResult.safetyStatus() == null) {
            throw new IllegalArgumentException(
                    "FatCalculationResult.safetyStatus must not be null."
            );
        }
    }
}
