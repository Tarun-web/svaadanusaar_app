package com.dietapp.diet_app.health_profile.service.ProfileCompletion;

import com.dietapp.diet_app.common.exception.ResourceNotFoundException;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileCompletionServiceImpl
        implements ProfileCompletionService {

    private final PersonalProfileRepository personalRepository;
    private final FitnessGoalProfileRepository fitnessRepository;
    private final NutritionPreferenceProfileRepository nutritionRepository;
    private final WorkoutProfileRepository workoutRepository;
    private final MedicalProfileRepository medicalRepository;
    private final CookingProfileRepository cookingRepository;
    private final LifestylePreferenceProfileRepository lifestyleRepository;
    private final SupplementProfileRepository supplementRepository;
    private final HealthProfileRepository healthProfileRepository;

    @Override
    public Integer calculateProfileCompletion(UUID healthProfileId) {

        int completedSections = 0;
        int totalSections = 8;

        if (personalRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (fitnessRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (nutritionRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (workoutRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (medicalRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (cookingRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (lifestyleRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        if (supplementRepository.existsByHealthProfileId(healthProfileId))
            completedSections++;

        return (completedSections * 100) / totalSections;

    }

    @Override
    @Transactional
    public void refreshProfileCompletion(UUID healthProfileId) {

        HealthProfile healthProfile =
                healthProfileRepository
                        .findById(healthProfileId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Health Profile not found."
                                )
                        );

        Integer completion =
                calculateProfileCompletion(
                        healthProfileId
                );

        healthProfile.setProfileCompletionPercentage(
                completion
        );

        healthProfile.setOnboardingCompleted(
                completion == 100
        );

        healthProfileRepository.save(
                healthProfile
        );
    }

}
