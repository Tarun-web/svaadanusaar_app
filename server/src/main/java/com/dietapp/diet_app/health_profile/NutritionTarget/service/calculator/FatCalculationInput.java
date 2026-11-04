package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Input DTO for {@link FatCalculator}.
 *
 * <p>Follows the same record convention as {@link ProteinCalculationInput}.</p>
 *
 * <p>Contains only what {@link FatCalculator} actually needs — body weight,
 * goal, training context, and the already-established calorie + protein targets
 * required for macro feasibility checking.</p>
 *
 * <h3>Protein grams</h3>
 * <p>Must be the <em>current adaptation target</em> from
 * {@code ProteinAdaptationResult.currentTargetGrams}, NOT the long-term target.
 * Fat feasibility is always checked against what the user is actually
 * recommended to eat right now.</p>
 */
public record FatCalculationInput(

        /**
         * Body weight in kilograms. Must be > 0.
         */
        BigDecimal weightKg,

        /**
         * Primary fitness goal. Must not be {@code null}.
         */
        Goal goal,

        /**
         * The user's current recommended daily calorie target.
         *
         * <p>Used for macro feasibility checking.
         * Must be > 0.</p>
         */
        BigDecimal targetCalories,

        /**
         * The user's current recommended daily protein target in grams.
         *
         * <p>This must be the <strong>current adaptation target</strong>
         * (from {@code ProteinAdaptationResult.currentTargetGrams}),
         * not the long-term goal.</p>
         *
         * <p>Must be &ge; 0.</p>
         */
        BigDecimal proteinGrams,

        /**
         * Whether the user performs resistance / weight training.
         * {@code false} when unknown/not applicable.
         */
        boolean resistanceTraining,

        /**
         * Workout intensity of the primary activity.
         * Optional — used to determine training-demand tier for
         * WEIGHT_GAIN and SPORTS_PERFORMANCE goals.
         */
        WorkoutIntensity workoutIntensity,

        /**
         * The sport the user participates in, if applicable.
         * Optional — used alongside {@link #workoutIntensity} to
         * contextualise SPORTS_PERFORMANCE fat targets.
         */
        SportType sportType,

        /**
         * The user's diagnosed medical conditions.
         * May be {@code null} or empty — treated as no conditions.
         */
        Set<MedicalCondition> medicalConditions

) {
}
