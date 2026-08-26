package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieSafetyResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.NutritionTargetResponse;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.CalorieSafetyEngine;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.GoalCalorieCalculator;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionTargetServiceImpl
        implements NutritionTargetService {

    private final HealthProfileContextService healthProfileContextService;

    private final GoalCalorieCalculator goalCalorieCalculator;

    private final CalorieSafetyEngine calorieSafetyEngine;

    @Override
    public NutritionTargetResponse calculateForCurrentUser() {

        /*
         * ---------------------------------------------------------
         * 1. GET CURRENT USER HEALTH PROFILE
         * ---------------------------------------------------------
         */

        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        if (healthProfile == null) {
            throw new IllegalStateException(
                    "Health profile is required to calculate nutrition targets."
            );
        }


        /*
         * ---------------------------------------------------------
         * 2. GET PROFILE SECTIONS
         * ---------------------------------------------------------
         */

        PersonalProfile personalProfile =
                healthProfile.getPersonalProfile();

        FitnessGoalProfile fitnessGoalProfile =
                healthProfile.getFitnessGoalProfile();

        MedicalProfile medicalProfile =
                healthProfile.getMedicalProfile();


        /*
         * ---------------------------------------------------------
         * 3. GET ACTIVITIES
         * ---------------------------------------------------------
         */

        Collection<ActivityProfile> activities =
                healthProfile.getWorkoutProfile() != null
                        && healthProfile.getWorkoutProfile().getActivities() != null

                        ? healthProfile
                        .getWorkoutProfile()
                        .getActivities()

                        : java.util.Collections.emptyList();


        /*
         * ---------------------------------------------------------
         * 4. VALIDATE REQUIRED PROFILE DATA
         * ---------------------------------------------------------
         */

        validateProfile(
                personalProfile,
                fitnessGoalProfile
        );


        /*
         * ---------------------------------------------------------
         * 5. CALCULATE GOAL CALORIE PROPOSAL
         * ---------------------------------------------------------
         */

        CalorieTargetResult calorieProposal =
                goalCalorieCalculator.calculate(
                        personalProfile,
                        fitnessGoalProfile,
                        activities
                );


        /*
         * ---------------------------------------------------------
         * 6. RUN SAFETY ENGINE
         * ---------------------------------------------------------
         */

        CalorieSafetyResult safetyResult =
                calorieSafetyEngine.evaluate(
                        personalProfile,
                        fitnessGoalProfile,
                        medicalProfile,
                        calorieProposal
                );


        /*
         * ---------------------------------------------------------
         * 7. BUILD FINAL RESPONSE
         * ---------------------------------------------------------
         */

        return NutritionTargetResponse.builder()

                .tdee(
                        calorieProposal.getTdee()
                )

                .proposedCalories(
                        safetyResult.originalCalories()
                )

                .finalCalories(
                        safetyResult.finalCalories()
                )

                .safetyAdjusted(
                        safetyResult.safetyAdjusted()
                )

                .status(
                        safetyResult.status()
                )

                .requiresClinicalReview(
                        safetyResult.requiresClinicalReview()
                )

                .warnings(
                        safetyResult.warnings()
                )

                .build();
    }


    private void validateProfile(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile
    ) {

        if (personalProfile == null) {

            throw new IllegalStateException(
                    "Personal profile is incomplete."
            );
        }

        if (fitnessGoalProfile == null) {

            throw new IllegalStateException(
                    "Fitness goal profile is incomplete."
            );
        }

        if (personalProfile.getWeightKg() == null) {

            throw new IllegalStateException(
                    "Current weight is required."
            );
        }

        if (personalProfile.getHeightCm() == null) {

            throw new IllegalStateException(
                    "Height is required."
            );
        }

        if (personalProfile.getGender() == null) {

            throw new IllegalStateException(
                    "Gender is required."
            );
        }
    }
}
