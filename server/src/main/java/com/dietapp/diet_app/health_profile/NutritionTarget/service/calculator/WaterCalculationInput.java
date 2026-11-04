package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.entity.ActivityProfile;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Input DTO for {@link WaterCalculator}.
 *
 * <p>Intentionally narrow — contains only the data the calculator
 * actually needs. The caller is responsible for extracting these
 * fields from {@code HealthProfile} / {@code WorkoutProfile} before
 * constructing this record.</p>
 *
 * <h3>Activities</h3>
 * <p>Pass {@code null} or an empty set when no workout data is available.
 * The calculator treats both as "no activities" and returns a
 * baseline-only hydration target with a {@code NO_ACTIVITIES} warning.</p>
 */
public record WaterCalculationInput(

        /**
         * Current body weight in kilograms. Must be &gt; 0.
         */
        BigDecimal weightKg,

        /**
         * The user's set of activity profiles.
         *
         * <p>May be {@code null} or empty — both are treated as
         * "no activities".</p>
         */
        Set<ActivityProfile> activities

) {
}
