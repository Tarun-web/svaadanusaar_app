package com.dietapp.diet_app.health_profile.service.LifestylePreferenceProfile;

import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.LifestylePreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.LifestylePreferenceResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.LifestylePreferenceProfile;
import com.dietapp.diet_app.health_profile.mapper.LifestylePreferenceMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.LifestylePreferenceProfileRepository;
import com.dietapp.diet_app.health_profile.service.AbstractHealthProfileSectionService;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import com.dietapp.diet_app.health_profile.service.LifestylePreferenceProfile.LifestylePreferenceService;
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
public class LifestylePreferenceServiceImpl implements LifestylePreferenceService {

    private final LifestylePreferenceProfileRepository repository;
    private final LifestylePreferenceMapper mapper;
    private final ProfileCompletionService  profileCompletionService;
    private final HealthProfileContextService healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;


    @Override
    public LifestylePreferenceResponse saveOrUpdate(LifestylePreferenceRequest request) {
        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();

        // Check whether a LifestylePreferenceProfile already exists for this HealthProfile.
        LifestylePreferenceProfile lifestylePreferenceProfile =
                repository.findByHealthProfileId(healthProfile.getId())
                .orElse(null);

        if(lifestylePreferenceProfile == null) {
            lifestylePreferenceProfile = mapper.toEntity(request);
            lifestylePreferenceProfile.setHealthProfile(healthProfile);
        }
        else{
            mapper.updateEntity(request, lifestylePreferenceProfile);
        }

        lifestylePreferenceProfile = repository.save(lifestylePreferenceProfile);

        // refresh profile completion
        profileCompletionService.refreshProfileCompletion(
                healthProfile.getId()
        );

        return mapper.toResponse(lifestylePreferenceProfile);

    }

    @Override
    public Optional<LifestylePreferenceResponse> getMyProfile() {
        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();
        return repository.findByHealthProfileId(healthProfile.getId())
                .map(mapper::toResponse);
    }
}