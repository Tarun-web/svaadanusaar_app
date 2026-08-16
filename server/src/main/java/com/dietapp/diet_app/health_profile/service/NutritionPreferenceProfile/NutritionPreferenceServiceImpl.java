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

    private final HealthProfileRepository
            healthProfileRepository;

    private final NutritionPreferenceMapper
            nutritionPreferenceMapper;

    private final HealthProfileContextService healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public NutritionPreferenceResponse saveOrUpdate(
            NutritionPreferenceRequest request
    ) {

        // Extract current user from JWT token
        UUID userId = authenticationFacade.getCurrentUserId();

        HealthProfile healthProfile =
                healthProfileRepository
                        .findById(request.getHealthProfileId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Health Profile not found."
                                ));

        // Verify ownership: health profile must belong to authenticated user
        if (!healthProfile.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException(
                    "Health Profile not found."
            );
        }

        NutritionPreferenceProfile profile =
                nutritionPreferenceRepository
                        .findByHealthProfileId(
                                request.getHealthProfileId()
                        )
                        .orElseGet(() -> {

                            NutritionPreferenceProfile entity =
                                    nutritionPreferenceMapper
                                            .toEntity(request);

                            entity.setHealthProfile(healthProfile);

                            return entity;

                        });

        if (profile.getId() != null) {

            nutritionPreferenceMapper.updateEntity(
                    request,
                    profile
            );

        }

        profile = nutritionPreferenceRepository.save(profile);

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
