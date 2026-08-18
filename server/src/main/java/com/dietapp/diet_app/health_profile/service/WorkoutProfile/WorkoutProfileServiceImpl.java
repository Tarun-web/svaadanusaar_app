package com.dietapp.diet_app.health_profile.service.WorkoutProfile;

import com.dietapp.diet_app.common.exception.EntityNotFoundException;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.health_profile.dto.request.WorkoutProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.WorkoutProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.WorkoutProfile;
import com.dietapp.diet_app.health_profile.mapper.WorkoutProfileMapper;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.repository.WorkoutProfileRepository;
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
public class WorkoutProfileServiceImpl
        implements WorkoutProfileService {

    private final WorkoutProfileRepository workoutProfileRepository;
    private final ProfileCompletionService  profileCompletionService;
    private final WorkoutProfileMapper workoutProfileMapper;
    private final HealthProfileContextService  healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public WorkoutProfileResponse saveOrUpdate(
            WorkoutProfileRequest request
    ) {

        HealthProfile healthProfile = healthProfileContextService.getCurrentUserHealthProfile();

        // find existing workout profile
        WorkoutProfile profile =
                workoutProfileRepository
                        .findByHealthProfileId(
                                healthProfile.getId())
                        .orElse(null);

        // Create
        if (profile == null) {

            profile =
                    workoutProfileMapper
                            .toEntity(request);

            profile.setHealthProfile(
                    healthProfile
            );
        }
        /*
         * UPDATE
         */
        else {

            workoutProfileMapper.updateEntity(
                    request,
                    profile
            );
        }

        // save profile
        profile = workoutProfileRepository.save(profile);

        /*
         * Recalculate Health Profile completion.
         */
        profileCompletionService
                .refreshProfileCompletion(
                        healthProfile.getId()
                );


        return workoutProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkoutProfileResponse> getMyProfile(
    ) {
        // check if exist else return exception
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        return workoutProfileRepository.findByHealthProfileId(healthProfile.getId())
                .map(workoutProfileMapper::toResponse);
    }
}
