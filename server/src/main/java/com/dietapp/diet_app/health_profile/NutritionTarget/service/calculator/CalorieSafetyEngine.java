package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;


import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieSafetyConstraints;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CalorieSafetyEngine {

    private static final BigDecimal DEFAULT_MIN_CALORIES =
            BigDecimal.valueOf(1200);

    private static final BigDecimal DEFAULT_MAX_CALORIES =
            BigDecimal.valueOf(5000);

    private static final BigDecimal DEFAULT_MAX_DEFICIT =
            BigDecimal.valueOf(1000);

    private static final BigDecimal DEFAULT_MAX_SURPLUS =
            BigDecimal.valueOf(500);

    private static final BigDecimal DEFAULT_MAX_WEIGHT_LOSS =
            BigDecimal.valueOf(0.01);

    private static final BigDecimal DEFAULT_MAX_WEIGHT_GAIN =
            BigDecimal.valueOf(0.50);


    public CalorieSafetyConstraints resolve(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile,
            MedicalProfile medicalProfile
    ) {

        validateInput(
                personalProfile,
                fitnessGoalProfile
        );

        List<String> warnings =
                new ArrayList<>();

        BigDecimal minimumCalories =
                DEFAULT_MIN_CALORIES;

        BigDecimal maximumCalories =
                DEFAULT_MAX_CALORIES;

        BigDecimal maximumDailyDeficit =
                DEFAULT_MAX_DEFICIT;

        BigDecimal maximumDailySurplus =
                DEFAULT_MAX_SURPLUS;

        BigDecimal maximumWeeklyWeightLoss =
                personalProfile
                        .getWeightKg()
                        .multiply(
                                DEFAULT_MAX_WEIGHT_LOSS
                        );

        BigDecimal maximumWeeklyWeightGain =
                DEFAULT_MAX_WEIGHT_GAIN;

        boolean requiresClinicalReview = false;


        /*
         * ---------------------------------------------------------
         * 1. BODY SIZE
         * ---------------------------------------------------------
         *
         * Body size affects how aggressive a calorie target
         * should be.
         *
         * We use BMI here only as a screening variable.
         *
         * BMI is NOT being treated as a diagnosis.
         */
        BigDecimal bmi =
                calculateBmi(
                        personalProfile
                );

        if (bmi != null) {

            /*
             * Lower body size:
             *
             * Don't allow extremely aggressive deficits.
             */
            if (bmi.compareTo(
                    BigDecimal.valueOf(18.5)
            ) < 0) {

                maximumDailyDeficit =
                        BigDecimal.valueOf(300);

                maximumWeeklyWeightLoss =
                        BigDecimal.valueOf(0.25);

                warnings.add(
                        "Current BMI is below the standard adult healthy-weight range. " +
                                "Aggressive weight loss is not recommended."
                );
            }

            /*
             * Higher BMI:
             *
             * We still don't automatically permit
             * unlimited deficits.
             */
            else if (bmi.compareTo(
                    BigDecimal.valueOf(30)
            ) >= 0) {

                warnings.add(
                        "Higher BMI detected. Weight-loss targets should be individualized."
                );
            }
        }


        /*
         * ---------------------------------------------------------
         * 2. SEX
         * ---------------------------------------------------------
         *
         * Sex affects estimated energy requirements.
         *
         * IMPORTANT:
         *
         * We do NOT arbitrarily subtract calories based
         * on sex.
         *
         * BMR/TDEE already account for sex.
         */
        Gender gender =
                personalProfile.getGender();

        if (gender == null) {

            warnings.add(
                    "Gender is missing. Energy estimation may be less precise."
            );
        }


        /*
         * ---------------------------------------------------------
         * 3. MEDICAL CONDITIONS
         * ---------------------------------------------------------
         */
        if (medicalProfile != null
                && medicalProfile.getMedicalConditions() != null) {

            for (MedicalCondition condition :
                    medicalProfile.getMedicalConditions()) {

                if (condition == null) {
                    continue;
                }

                applyMedicalConstraint(
                        condition,
                        warnings
                );

                if (requiresClinicalReview(condition)) {

                    requiresClinicalReview = true;
                }
            }
        }


        /*
         * ---------------------------------------------------------
         * 4. TARGET SAFETY
         * ---------------------------------------------------------
         *
         * Never allow a nonsensical range.
         */
        if (minimumCalories.compareTo(
                maximumCalories
        ) > 0) {

            maximumCalories =
                    minimumCalories;
        }


        return new CalorieSafetyConstraints(
                minimumCalories,
                maximumCalories,
                maximumDailyDeficit,
                maximumDailySurplus,
                maximumWeeklyWeightLoss,
                maximumWeeklyWeightGain,
                requiresClinicalReview,
                warnings
        );
    }


    private void applyMedicalConstraint(
            MedicalCondition condition,
            List<String> warnings
    ) {

        switch (condition) {

            case DIABETES -> warnings.add(
                    "Diabetes detected. Calorie targets and carbohydrate distribution " +
                            "should be individualized."
            );

            case PREDIABETES -> warnings.add(
                    "Prediabetes detected. Weight-loss nutrition should emphasize " +
                            "overall diet quality and individualized carbohydrate management."
            );

            case HYPERTENSION -> warnings.add(
                    "Hypertension detected. Sodium and overall dietary pattern " +
                            "should be considered in meal planning."
            );

            case HIGH_CHOLESTEROL -> warnings.add(
                    "High cholesterol detected. Fat quality and dietary pattern " +
                            "should be considered."
            );

            case FATTY_LIVER -> warnings.add(
                    "Fatty liver detected. Weight-management and dietary quality " +
                            "should be handled carefully."
            );

            case HYPOTHYROID -> warnings.add(
                    "Hypothyroidism detected. Calorie targets should consider " +
                            "the individual's clinical context and treatment."
            );

            case HYPERTHYROID -> warnings.add(
                    "Hyperthyroidism detected. Energy requirements may require " +
                            "individualized clinical assessment."
            );

            case PCOS -> warnings.add(
                    "PCOS detected. Weight-management and dietary strategy " +
                            "should be individualized."
            );

            case IBS -> warnings.add(
                    "IBS detected. Calorie target alone is insufficient; " +
                            "food tolerance and symptom triggers should also be considered."
            );

            case GERD -> warnings.add(
                    "GERD detected. Meal composition and timing should be considered."
            );

            case CELIAC -> warnings.add(
                    "Celiac disease detected. Gluten exclusion must be enforced."
            );

            case GOUT -> warnings.add(
                    "Gout detected. Food selection and hydration considerations " +
                            "are important."
            );

            case KIDNEY_DISEASE -> warnings.add(
                    "Kidney disease detected. Automated nutrition targets " +
                            "should not be treated as a clinical prescription."
            );

            case LIVER_DISEASE -> warnings.add(
                    "Liver disease detected. Nutrition targets require individualized "
                            +
                            "clinical consideration."
            );
        }
    }


    private boolean requiresClinicalReview(
            MedicalCondition condition
    ) {

        return switch (condition) {

            case KIDNEY_DISEASE,
                 LIVER_DISEASE,
                 HYPERTHYROID,
                 CELIAC -> true;

            default -> false;
        };
    }


    private BigDecimal calculateBmi(
            PersonalProfile personalProfile
    ) {

        if (personalProfile.getWeightKg() == null
                || personalProfile.getHeightCm() == null
                || personalProfile.getHeightCm() <= 0) {

            return null;
        }

        BigDecimal heightMeters =
                BigDecimal.valueOf(
                        personalProfile.getHeightCm()
                ).divide(
                        BigDecimal.valueOf(100),
                        4,
                        java.math.RoundingMode.HALF_UP
                );

        return personalProfile
                .getWeightKg()
                .divide(
                        heightMeters.multiply(heightMeters),
                        2,
                        java.math.RoundingMode.HALF_UP
                );
    }


    private void validateInput(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile
    ) {

        if (personalProfile == null) {

            throw new IllegalArgumentException(
                    "Personal profile is required."
            );
        }

        if (personalProfile.getWeightKg() == null) {

            throw new IllegalArgumentException(
                    "Current weight is required."
            );
        }

        if (fitnessGoalProfile == null) {

            throw new IllegalArgumentException(
                    "Fitness goal profile is required."
            );
        }
    }
}
