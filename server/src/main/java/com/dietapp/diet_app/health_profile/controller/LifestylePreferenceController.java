package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.LifestylePreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.LifestylePreferenceResponse;
import com.dietapp.diet_app.health_profile.service.LifestylePreferenceProfile.LifestylePreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/lifestyle")
@RequiredArgsConstructor
public class LifestylePreferenceController {

    private final LifestylePreferenceService lifestylePreferenceService;

    /**
     * Create or update Lifestyle Preference Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<LifestylePreferenceResponse> saveOrUpdate(
            @Valid @RequestBody LifestylePreferenceRequest request
    ) {

        return ResponseEntity.ok(
                lifestylePreferenceService.saveOrUpdate(request)
        );
    }

    /**
     * Get Lifestyle Preference Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<LifestylePreferenceResponse>> getMyProfile() {

        return ResponseEntity.ok(
                lifestylePreferenceService.getMyProfile()
        );
    }
}