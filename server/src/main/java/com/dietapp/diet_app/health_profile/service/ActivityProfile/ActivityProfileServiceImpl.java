package com.dietapp.diet_app.health_profile.service.ActivityProfile;

import com.dietapp.diet_app.common.exception.BusinessException;
import com.dietapp.diet_app.common.exception.ResourceNotFoundException;
import com.dietapp.diet_app.health_profile.dto.request.ActivityRequest;
import com.dietapp.diet_app.health_profile.dto.response.ActivityResponse;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.WorkoutProfile;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.mapper.ActivityMapper;
import com.dietapp.diet_app.health_profile.repository.ActivityProfileRepository;
import com.dietapp.diet_app.health_profile.repository.WorkoutProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityProfileServiceImpl
        implements ActivityProfileService {

    private final ActivityProfileRepository activityRepository;

    private final WorkoutProfileRepository workoutRepository;

    private final ActivityMapper activityMapper;

    private final HealthProfileContextService
            healthProfileContextService;


    /**
     * Add a new activity for the authenticated user.
     */
    @Override
    public ActivityResponse addActivity(
            ActivityRequest request
    ) {


        validateActivityRequest(request);
        /*
         * Get Health Profile of authenticated user
         * from JWT.
         */
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        /*
         * Get Workout Profile belonging to
         * this Health Profile.
         */
        WorkoutProfile workoutProfile =
                workoutRepository
                        .findByHealthProfileId(
                                healthProfile.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workout Profile not found. " +
                                                "Please complete your workout profile first."
                                )
                        );

        /*
         * Convert request DTO -> entity.
         */
        ActivityProfile activity =
                activityMapper.toEntity(request);

        /*
         * Associate activity with the user's
         * Workout Profile.
         */
        activity.setWorkoutProfile(
                workoutProfile
        );

        /*
         * Save activity.
         */
        activity = activityRepository.save(activity);

        return activityMapper.toResponse(activity);
    }


    /**
     * Update an activity belonging to
     * the authenticated user.
     */
    @Override
    public ActivityResponse updateActivity(
            UUID activityId,
            ActivityRequest request
    ) {

        validateActivityRequest(request);
        /*
         * Get authenticated user's Health Profile.
         */
        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        /*
         * Find the activity.
         */
        ActivityProfile activity =
                activityRepository
                        .findById(activityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Activity not found."
                                )
                        );

        /*
         * SECURITY CHECK
         *
         * Make sure this activity actually belongs
         * to the currently authenticated user.
         */
        if (!activity.getWorkoutProfile()
                .getHealthProfile()
                .getId()
                .equals(healthProfile.getId())) {

            throw new ResourceNotFoundException(
                    "Activity not found."
            );
        }

        /*
         * Update existing entity.
         */
        activityMapper.updateEntity(
                request,
                activity
        );

        activity =
                activityRepository.save(activity);

        return activityMapper.toResponse(activity);
    }


    /**
     * Delete an activity belonging to
     * the authenticated user.
     */
    @Override
    public void deleteActivity(
            UUID activityId
    ) {

        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        ActivityProfile activity =
                activityRepository
                        .findById(activityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Activity not found."
                                )
                        );

        /*
         * SECURITY CHECK
         */
        if (!activity.getWorkoutProfile()
                .getHealthProfile()
                .getId()
                .equals(healthProfile.getId())) {

            throw new ResourceNotFoundException(
                    "Activity not found."
            );
        }

        activityRepository.delete(activity);
    }


    /**
     * Get all activities belonging to
     * the authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getMyActivities() {

        HealthProfile healthProfile =
                healthProfileContextService
                        .getCurrentUserHealthProfile();

        WorkoutProfile workoutProfile =
                workoutRepository
                        .findByHealthProfileId(
                                healthProfile.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workout Profile not found."
                                )
                        );

        return activityRepository
                .findByWorkoutProfileId(
                        workoutProfile.getId()
                )
                .stream()
                .map(activityMapper::toResponse)
                .toList();
    }

    // helper
    private void validateActivityRequest(ActivityRequest request) {

        if (request.getActivityType() == ActivityType.SPORTS
                && request.getSportType() == null) {

            throw new BusinessException(
                    "Sport type is required when activity type is SPORTS."
            );
        }

        if (request.getActivityType() != ActivityType.SPORTS
                && request.getSportType() != null) {

            throw new BusinessException(
                    "Sport type should only be provided for SPORTS activity."
            );
        }
    }
}