package com.dietapp.diet_app.health_profile.service.HealthProfile;

import com.dietapp.diet_app.common.exception.ResourceAlreadyExistsException;
import com.dietapp.diet_app.common.exception.ResourceNotFoundException;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileDetailsResponse;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.HealthProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.CookingProfile.CookingProfileService;
import com.dietapp.diet_app.health_profile.service.FitnessGoalProfile.FitnessGoalService;
import com.dietapp.diet_app.health_profile.service.LifestylePreferenceProfile.LifestylePreferenceService;
import com.dietapp.diet_app.health_profile.service.MedicalProfile.MedicalProfileService;
import com.dietapp.diet_app.health_profile.service.NutritionPreferenceProfile.NutritionPreferenceService;
import com.dietapp.diet_app.health_profile.service.PersonalProfile.PersonalProfileService;
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
import com.dietapp.diet_app.health_profile.service.SupplementProfile.SupplementProfileService;
import com.dietapp.diet_app.health_profile.service.WorkoutProfile.WorkoutProfileService;
import com.dietapp.diet_app.user.entity.User;
import com.dietapp.diet_app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HealthProfileServiceImpl
        implements HealthProfileService {

    private final UserRepository userRepository;

    private final HealthProfileRepository healthProfileRepository;

    private final HealthProfileMapper healthProfileMapper;

    private final AuthenticationFacade authenticationFacade;

    /*
     * Used to calculate and refresh the onboarding
     * completion percentage.
     */
    private final ProfileCompletionService profileCompletionService;

    /*
     * Individual profile services.
     *
     * These are used when building the complete
     * HealthProfileDetailsResponse.
     */
    private final PersonalProfileService personalProfileService;

    private final FitnessGoalService fitnessGoalService;

    private final NutritionPreferenceService
            nutritionPreferenceService;

    private final WorkoutProfileService workoutProfileService;

    private final MedicalProfileService medicalProfileService;

    private final CookingProfileService cookingProfileService;

    private final LifestylePreferenceService
            lifestylePreferenceService;

    private final SupplementProfileService
            supplementProfileService;


    /**
     * Creates an empty Health Profile for the
     * currently authenticated user.
     *
     * The user is identified using the JWT.
     */
    @Override
    public HealthProfileResponse createHealthProfile() {

        /*
         * Get the currently authenticated user's ID
         * from the JWT.
         */
        UUID userId =
                authenticationFacade.getCurrentUserId();

        /*
         * Make sure the User still exists.
         */
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found."
                                )
                        );

        /*
         * A user can have only one Health Profile.
         */
        if (healthProfileRepository.existsByUserId(userId)) {

            throw new ResourceAlreadyExistsException(
                    "Health Profile already exists for this user."
            );
        }

        /*
         * Create an empty Health Profile.
         *
         * The actual profile sections will be created
         * later as the user completes onboarding.
         */
        HealthProfile healthProfile =
                HealthProfile.builder()
                        .user(user)
                        .build();

        /*
         * The entity defaults will set:
         *
         * onboardingCompleted = false
         * profileCompletionPercentage = 0
         */
        healthProfile =
                healthProfileRepository.save(
                        healthProfile
                );

        return healthProfileMapper.toResponse(
                healthProfile
        );
    }

    @Override
    public HealthProfileDetailsResponse getHealthProfile(UUID healthProfileId) {
        HealthProfile healthProfile = healthProfileRepository.findById(healthProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Health Profile not found."));
        return buildHealthProfileDetails(healthProfile);
    }


    /**
     * Gets the complete Health Profile of the
     * currently authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public HealthProfileDetailsResponse getMyHealthProfile() {

        /*
         * Get current user from JWT.
         */
        UUID userId =
                authenticationFacade.getCurrentUserId();

        /*
         * Find Health Profile belonging to
         * the authenticated user.
         */
        HealthProfile healthProfile =
                healthProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Health Profile not found."
                                )
                        );

        /*
         * Build the complete response.
         */
        return buildHealthProfileDetails(
                healthProfile
        );
    }


    /**
     * Gets a Health Profile summary for the
     * currently authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public HealthProfileResponse
    getMyHealthProfileSummary() {

        UUID userId =
                authenticationFacade.getCurrentUserId();

        HealthProfile healthProfile =
                healthProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Health Profile not found."
                                )
                        );

        return healthProfileMapper.toResponse(
                healthProfile
        );
    }


    /**
     * Deletes the Health Profile of the
     * currently authenticated user.
     */
    @Override
    public void deleteMyHealthProfile() {

        UUID userId =
                authenticationFacade.getCurrentUserId();

        HealthProfile healthProfile =
                healthProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Health Profile not found."
                                )
                        );

        healthProfileRepository.delete(
                healthProfile
        );
    }


    /**
     * Refreshes the profile completion percentage.
     *
     * This method delegates the actual calculation
     * and persistence to ProfileCompletionService.
     *
     * Other profile services call this method after
     * successfully saving their section.
     */
    @Override
    public void refreshProfileCompletion(
            UUID healthProfileId
    ) {

        /*
         * ProfileCompletionService owns the
         * completion calculation and update.
         */
        profileCompletionService
                .refreshProfileCompletion(
                        healthProfileId
                );
    }


    /**
     * Builds the complete Health Profile response.
     *
     * This method is kept private because normal users
     * should access their own profile through /me.
     */

    private HealthProfileDetailsResponse buildHealthProfileDetails(
            HealthProfile healthProfile
    ) {

        return HealthProfileDetailsResponse.builder()

                .healthProfile(
                        healthProfileMapper.toResponse(
                                healthProfile
                        )
                )

                .personalProfile(
                        personalProfileService
                                .getMyProfile()
                                .orElse(null)
                )

                .fitnessGoal(
                        fitnessGoalService
                                .getMyProfile()
                                .orElse(null)
                )

                .nutritionPreference(
                        nutritionPreferenceService
                                .getMyProfile()
                                .orElse(null)
                )

                .workoutProfile(
                        workoutProfileService
                                .getMyProfile()
                                .orElse(null)
                )

                .medicalProfile(
                        medicalProfileService
                                .getMyProfile()
                                .orElse(null)
                )

                .cookingProfile(
                        cookingProfileService
                                .getMyProfile()
                                .orElse(null)
                )

                .lifestylePreference(
                        lifestylePreferenceService
                                .getMyProfile()
                                .orElse(null)
                )

                .supplementProfile(
                        supplementProfileService
                                .getMyProfile()
                                .orElse(null)
                )

                .build();
    }
}