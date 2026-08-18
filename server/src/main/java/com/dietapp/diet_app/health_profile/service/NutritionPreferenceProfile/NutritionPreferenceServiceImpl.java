package com.dietapp.diet_app.health_profile.service.NutritionPreferenceProfile;

import com.dietapp.diet_app.common.exception.EntityNotFoundException;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.NutritionPreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.NutritionPreferenceResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.NutritionPreferenceProfile;
import com.dietapp.diet_app.health_profile.mapper.NutritionPreferenceMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.NutritionPreferenceProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NutritionPreferenceServiceImpl
        implements NutritionPreferenceService {

    private final NutritionPreferenceProfileRepository
            nutritionPreferenceRepository;

    private final ProfileCompletionService  profileCompletionService;

    private final NutritionPreferenceMapper
            nutritionPreferenceMapper;

    private final HealthProfileContextService healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public NutritionPreferenceResponse saveOrUpdate(
            NutritionPreferenceRequest request
    ) {

        /*
         * Get the Health Profile belonging to
         * the currently authenticated user.
         */
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        /*
         * Find existing Nutrition Preference Profile.
         */
        NutritionPreferenceProfile profile =
                nutritionPreferenceRepository
                        .findByHealthProfileId(
                                healthProfile.getId()
                        )
                        .orElse(null);

        /*
         * CREATE
         */
        if (profile == null) {

            profile =
                    nutritionPreferenceMapper
                            .toEntity(request);

            profile.setHealthProfile(
                    healthProfile
            );
        }

        /*
         * UPDATE
         */
        else {

            nutritionPreferenceMapper.updateEntity(
                    request,
                    profile
            );
        }

        /*
         * Save profile.
         */
        profile =
                nutritionPreferenceRepository.save(profile);

        /*
         * Recalculate Health Profile completion.
         */
        profileCompletionService
                .refreshProfileCompletion(
                        healthProfile.getId()
                );

        return nutritionPreferenceMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NutritionPreferenceResponse> getMyProfile(
    ) {
        // check if exist else return exception
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();
        return nutritionPreferenceRepository.findByHealthProfileId(healthProfile.getId())
                .map(nutritionPreferenceMapper::toResponse);

    }

}
