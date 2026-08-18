package com.dietapp.diet_app.health_profile.service.CookingProfile;

import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.CookingProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CookingProfileResponse;
import com.dietapp.diet_app.health_profile.entity.CookingProfile;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.CookingProfileMapper;
import com.dietapp.diet_app.health_profile.repository.CookingProfileRepository;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.AbstractHealthProfileSectionService;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CookingProfileServiceImpl implements CookingProfileService {

    private final CookingProfileRepository repository;
    private final CookingProfileMapper mapper;
    private final HealthProfileContextService healthProfileContextService;
    private final ProfileCompletionService  profileCompletionService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;


    @Override
    public CookingProfileResponse saveOrUpdate(CookingProfileRequest request) {

        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();

        CookingProfile cookingProfile = repository
                        .findByHealthProfileId(healthProfile.getId())
                        .orElse(null);

        if (cookingProfile != null) {

            mapper.updateEntity(
                    request,
                    cookingProfile
            );

        } else {

            cookingProfile = mapper.toEntity(request);
            cookingProfile.setHealthProfile(healthProfile);

        }

        cookingProfile = repository.save(cookingProfile);

        // refresh profile completion
        profileCompletionService.refreshProfileCompletion(
                healthProfile.getId()
        );

        return mapper.toResponse(cookingProfile);
    }

    @Override
    public Optional<CookingProfileResponse> getMyProfile() {
        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();
        return repository.findByHealthProfileId(healthProfile.getId())
                .map(mapper::toResponse);
    }
}



