package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.MedicalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.MedicalProfileResponse;
import com.dietapp.diet_app.health_profile.service.MedicalProfile.MedicalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/medical")
@RequiredArgsConstructor
public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    /**
     * Create or update Medical Profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<MedicalProfileResponse> saveOrUpdate(
            @Valid @RequestBody MedicalProfileRequest request
    ) {

        return ResponseEntity.ok(
                medicalProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get Medical Profile
     * for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<MedicalProfileResponse>> getMyProfile() {

        return ResponseEntity.ok(
                medicalProfileService.getMyProfile()
        );
    }
}