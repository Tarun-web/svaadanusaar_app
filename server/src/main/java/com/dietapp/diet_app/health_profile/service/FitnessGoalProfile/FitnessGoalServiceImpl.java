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
import com.dietapp.diet_app.health_profile.service.ProfileCompletion.ProfileCompletionService;
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

    private final ProfileCompletionService profileCompletionService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public FitnessGoalResponse saveOrUpdate(FitnessGoalRequest request) {

        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        FitnessGoalProfile fitnessGoal =
                fitnessGoalRepository
                        .findByHealthProfileId(
                                healthProfile.getId()
                        )
                        .orElse(null);

        /*
         * CREATE
         */
        if (fitnessGoal == null) {

            fitnessGoal =
                    fitnessGoalMapper.toEntity(request);

            fitnessGoal.setHealthProfile(
                    healthProfile
            );
        }

        /*
         * UPDATE
         */
        else {

            fitnessGoalMapper.updateEntity(
                    request,
                    fitnessGoal
            );
        }

        fitnessGoal =
                fitnessGoalRepository.save(
                        fitnessGoal
                );

        /*
         * Recalculate overall HealthProfile completion.
         */
        profileCompletionService.refreshProfileCompletion(
                healthProfile.getId()
        );

        return fitnessGoalMapper.toResponse(
                fitnessGoal
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FitnessGoalResponse> getMyProfile() {

        // check if exist else return exception
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        return fitnessGoalRepository
                .findByHealthProfileId(
                        healthProfile.getId()
                )
                .map(fitnessGoalMapper::toResponse);
    }

}
