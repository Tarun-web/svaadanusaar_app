package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.WaterCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.WaterWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.WaterWarningCode;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calculates the user's daily water (hydration) planning target in millilitres.
 *
 * <h3>Formula</h3>
 * <pre>
 *   baselineWaterMl = weightKg × 35 ml/kg
 *
 *   exerciseWaterMl = Σ( durationMinutes × daysPerWeek × intensityRate ) / 7
 *
 *   totalWaterMl    = round10( baselineWaterMl + exerciseWaterMl )
 * </pre>
 *
 * <h3>Intensity hydration rates</h3>
 * <pre>
 *   LOW      = 4 ml/minute
 *   MODERATE = 6 ml/minute
 *   HIGH     = 8 ml/minute
 *   ATHLETE  = 10 ml/minute
 * </pre>
 *
 * <h3>Rounding</h3>
 * <p>The final result is rounded to the nearest 10 ml using
 * {@link RoundingMode#HALF_UP} and BigDecimal only.
 * No intermediate value uses {@code double} or {@code float}.</p>
 *
 * <h3>Planning target — NOT a medical prescription</h3>
 * <p>This is a daily hydration planning target only.
 * No climate, sweat-rate, caffeine, creatine, or medical
 * adjustments are applied here. Those belong to downstream layers.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Compute baseline hydration from body weight.</li>
 *   <li>Aggregate exercise hydration across all valid activities.</li>
 *   <li>Validate input and reject malformed data.</li>
 *   <li>Round the final target to the nearest 10 ml.</li>
 * </ul>
 *
 * <h3>NOT responsible for</h3>
 * <ul>
 *   <li>Medical hydration limits or restrictions.</li>
 *   <li>Climate or environment adjustments.</li>
 *   <li>Sweat-rate estimation.</li>
 *   <li>Supplement compensation (creatine, caffeine, etc.).</li>
 *   <li>Calculating calories, protein, fat, carbohydrates, or fibre.</li>
 *   <li>Database persistence.</li>
 * </ul>
 */
@Component
public class WaterCalculator {

    // ================================================================
    // Constants — all centralised here for easy adjustment
    // ================================================================

    /** Baseline hydration rate: millilitres of water per kilogram of body weight per day. */
    public static final BigDecimal WATER_ML_PER_KG = new BigDecimal("35");

    /** Days in a week — used for converting weekly exercise volume to a daily average. */
    public static final BigDecimal DAYS_PER_WEEK = new BigDecimal("7");

    /** Hydration rate during LOW intensity exercise (ml per minute of activity). */
    public static final BigDecimal RATE_LOW      = new BigDecimal("4");

    /** Hydration rate during MODERATE intensity exercise (ml per minute of activity). */
    public static final BigDecimal RATE_MODERATE = new BigDecimal("6");

    /** Hydration rate during HIGH intensity exercise (ml per minute of activity). */
    public static final BigDecimal RATE_HIGH     = new BigDecimal("8");

    /** Hydration rate during ATHLETE intensity exercise (ml per minute of activity). */
    public static final BigDecimal RATE_ATHLETE  = new BigDecimal("10");

    /**
     * Rounding increment for the final water target (nearest 10 ml).
     *
     * <p>Example: 2806 → 2810, 2814 → 2810, 2816 → 2820.</p>
     */
    private static final BigDecimal ROUNDING_INCREMENT = new BigDecimal("10");

    // ================================================================
    // calculate()
    // ================================================================

    /**
     * Calculates the daily water planning target.
     *
     * @param input validated water calculation input; must not be {@code null}
     * @return a {@link WaterCalculationResult} containing the target and
     *         traceability information
     * @throws IllegalArgumentException when {@code input} or {@code weightKg}
     *         is null, or when {@code weightKg} is &le; 0
     */
    public WaterCalculationResult calculate(WaterCalculationInput input) {

        validateInput(input);

        List<WaterWarning> warnings = new ArrayList<>();

        // ── 1. Baseline from body weight ──────────────────────────────
        BigDecimal baselineWaterMl = input.weightKg()
                .multiply(WATER_ML_PER_KG);

        // ── 2. Exercise adjustment from all activities ────────────────
        Set<ActivityProfile> activities = input.activities();
        boolean hasActivities = activities != null && !activities.isEmpty();

        BigDecimal weeklyExerciseWaterMl   = BigDecimal.ZERO;
        BigDecimal weeklyExerciseMinutes   = BigDecimal.ZERO;
        int invalidActivityCount           = 0;

        if (hasActivities) {
            for (ActivityProfile activity : activities) {

                // Defensive validation of each entry
                if (!isValidActivity(activity)) {
                    invalidActivityCount++;
                    continue;
                }

                BigDecimal duration  = BigDecimal.valueOf(activity.getDurationMinutes());
                BigDecimal days      = BigDecimal.valueOf(activity.getDaysPerWeek());
                BigDecimal rate      = hydrationRate(activity.getIntensity());

                // weeklyWater for this activity = duration × days × rate
                weeklyExerciseWaterMl = weeklyExerciseWaterMl
                        .add(duration.multiply(days).multiply(rate));

                // weekly minutes for this activity = duration × days
                weeklyExerciseMinutes = weeklyExerciseMinutes
                        .add(duration.multiply(days));
            }
        }

        if (!hasActivities) {
            warnings.add(new WaterWarning(
                    WaterWarningCode.NO_ACTIVITIES,
                    "No activity data was provided. " +
                    "The water target is based on body weight only."
            ));
        }

        if (invalidActivityCount > 0) {
            warnings.add(new WaterWarning(
                    WaterWarningCode.INVALID_ACTIVITY_DATA,
                    invalidActivityCount + " activity record(s) contained invalid data " +
                    "(null intensity, non-positive duration, or non-positive days per week) " +
                    "and were excluded from the exercise hydration calculation."
            ));
        }

        // ── 3. Daily averages ─────────────────────────────────────────
        // Divide weekly totals by 7 (exact; scale 6 to preserve precision)
        BigDecimal exerciseWaterMl = weeklyExerciseWaterMl
                .divide(DAYS_PER_WEEK, 6, RoundingMode.HALF_UP);

        BigDecimal averageDailyExerciseMinutes = weeklyExerciseMinutes
                .divide(DAYS_PER_WEEK, 6, RoundingMode.HALF_UP);

        // ── 4. Total and round to nearest 10 ml ──────────────────────
        BigDecimal rawTotal = baselineWaterMl.add(exerciseWaterMl);
        BigDecimal waterMl  = roundToNearest10(rawTotal);

        return new WaterCalculationResult(
                waterMl,
                baselineWaterMl,
                exerciseWaterMl,
                averageDailyExerciseMinutes,
                true,
                List.copyOf(warnings)
        );
    }

    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Returns the hydration rate (ml/minute) for the given intensity.
     * All four enum values are handled explicitly — no default fall-through.
     */
    private BigDecimal hydrationRate(WorkoutIntensity intensity) {
        return switch (intensity) {
            case LOW      -> RATE_LOW;
            case MODERATE -> RATE_MODERATE;
            case HIGH     -> RATE_HIGH;
            case ATHLETE  -> RATE_ATHLETE;
        };
    }

    /**
     * Returns {@code true} when the activity entry is safe to use.
     *
     * <p>An activity is invalid when:
     * <ul>
     *   <li>intensity is {@code null}</li>
     *   <li>durationMinutes is {@code null} or &le; 0</li>
     *   <li>daysPerWeek is {@code null} or &le; 0</li>
     * </ul>
     */
    private boolean isValidActivity(ActivityProfile activity) {
        if (activity == null) {
            return false;
        }
        if (activity.getIntensity() == null) {
            return false;
        }
        if (activity.getDurationMinutes() == null
                || activity.getDurationMinutes() <= 0) {
            return false;
        }
        if (activity.getDaysPerWeek() == null
                || activity.getDaysPerWeek() <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Rounds {@code value} to the nearest 10 ml using {@link RoundingMode#HALF_UP}.
     *
     * <p>Uses only BigDecimal arithmetic — no floating-point types.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>2806 → 2810</li>
     *   <li>2814 → 2810</li>
     *   <li>2815 → 2820 (HALF_UP)</li>
     *   <li>2816 → 2820</li>
     * </ul>
     * </p>
     */
    static BigDecimal roundToNearest10(BigDecimal value) {
        return value
                .divide(ROUNDING_INCREMENT, 0, RoundingMode.HALF_UP)
                .multiply(ROUNDING_INCREMENT);
    }

    /**
     * Validates the top-level input and throws {@link IllegalArgumentException}
     * on the first violation.
     *
     * <p>Individual activity validation is handled gracefully inside
     * {@link #calculate} — invalid activities emit a warning and are skipped
     * rather than causing a hard failure.</p>
     */
    private void validateInput(WaterCalculationInput input) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "WaterCalculationInput must not be null."
            );
        }

        if (input.weightKg() == null) {
            throw new IllegalArgumentException(
                    "Weight is required for water calculation."
            );
        }

        if (input.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Weight must be greater than 0. Received: " + input.weightKg()
            );
        }
    }
}
