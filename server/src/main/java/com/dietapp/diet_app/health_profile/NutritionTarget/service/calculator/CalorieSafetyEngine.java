package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;


import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieWarningCode;
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

        List<CalorieWarning> warnings =
                new ArrayList<>();

        BigDecimal minimumCalories =
                DEFAULT_MIN_CALORIES;

        BigDecimal maximumCalories =
                DEFAULT_MAX_CALORIES;

        BigDecimal maximumDailyDeficit =
                DEFAULT_MAX_DEFICIT;

        BigDecimal maximumWeeklyWeightLoss =
                personalProfile
                        .getWeightKg()
                        .multiply(
                                DEFAULT_MAX_WEIGHT_LOSS
                        );

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
                        new CalorieWarning(
                                CalorieWarningCode.LOW_BODY_WEIGHT,
                                "Current BMI is below the standard adult healthy-weight range. " +
                                        "Aggressive weight loss is not recommended."
                        )
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
                        new CalorieWarning(
                                CalorieWarningCode.HIGH_BODY_WEIGHT,
                                "Higher BMI detected. Weight-loss targets should be individualized."
                        )
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
                    new CalorieWarning(
                            CalorieWarningCode.MISSING_GENDER,
                            "Gender is missing. Energy estimation may be less precise."
                    )
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

                    warnings.add(
                            new CalorieWarning(
                                    CalorieWarningCode.CLINICAL_REVIEW_REQUIRED,
                                    "This medical condition requires individualized clinical " +
                                            "assessment before relying on an automated nutrition target."
                            )
                    );
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
                DEFAULT_MAX_SURPLUS,
                maximumWeeklyWeightLoss,
                DEFAULT_MAX_WEIGHT_GAIN,
                requiresClinicalReview,
                warnings
        );
    }

    public CalorieSafetyResult evaluate(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile,
            MedicalProfile medicalProfile,
            CalorieTargetResult calorieTarget
    ) {

        if (calorieTarget == null) {
            throw new IllegalArgumentException(
                    "Calorie target result is required."
            );
        }

        if (calorieTarget.getProposedCalories() == null) {
            throw new IllegalArgumentException(
                    "Proposed calories are required."
            );
        }

        CalorieSafetyConstraints constraints =
                resolve(
                        personalProfile,
                        fitnessGoalProfile,
                        medicalProfile
                );

        BigDecimal originalCalories =
                calorieTarget.getProposedCalories();

        BigDecimal finalCalories =
                originalCalories;

        boolean adjusted = false;

        List<CalorieWarning> warnings =
                new ArrayList<>(constraints.warnings());

        BigDecimal tdee =
                calorieTarget.getTdee();

        /*
         * ---------------------------------------------------------
         * 1. DETERMINE CALORIE BOUNDARIES
         * ---------------------------------------------------------
         */

        BigDecimal minimumCalories =
                constraints.minimumCalories();

        BigDecimal maximumCalories =
                constraints.maximumCalories();

        /*
         * Maximum allowable deficit based on TDEE.
         *
         * Example:
         *
         * TDEE = 2500
         * Maximum deficit = 750
         *
         * Minimum calories from deficit rule = 1750
         */
        if (tdee != null
                && constraints.maximumDailyDeficit() != null) {

            BigDecimal minimumFromDeficit =
                    tdee.subtract(
                            constraints.maximumDailyDeficit()
                    );

            /*
             * Never allow the deficit-derived minimum
             * to fall below the absolute calorie floor.
             */
            minimumCalories =
                    minimumCalories.max(
                            minimumFromDeficit
                    );
        }

        /*
         * Maximum allowable surplus.
         *
         * Example:
         *
         * TDEE = 2500
         * Maximum surplus = 500
         *
         * Maximum calories = 3000
         */
        if (tdee != null
                && constraints.maximumDailySurplus() != null) {

            BigDecimal maximumFromSurplus =
                    tdee.add(
                            constraints.maximumDailySurplus()
                    );

            /*
             * Never allow the surplus-derived maximum
             * to exceed the absolute calorie ceiling.
             */
            maximumCalories =
                    maximumCalories.min(
                            maximumFromSurplus
                    );
        }

        /*
         * Safety check.
         */
        if (minimumCalories.compareTo(
                maximumCalories
        ) > 0) {

            maximumCalories =
                    minimumCalories;
        }


        /*
         * ---------------------------------------------------------
         * 2. MINIMUM CALORIE CHECK
         * ---------------------------------------------------------
         */

        if (finalCalories.compareTo(
                minimumCalories
        ) < 0) {

            finalCalories =
                    minimumCalories;

            adjusted = true;

            warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.CALORIE_TARGET_ADJUSTED,
                            "The proposed calorie target was below "
                                    + "the applicable minimum safety "
                                    + "boundary and has been adjusted."
                    )
            );
        }


        /*
         * ---------------------------------------------------------
         * 3. MAXIMUM CALORIE CHECK
         * ---------------------------------------------------------
         */

        if (finalCalories.compareTo(
                maximumCalories
        ) > 0) {

            finalCalories =
                    maximumCalories;

            adjusted = true;

            warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.CALORIE_TARGET_ADJUSTED,
                            "The proposed calorie target exceeded "
                                    + "the applicable maximum safety "
                                    + "boundary and has been adjusted."
                    )
            );
        }


        /*
         * ---------------------------------------------------------
         * 4. WEEKLY WEIGHT-CHANGE VALIDATION
         * ---------------------------------------------------------
         */

        BigDecimal requestedWeeklyChange =
                toBigDecimal(
                        fitnessGoalProfile
                                .getWeeklyWeightChangeKg()
                );

        Goal goal =
                fitnessGoalProfile.getPrimaryGoal();

        if (requestedWeeklyChange != null) {

            switch (goal) {

                case FAT_LOSS -> {

                    BigDecimal maximumAllowedLoss =
                            constraints.maximumWeeklyWeightLoss();

                    if (maximumAllowedLoss != null
                            && requestedWeeklyChange.compareTo(
                            maximumAllowedLoss
                    ) > 0) {

                        warnings.add(
                                new CalorieWarning(
                                        CalorieWarningCode
                                                .TARGET_RATE_TOO_AGGRESSIVE,
                                        "The requested weekly weight-loss "
                                                + "rate exceeds the applicable "
                                                + "safety boundary."
                                )
                        );
                    }
                }

                case WEIGHT_GAIN,
                     MUSCLE_GAIN -> {

                    BigDecimal maximumAllowedGain =
                            constraints.maximumWeeklyWeightGain();

                    if (maximumAllowedGain != null
                            && requestedWeeklyChange.compareTo(
                            maximumAllowedGain
                    ) > 0) {

                        warnings.add(
                                new CalorieWarning(
                                        CalorieWarningCode
                                                .TARGET_RATE_TOO_AGGRESSIVE,
                                        "The requested weekly weight-gain "
                                                + "rate exceeds the applicable "
                                                + "safety boundary."
                                )
                        );
                    }
                }

                default -> {
                    // Weekly weight change is not relevant
                    // to maintenance/general-health goals.
                }
            }
        }


        /*
         * ---------------------------------------------------------
         * 5. TIMELINE VALIDATION
         * ---------------------------------------------------------
         */

        if (!calorieTarget.isTimelineAchievable()) {

            warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.TARGET_DATE_TOO_SOON,
                            "The requested target date is not achievable "
                                    + "at the calculated weekly rate. "
                                    + "A longer timeline is recommended."
                    )
            );
        }


        /*
         * ---------------------------------------------------------
         * 6. DETERMINE FINAL STATUS
         * ---------------------------------------------------------
         */

        CalorieSafetyStatus status;

        if (constraints.requiresClinicalReview()) {

            status = CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED;

        } else if (adjusted) {

            status = CalorieSafetyStatus.ADJUSTED;

        } else if (!warnings.isEmpty()) {

            status = CalorieSafetyStatus.WARNING;

        } else {

            status = CalorieSafetyStatus.SAFE;
        }


        /*
         * ---------------------------------------------------------
         * 7. BUILD RESULT
         * ---------------------------------------------------------
         */

        return new CalorieSafetyResult(
                originalCalories,
                finalCalories,
                status,
                adjusted,
                constraints.requiresClinicalReview(),
                List.copyOf(warnings)
        );
    }

    private BigDecimal toBigDecimal(Double value) {

        return value == null
                ? null
                : BigDecimal.valueOf(value);
    }

    private void applyMedicalConstraint(
            MedicalCondition condition,
            List<CalorieWarning> warnings
    ) {

        switch (condition) {

            case DIABETES -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Diabetes detected. Calorie targets and carbohydrate distribution " +
                                    "should be individualized."
                    )
            );

            case PREDIABETES -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Prediabetes detected. Weight-loss nutrition should emphasize " +
                                    "overall diet quality and individualized carbohydrate management."
                    )
            );

            case HYPERTENSION -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Hypertension detected. Sodium and overall dietary pattern " +
                                    "should be considered in meal planning."
                    )
            );

            case HIGH_CHOLESTEROL -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "High cholesterol detected. Fat quality and dietary pattern " +
                                    "should be considered."
                    )
            );

            case FATTY_LIVER -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Fatty liver detected. Weight-management and dietary quality " +
                                    "should be handled carefully."
                    )
            );

            case HYPOTHYROID -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Hypothyroidism detected. Calorie targets should consider " +
                                    "the individual's clinical context and treatment."
                    )
            );

            case HYPERTHYROID -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Hyperthyroidism detected. Energy requirements may require " +
                                    "individualized clinical assessment."
                    )
            );

            case PCOS -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "PCOS detected. Weight-management and dietary strategy " +
                                    "should be individualized."
                    )
            );

            case IBS -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "IBS detected. Calorie target alone is insufficient; " +
                                    "food tolerance and symptom triggers should also be considered."
                    )
            );

            case GERD -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "GERD detected. Meal composition and timing should be considered."
                    )
            );

            case CELIAC -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Celiac disease detected. Gluten exclusion must be enforced."
                    )
            );

            case GOUT -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Gout detected. Food selection and hydration considerations " +
                                    "are important."
                    )
            );

            case KIDNEY_DISEASE -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Kidney disease detected. Automated nutrition targets " +
                                    "should not be treated as a clinical prescription."
                    )
            );

            case LIVER_DISEASE -> warnings.add(
                    new CalorieWarning(
                            CalorieWarningCode.MEDICAL_CONDITION_PRESENT,
                            "Liver disease detected. Nutrition targets require individualized " +
                                    "clinical consideration."
                    )
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
