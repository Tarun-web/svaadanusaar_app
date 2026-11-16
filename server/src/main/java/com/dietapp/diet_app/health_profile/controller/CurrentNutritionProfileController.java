package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile.CurrentNutritionProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/health-profile/current-nutrition")
@RequiredArgsConstructor
public class CurrentNutritionProfileController {

    private final CurrentNutritionProfileService currentNutritionProfileService;

    /**
     * Update the authenticated user's self-reported protein intake.
     *
     * <p>Omitted field: unchanged. Explicit {@code null}: cleared.</p>
     */
    @PutMapping
    public ResponseEntity<CurrentNutritionProfileResponse> saveOrUpdate(
            @Valid @RequestBody CurrentNutritionProfileRequest request
    ) {

        return ResponseEntity.ok(
                currentNutritionProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get the authenticated user's self-reported protein intake.
     */
    @GetMapping
    public ResponseEntity<CurrentNutritionProfileResponse> getMyProfile() {

        return ResponseEntity.ok(
                currentNutritionProfileService.getMyProfile()
        );
    }
}
