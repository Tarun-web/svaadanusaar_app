package com.dietapp.diet_app.health_profile.service.FitnessGoalProfile;

import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.FitnessGoalRequest;
import com.dietapp.diet_app.health_profile.dto.response.FitnessGoalResponse;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.FitnessGoalMapper;
import com.dietapp.diet_app.health_profile.repository.FitnessGoalProfileRepository;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FitnessGoalServiceImpl implements FitnessGoalService {

    private final FitnessGoalProfileRepository fitnessGoalRepository;

    private final HealthProfileRepository healthProfileRepository;

    private final FitnessGoalMapper fitnessGoalMapper;

    private final HealthProfileContextService healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public FitnessGoalResponse saveOrUpdate(FitnessGoalRequest request) {

        // Extract current user from JWT token
        UUID userId = authenticationFacade.getCurrentUserId();

        HealthProfile healthProfile = healthProfileRepository
                .findById(request.getHealthProfileId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Health Profile not found."
                        )
                );

        // Verify ownership: health profile must belong to authenticated user
        if (!healthProfile.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException(
                    "Health Profile not found."
            );
        }

        FitnessGoalProfile fitnessGoal =
                fitnessGoalRepository
                        .findByHealthProfileId(request.getHealthProfileId())
                        .orElseGet(() -> {

                            FitnessGoalProfile profile =
                                    fitnessGoalMapper.toEntity(request);

                            profile.setHealthProfile(healthProfile);

                            return profile;

                        });

        if (fitnessGoal.getId() != null) {

            fitnessGoalMapper.updateEntity(
                    request,
                    fitnessGoal
            );

        }

        fitnessGoal = fitnessGoalRepository.save(fitnessGoal);

        return fitnessGoalMapper.toResponse(fitnessGoal);

    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FitnessGoalResponse> getMyProfile() {

        // check if exist else return exception
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        return fitnessGoalRepository.findByHealthProfileId(healthProfile.getUser().getId())
                .map(fitnessGoalMapper::toResponse);
    }

}
