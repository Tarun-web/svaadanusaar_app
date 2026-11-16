package com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.CurrentNutritionProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrentNutritionProfileServiceImpl
        implements CurrentNutritionProfileService {

    private final HealthProfileContextService healthProfileContextService;
    private final HealthProfileRepository healthProfileRepository;
    private final CurrentNutritionProfileMapper mapper;

    @Override
    public CurrentNutritionProfileResponse saveOrUpdate(
            CurrentNutritionProfileRequest request
    ) {
        /*
         * The profile is resolved from the JWT, never from request input,
         * so a user can only ever change their own intake.
         */
        HealthProfile healthProfile =
                healthProfileContextService.getCurrentUserHealthProfile();

        /*
         * Field omitted -> leave the stored value alone.
         * Field present with a number -> store it.
         * Field present as null -> clear it (intake unknown).
         */
        if (request.suppliesCurrentProteinGrams()) {

            healthProfile.setCurrentNutritionProfile(
                    request.getCurrentProteinGrams() == null
                            ? null
                            : mapper.toEmbeddable(request)
            );

            healthProfile = healthProfileRepository.save(healthProfile);
        }

        return mapper.toResponse(healthProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentNutritionProfileResponse getMyProfile() {

        return mapper.toResponse(
                healthProfileContextService.getCurrentUserHealthProfile()
        );
    }
}
