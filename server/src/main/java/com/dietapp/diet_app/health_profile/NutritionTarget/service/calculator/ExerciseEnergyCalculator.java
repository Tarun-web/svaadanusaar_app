package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

@Component
public class ExerciseEnergyCalculator {

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

        if (weightKg == null
                || weightKg.signum() <= 0
                || activities == null
                || activities.isEmpty()) {

            return BigDecimal.ZERO;
        }

        double weeklyCalories = 0.0;

        for (ActivityProfile activity : activities) {

            if (activity == null
                    || activity.getDaysPerWeek() == null
                    || activity.getDurationMinutes() == null) {

                continue;
            }

            if (activity.getDaysPerWeek() <= 0
                    || activity.getDurationMinutes() <= 0) {

                continue;
            }

            double met = resolveMet(
                    activity.getActivityType(),
                    activity.getIntensity()
            );

            double caloriesPerSession =
                    met
                            * 3.5
                            * weightKg.doubleValue()
                            / 200.0
                            * activity.getDurationMinutes();

            weeklyCalories +=
                    caloriesPerSession
                            * activity.getDaysPerWeek();
        }

        /*
         * Convert weekly exercise expenditure
         * into average daily expenditure.
         */
        double dailyAverage =
                weeklyCalories / 7.0;

        return BigDecimal.valueOf(dailyAverage)
                .setScale(2, RoundingMode.HALF_UP);
    }


    private double resolveMet(
            ActivityType activityType,
            WorkoutIntensity intensity
    ) {

        if (activityType == null) {
            return 0.0;
        }

        double baseMet = switch (activityType) {

            case WALKING ->
                    3.5;

            case YOGA ->
                    2.5;

            case PILATES ->
                    3.0;

            case CYCLING ->
                    7.0;

            case RUNNING ->
                    9.0;

            case SWIMMING ->
                    7.0;

            case GYM,
                 HOME_WORKOUT ->
                    5.0;

            case SPORTS ->
                    8.0;

            case MARTIAL_ARTS ->
                    8.0;

            case PHYSICALLY_DEMANDING_JOB ->
                    6.0;

            case SEDENTARY ->
                    1.2;
        };

        double intensityMultiplier = switch (intensity) {

            case LOW ->
                    0.80;

            case MODERATE ->
                    1.00;

            case HIGH ->
                    1.15;

            case ATHLETE ->
                    1.30;
        };

        return baseMet * intensityMultiplier;
    }
}
