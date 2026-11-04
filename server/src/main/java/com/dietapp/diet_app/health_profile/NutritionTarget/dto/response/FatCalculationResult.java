package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * The output of {@code FatCalculator}.
 *
 * <p>Contains the calculated fat target, its caloric equivalent, feasibility
 * information for carbohydrate headroom, and any warnings.</p>
 *
 * <h3>Carbohydrate headroom</h3>
 * <p>{@code remainingCaloriesForCarbs} tells the downstream
 * {@code CarbohydrateCalculator} exactly how many calories are available
 * after protein and fat have been allocated.  A negative or very low value
 * means the macro combination is infeasible and
 * {@link #feasible} will be {@code false}.</p>
 *
 * <h3>Protein is never reduced</h3>
 * <p>Protein has been established by {@code ProteinCalculator} +
 * {@code ProteinAdaptationService} and is treated as fixed.
 * If fat + protein exceeds target calories, this result will carry a
 * {@code MACRO_COMBINATION_INFEASIBLE} warning and {@link #feasible}
 * will be {@code false}. The resolution is handled by the macro
 * orchestration layer, not silently here.</p>
 */
public record FatCalculationResult(

        /**
         * The calculated daily fat target in grams.
         *
         * <p>Always &ge; the configured minimum floor and never negative.</p>
         */
        BigDecimal fatGrams,

        /**
         * Caloric equivalent of {@link #fatGrams} (fatGrams × 9).
         */
        BigDecimal fatCalories,

        /**
         * Calories remaining for carbohydrates after protein and fat are allocated.
         *
         * <pre>
         *   remainingCaloriesForCarbs
         *       = targetCalories
         *       - (proteinGrams × 4)
         *       - fatCalories
         * </pre>
         *
         * <p>May be negative when the macro combination is infeasible.
         * {@link #feasible} will be {@code false} in that case.</p>
         */
        BigDecimal remainingCaloriesForCarbs,

        /**
         * The fat-per-kg multiplier that was applied.
         */
        BigDecimal fatPerKg,

        /**
         * A human-readable label for the fat multiplier tier selected.
         *
         * <p>Examples: {@code "FAT_LOSS"}, {@code "SPORTS_HIGH_DEMAND"},
         * {@code "WEIGHT_GAIN_MODERATE"}.</p>
         */
        String calculationBasis,

        /**
         * Whether the calculated macro split is feasible —
         * i.e. remaining calories for carbohydrates are positive.
         *
         * <p>When {@code false} the caller must surface the
         * {@code MACRO_COMBINATION_INFEASIBLE} warning and adjust at
         * the orchestration layer.</p>
         */
        boolean feasible,

        /**
         * Overall safety status of the fat calculation.
         *
         * <p>Uses the same {@link CalorieSafetyStatus} taxonomy as the
         * calorie and protein safety pipelines for consistency.</p>
         */
        CalorieSafetyStatus safetyStatus,

        /**
         * Whether clinical review is required before this fat target is used.
         */
        boolean requiresClinicalReview,

        /**
         * Warnings or safety notices generated during the calculation.
         */
        List<FatWarning> warnings

) {
}
