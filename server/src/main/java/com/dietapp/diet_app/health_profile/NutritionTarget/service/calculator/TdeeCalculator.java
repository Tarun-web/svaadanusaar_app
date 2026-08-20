package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.BmrResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.TdeeResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class TdeeCalculator {

    private final BmrCalculator bmrCalculator;

    private final ActivityLevelResolver activityLevelResolver;

    private final ActivityFactorStrategy activityFactorStrategy;

    private final NeatCalculator neatCalculator;

    private final TefCalculator tefCalculator;


    /**
     * Calculates Total Daily Energy Expenditure.
     *
     * Current model:
     *
     * BMR
     * + NEAT
     * + structured exercise
     * = energy before TEF
     *
     * TEF
     * = approximately 10% of energy before TEF
     *
     * TDEE
     * = energy before TEF + TEF
     */
    public TdeeResult calculate(
            PersonalProfile personalProfile,
            Collection<ActivityProfile> activities
    ) {

        if (personalProfile == null) {
            throw new IllegalArgumentException(
                    "Personal profile is required."
            );
        }

        /*
         * ---------------------------------------------------------
         * 1. BMR
         * ---------------------------------------------------------
         */
        BmrResult bmrResult =
                bmrCalculator.calculate(
                        personalProfile
                );

        BigDecimal bmr =
                bmrResult.bmr();


        /*
         * ---------------------------------------------------------
         * 2. ACTIVITY LEVEL
         * ---------------------------------------------------------
         *
         * This is still useful for classification and
         * reporting, but we do NOT use the activity factor
         * to multiply BMR here.
         */
        ActivityLevel activityLevel =
                activityLevelResolver.resolve(
                        personalProfile,
                        activities
                );


        /*
         * ---------------------------------------------------------
         * 3. ACTIVITY FACTOR
         * ---------------------------------------------------------
         *
         * Keep this value in the result for compatibility
         * and reporting.
         *
         * IMPORTANT:
         *
         * We do NOT multiply BMR by this factor below.
         *
         * Otherwise NEAT + exercise would be double-counted.
         */
        BigDecimal activityFactor =
                activityFactorStrategy.getFactor(
                        activityLevel
                );


        /*
         * ---------------------------------------------------------
         * 4. NEAT
         * ---------------------------------------------------------
         */
        BigDecimal neat =
                neatCalculator.calculate(
                        bmr,
                        personalProfile.getOccupation()
                );


        /*
         * ---------------------------------------------------------
         * 5. STRUCTURED EXERCISE
         * ---------------------------------------------------------
         */
        BigDecimal exerciseCalories =
                calculateExerciseCalories(
                        personalProfile,
                        activities
                );


        /*
         * ---------------------------------------------------------
         * 6. ENERGY BEFORE TEF
         * ---------------------------------------------------------
         *
         * BMR + NEAT + Exercise
         */
        BigDecimal energyBeforeTef =
                bmr
                        .add(neat)
                        .add(exerciseCalories)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        /*
         * ---------------------------------------------------------
         * 7. TEF
         * ---------------------------------------------------------
         */
        BigDecimal tef =
                tefCalculator.calculate(
                        energyBeforeTef
                );


        /*
         * ---------------------------------------------------------
         * 8. FINAL TDEE
         * ---------------------------------------------------------
         */
        BigDecimal tdee =
                energyBeforeTef
                        .add(tef)
                        .setScale(
                                0,
                                RoundingMode.HALF_UP
                        );


        return new TdeeResult(
                bmrResult.age(),
                bmr,
                activityLevel,
                activityFactor,
                neat,
                exerciseCalories,
                energyBeforeTef,
                tef,
                tdee
        );
    }


    /**
     * Calculates structured exercise expenditure.
     *
     * IMPORTANT:
     * This is currently a conservative estimation.
     *
     * It is intentionally separate from ActivityLevelResolver.
     */
    private BigDecimal calculateExerciseCalories(
            PersonalProfile personalProfile,
            Collection<ActivityProfile> activities
    ) {

        if (activities == null
                || activities.isEmpty()) {

            return BigDecimal.ZERO;
        }

        if (personalProfile.getWeightKg() == null
                || personalProfile.getWeightKg().signum() <= 0) {

            return BigDecimal.ZERO;
        }

        double bodyWeightKg =
                personalProfile
                        .getWeightKg()
                        .doubleValue();

        double totalWeeklyCalories = 0.0;

        for (ActivityProfile activity : activities) {

            if (activity == null
                    || activity.getDaysPerWeek() == null
                    || activity.getDurationMinutes() == null
                    || activity.getIntensity() == null
                    || activity.getActivityType() == null) {

                continue;
            }

            if (activity.getDaysPerWeek() <= 0
                    || activity.getDurationMinutes() <= 0) {

                continue;
            }

            /*
             * Approximate MET value.
             */
            double met =
                    resolveMet(
                            activity
                    );

            /*
             * Calories/minute:
             *
             * MET × 3.5 × body weight / 200
             */
            double caloriesPerMinute =
                    met
                            * 3.5
                            * bodyWeightKg
                            / 200.0;

            double sessionCalories =
                    caloriesPerMinute
                            * activity.getDurationMinutes();

            double weeklyCalories =
                    sessionCalories
                            * activity.getDaysPerWeek();

            totalWeeklyCalories += weeklyCalories;
        }

        /*
         * Convert weekly exercise expenditure
         * into average daily expenditure.
         */
        double dailyCalories =
                totalWeeklyCalories / 7.0;

        return BigDecimal
                .valueOf(dailyCalories)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    /**
     * Conservative MET estimates.
     *
     * These are intentionally kept separate so that
     * they can later be moved into configuration/database.
     */
    private double resolveMet(
            ActivityProfile activity
    ) {

        return switch (activity.getActivityType()) {

            case WALKING ->
                    switch (activity.getIntensity()) {
                        case LOW -> 2.5;
                        case MODERATE -> 3.5;
                        case HIGH -> 4.5;
                        case ATHLETE -> 5.0;
                    };

            case GYM,
                 HOME_WORKOUT ->
                    switch (activity.getIntensity()) {
                        case LOW -> 3.5;
                        case MODERATE -> 5.0;
                        case HIGH -> 6.0;
                        case ATHLETE -> 7.0;
                    };

            case RUNNING ->
                    switch (activity.getIntensity()) {
                        case LOW -> 6.0;
                        case MODERATE -> 8.0;
                        case HIGH -> 10.0;
                        case ATHLETE -> 12.0;
                    };

            case CYCLING ->
                    switch (activity.getIntensity()) {
                        case LOW -> 4.0;
                        case MODERATE -> 6.0;
                        case HIGH -> 8.0;
                        case ATHLETE -> 10.0;
                    };

            case SWIMMING ->
                    switch (activity.getIntensity()) {
                        case LOW -> 4.5;
                        case MODERATE -> 6.0;
                        case HIGH -> 8.0;
                        case ATHLETE -> 10.0;
                    };

            case SPORTS, MARTIAL_ARTS ->
                    switch (activity.getIntensity()) {
                        case LOW -> 5.0;
                        case MODERATE -> 7.0;
                        case HIGH -> 9.0;
                        case ATHLETE -> 11.0;
                    };

            case YOGA,
                 PILATES ->
                    switch (activity.getIntensity()) {
                        case LOW -> 2.5;
                        case MODERATE -> 3.0;
                        case HIGH -> 4.0;
                        case ATHLETE -> 5.0;
                    };

            /*
             * This should eventually be removed from
             * ActivityType because occupational activity
             * belongs to Occupation.
             */
            case PHYSICALLY_DEMANDING_JOB ->
                    switch (activity.getIntensity()) {
                        case LOW -> 3.0;
                        case MODERATE -> 4.0;
                        case HIGH -> 5.0;
                        case ATHLETE -> 6.0;
                    };

            /*
             * SEDENTARY is not structured exercise.
             */
            case SEDENTARY ->
                    1.2;
        };
    }
}