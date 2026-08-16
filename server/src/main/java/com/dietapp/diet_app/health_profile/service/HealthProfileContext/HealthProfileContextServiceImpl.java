package com.dietapp.diet_app.health_profile.service.HealthProfileContext;

import com.dietapp.diet_app.common.exception.ResourceNotFoundException;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthProfileContextServiceImpl
        implements HealthProfileContextService {

    private final AuthenticationFacade authenticationFacade;
    private final HealthProfileRepository healthProfileRepository;

    @Override
    public HealthProfile getCurrentUserHealthProfile() {

        UUID userId =
                authenticationFacade.getCurrentUserId();

        return healthProfileRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Health Profile not found. " +
                                        "Please create your Health Profile first."
                        ));
    }
}