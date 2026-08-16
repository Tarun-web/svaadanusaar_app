package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.response.HealthProfileDetailsResponse;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileResponse;
import com.dietapp.diet_app.health_profile.service.HealthProfile.HealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/health-profile")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    /**
     * Create Health Profile for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<HealthProfileResponse> createHealthProfile() {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthProfileService.createHealthProfile());

    }

    /**
     * Get complete Health Profile of the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<HealthProfileDetailsResponse> getMyHealthProfile() {

        return ResponseEntity.ok(
                healthProfileService.getMyHealthProfile()
        );

    }

    /**
     * Get Health Profile summary of the authenticated user.
     */
    @GetMapping("/me/summary")
    public ResponseEntity<HealthProfileResponse> getMyHealthProfileSummary() {

        return ResponseEntity.ok(
                healthProfileService.getMyHealthProfileSummary()
        );

    }

    /**
     * Delete Health Profile of the authenticated user.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyHealthProfile() {

        healthProfileService.deleteMyHealthProfile();

    }

}