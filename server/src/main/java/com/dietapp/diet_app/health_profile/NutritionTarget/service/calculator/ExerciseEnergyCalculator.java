package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.entity.ActivityProfile;

import java.math.BigDecimal;
import java.util.Collection;

public class ExcerciseEnergyCalculator {

    /**
     * Calculates average daily calories burned from
     * structured exercise.
     *
     * Formula:
     *
     * Calories =
     * MET × 3.5 × bodyWeightKg / 200 × durationMinutes
     *
     * We calculate weekly exercise expenditure and
     * convert it to an average daily value.
     */
    public BigDecimal calculateDailyAverage(
            BigDecimal weightKg,
            Collection<ActivityProfile> activities
    ) {
        // 
        if (weightKg == null
                || weightKg.signum() <= 0
                || activities == null
                || activities.isEmpty()) {

            return BigDecimal.ZERO;
        }
    }
}
