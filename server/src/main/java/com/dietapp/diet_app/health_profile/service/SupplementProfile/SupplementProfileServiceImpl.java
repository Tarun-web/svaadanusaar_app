package com.dietapp.diet_app.health_profile.service.SupplementProfile;

import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.SupplementProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.SupplementProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.SupplementProfile;
import com.dietapp.diet_app.health_profile.mapper.SupplementProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.SupplementProfileRepository;
import com.dietapp.diet_app.health_profile.service.AbstractHealthProfileSectionService;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplementProfileServiceImpl implements SupplementProfileService {

    private final SupplementProfileRepository repository;
    private final SupplementProfileMapper mapper;
    private final HealthProfileContextService healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;



    @Override
    public SupplementProfileResponse saveOrUpdate(
            SupplementProfileRequest request
    ) {

        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();

        SupplementProfile supplementProfile = repository.findByHealthProfileId(request.getHealthProfileId())
                .orElseGet(() -> {
                    SupplementProfile profile = mapper.toEntity(request);
                    profile.setHealthProfile(healthProfile);
                    return profile;
                });

        if(supplementProfile.getId() == null) {
            mapper.updateEntity(request, supplementProfile);
        }

        supplementProfile = repository.save(supplementProfile);
        return mapper.toResponse(supplementProfile);

    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplementProfileResponse> getMyProfile(
    ) {
        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();
        return repository.findByHealthProfileId(healthProfile.getId())
                .map(mapper::toResponse);
    }

}
