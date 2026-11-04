package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.TrainingStatus;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Input DTO for {@link ProteinCalculator}.
 *
 * <p>All fields that are optional are represented as {@code null}
 * and must be handled gracefully by the calculator.</p>
 *
 * <p>{@code currentProteinGrams} is the user's current daily intake
 * (user-provided estimate, or derived from food-log analysis).
 * It is intentionally nullable — when unknown the system must NOT
 * invent a value.</p>
 */
public record ProteinCalculationInput(

        /** Body weight in kilograms. Must be > 0. */
        BigDecimal weightKg,

        /**
         * Body-fat percentage (0–60 inclusive).
         * Optional — when {@code null} or outside valid range
         * the calculator falls back to total body weight.
         */
        BigDecimal bodyFatPercentage,

        /** Primary fitness goal. Must not be {@code null}. */
        Goal goal,

        /**
         * Whether the user performs resistance / weight training.
         * {@code false} when unknown/not applicable.
         */
        boolean resistanceTraining,

        /**
         * Workout intensity of the primary activity.
         * Optional — used to determine SPORTS_PERFORMANCE tier.
         */
        WorkoutIntensity workoutIntensity,

        /**
         * Training experience level.
         * Optional — used for context-aware defaults.
         */
        TrainingStatus trainingStatus,

        /**
         * The user's current daily protein intake in grams.
         * <strong>Optional</strong> — {@code null} means unknown.
         * The adaptation engine must not invent a value when absent.
         */
        BigDecimal currentProteinGrams,

        /**
         * The user's diagnosed medical conditions.
         * May be {@code null} or empty — treated as no conditions.
         */
        Set<MedicalCondition> medicalConditions

) {
}
