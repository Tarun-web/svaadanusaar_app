package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.SupplementProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.SupplementProfileResponse;
import com.dietapp.diet_app.health_profile.service.SupplementProfile.SupplementProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/supplements")
@RequiredArgsConstructor
public class SupplementProfileController {

    private final SupplementProfileService supplementProfileService;

    /**
     * Create or update Supplement Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<SupplementProfileResponse> saveOrUpdate(
            @Valid @RequestBody SupplementProfileRequest request
    ) {

        return ResponseEntity.ok(
                supplementProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get Supplement Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<SupplementProfileResponse>> getMyProfile() {

        return ResponseEntity.ok(
                supplementProfileService.getMyProfile()
        );
    }
}