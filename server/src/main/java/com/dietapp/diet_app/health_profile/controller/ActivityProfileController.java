package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.ActivityRequest;
import com.dietapp.diet_app.health_profile.dto.response.ActivityResponse;
import com.dietapp.diet_app.health_profile.service.ActivityProfile.ActivityProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-profile/workout/activities")
@RequiredArgsConstructor
public class ActivityProfileController {

    private final ActivityProfileService activityProfileService;

    /**
     * Add activity for authenticated user.
     */
    @PostMapping
    public ResponseEntity<ActivityResponse> addActivity(
            @Valid @RequestBody ActivityRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        activityProfileService.addActivity(
                                request
                        )
                );
    }

    /**
     * Get all activities for authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getMyActivities() {

        return ResponseEntity.ok(
                activityProfileService.getMyActivities()
        );
    }

    /**
     * Update an activity.
     */
    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityRequest request
    ) {

        return ResponseEntity.ok(
                activityProfileService.updateActivity(
                        activityId,
                        request
                )
        );
    }

    /**
     * Delete an activity.
     */
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(
            @PathVariable UUID activityId
    ) {

        activityProfileService.deleteActivity(
                activityId
        );
    }
}