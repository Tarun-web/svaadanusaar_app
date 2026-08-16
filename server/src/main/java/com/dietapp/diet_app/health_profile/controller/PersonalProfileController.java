package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.PersonalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.PersonalProfileResponse;
import com.dietapp.diet_app.health_profile.service.PersonalProfile.PersonalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-profile/personal")
@RequiredArgsConstructor
public class PersonalProfileController {

    private final PersonalProfileService personalProfileService;

    /**
     * Create or update personal profile
     * for the authenticated user.
     */
    @PutMapping
    public ResponseEntity<PersonalProfileResponse> saveOrUpdate(
            @Valid @RequestBody PersonalProfileRequest request
    ) {

        return ResponseEntity.ok(
                personalProfileService.saveOrUpdate(request)
        );
    }

    /**
     * Get personal profile of authenticated user.
     */
    @GetMapping
    public ResponseEntity<Optional<PersonalProfileResponse>> getMyProfile() {

        return ResponseEntity.ok(
                personalProfileService.getMyProfile()
        );
    }
}