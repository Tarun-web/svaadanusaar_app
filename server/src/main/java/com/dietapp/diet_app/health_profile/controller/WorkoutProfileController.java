package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.WorkoutProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.WorkoutProfileResponse;
import com.dietapp.diet_app.health_profile.service.WorkoutProfile.WorkoutProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/workout")
@RequiredArgsConstructor
public class WorkoutProfileController {

    private final WorkoutProfileService workoutProfileService;

    /**
     * Create or update Workout Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<WorkoutProfileResponse> saveOrUpdate(
            @Valid @RequestBody WorkoutProfileRequest request
    ) {

        return ResponseEntity.ok(
                workoutProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get Workout Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<WorkoutProfileResponse>> getMyProfile() {

        return ResponseEntity.ok(
                workoutProfileService.getMyProfile()
        );
    }
}