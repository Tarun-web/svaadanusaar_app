package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * The output of {@code WaterCalculator}.
 *
 * <h3>Formula (for traceability)</h3>
 * <pre>
 *   baselineWaterMl      = weightKg × 35 ml/kg
 *   exerciseWaterMl      = Σ(durationMinutes × daysPerWeek × intensityRate) / 7
 *   waterMl              = round10(baselineWaterMl + exerciseWaterMl)
 * </pre>
 *
 * <h3>Planning target</h3>
 * <p>This is a daily hydration planning target, not a medical prescription.
 * No climate, sweat-rate, caffeine, or medical adjustments are applied here.</p>
 */
public record WaterCalculationResult(

        /**
         * The final daily water target in millilitres, rounded to the nearest 10 ml.
         *
         * <p>This is the value that should be written to
         * {@code NutritionTarget.waterMl}.</p>
         */
        BigDecimal waterMl,

        /**
         * The body-weight component of the hydration target before rounding.
         *
         * <p>{@code weightKg × 35 ml/kg}, unrounded.</p>
         */
        BigDecimal baselineWaterMl,

        /**
         * The exercise-adjustment component of the hydration target before rounding.
         *
         * <p>Σ(durationMinutes × daysPerWeek × intensityRate) / 7, unrounded.</p>
         *
         * <p>Zero when no valid activities were supplied.</p>
         */
        BigDecimal exerciseWaterMl,

        /**
         * The total daily-average exercise minutes across all valid activities.
         *
         * <p>Σ(durationMinutes × daysPerWeek) / 7, unrounded.</p>
         *
         * <p>Provided for traceability. Zero when no valid activities.</p>
         */
        BigDecimal averageDailyExerciseMinutes,

        /**
         * {@code true} when the result was calculated from valid input.
         *
         * <p>Currently always {@code true} when the constructor succeeds —
         * the field is present for future extensibility (e.g. if upstream
         * constraints make a hydration target infeasible).</p>
         */
        boolean feasible,

        /**
         * Warnings generated during the calculation.
         *
         * <p>Examples: {@code NO_ACTIVITIES}, {@code INVALID_ACTIVITY_DATA}.</p>
         */
        List<WaterWarning> warnings

) {
}
