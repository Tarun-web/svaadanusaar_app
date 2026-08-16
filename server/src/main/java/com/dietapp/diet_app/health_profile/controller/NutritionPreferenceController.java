package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.NutritionPreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.NutritionPreferenceResponse;
import com.dietapp.diet_app.health_profile.service.NutritionPreferenceProfile.NutritionPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/nutrition-preference")
@RequiredArgsConstructor
public class NutritionPreferenceController {

    private final NutritionPreferenceService nutritionPreferenceService;

    /**
     * Create or update Nutrition Preference Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<NutritionPreferenceResponse> saveOrUpdate(
            @Valid @RequestBody NutritionPreferenceRequest request
    ) {

        return ResponseEntity.ok(
                nutritionPreferenceService.saveOrUpdate(request)
        );
    }

    /**
     * Get Nutrition Preference Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<NutritionPreferenceResponse>> getMyProfile() {

        return ResponseEntity.ok(
                nutritionPreferenceService.getMyProfile()
        );
    }
}