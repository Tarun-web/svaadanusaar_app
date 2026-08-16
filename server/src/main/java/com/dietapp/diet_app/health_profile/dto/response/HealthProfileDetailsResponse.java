package com.dietapp.diet_app.health_profile.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileDetailsResponse {

    private HealthProfileResponse healthProfile;

    private PersonalProfileResponse personalProfile;

    private FitnessGoalResponse fitnessGoal;

    private NutritionPreferenceResponse nutritionPreference;

    private WorkoutProfileResponse workoutProfile;

    private MedicalProfileResponse medicalProfile;

    private CookingProfileResponse cookingProfile;

    private LifestylePreferenceResponse lifestylePreference;

    private SupplementProfileResponse supplementProfile;

}