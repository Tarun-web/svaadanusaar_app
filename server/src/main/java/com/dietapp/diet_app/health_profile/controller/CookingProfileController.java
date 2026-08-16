package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.CookingProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CookingProfileResponse;
import com.dietapp.diet_app.health_profile.service.CookingProfile.CookingProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/cooking")
@RequiredArgsConstructor
public class CookingProfileController {

    private final CookingProfileService cookingProfileService;

    /**
     * Create or update Cooking Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<CookingProfileResponse> saveOrUpdate(
            @Valid @RequestBody CookingProfileRequest request
    ) {

        return ResponseEntity.ok(
                cookingProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get Cooking Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<CookingProfileResponse>> getMyProfile() {

        return ResponseEntity.ok(
                cookingProfileService.getMyProfile()
        );
    }
}