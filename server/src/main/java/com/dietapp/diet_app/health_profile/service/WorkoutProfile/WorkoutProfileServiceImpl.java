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
    private final HealthProfileRepository healthProfileRepository;
    private final WorkoutProfileMapper workoutProfileMapper;
    private final HealthProfileContextService  healthProfileContextService;

    // Extract authenticated user from JWT token
    private final AuthenticationFacade authenticationFacade;

    @Override
    public WorkoutProfileResponse saveOrUpdate(
            WorkoutProfileRequest request
    ) {

        // Extract current user from JWT token
        UUID userId = authenticationFacade.getCurrentUserId();

        HealthProfile healthProfile = healthProfileRepository
                .findById(request.getHealthProfileId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Health Profile not found."));

        // Verify ownership: health profile must belong to authenticated user
        if (!healthProfile.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Health Profile not found.");
        }

        WorkoutProfile workoutProfile =
                workoutProfileRepository
                        .findByHealthProfileId(request.getHealthProfileId())
                        .orElseGet(() -> {

                            WorkoutProfile entity =
                                    workoutProfileMapper.toEntity(request);

                            entity.setHealthProfile(healthProfile);

                            return entity;
                        });

        if (workoutProfile.getId() != null) {
            workoutProfileMapper.updateEntity(request, workoutProfile);
        }

        workoutProfile = workoutProfileRepository.save(workoutProfile);

        return workoutProfileMapper.toResponse(workoutProfile);
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
