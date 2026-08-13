package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.FitnessGoalRequest;
import com.dietapp.diet_app.health_profile.dto.response.FitnessGoalResponse;
import com.dietapp.diet_app.health_profile.service.FitnessGoalProfile.FitnessGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/fitness-goal")
@RequiredArgsConstructor
public class FitnessGoalController {

    private final FitnessGoalService fitnessGoalService;

    /**
     * Create or update Fitness Goal Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<FitnessGoalResponse> saveOrUpdate(
            @Valid @RequestBody FitnessGoalRequest request
    ) {

        return ResponseEntity.ok(
                fitnessGoalService.saveOrUpdate(request)
        );
    }

    /**
     * Get Fitness Goal Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<FitnessGoalResponse>> getMyProfile() {

        return ResponseEntity.ok(
                fitnessGoalService.getMyProfile()
        );
    }
}