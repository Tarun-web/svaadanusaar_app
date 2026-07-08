package com.dietapp.diet_app.HealthProfile.controller;

import com.dietapp.diet_app.HealthProfile.dto.request.HealthProfileRequest;
import com.dietapp.diet_app.HealthProfile.dto.response.HealthProfileResponse;
import com.dietapp.diet_app.HealthProfile.service.HealthProfileService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@AllArgsConstructor
public class HealthProfileController {

    @Autowired
    private final HealthProfileService healthProfileService;

    // Upsert health profile
    @PostMapping("/health")
    public HealthProfileResponse upsert(@RequestBody HealthProfileRequest req, Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        return healthProfileService.upsert(userId, req);
    }

    // Get health profile
    @GetMapping("/health")
    public HealthProfileResponse getByUserId(Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        return healthProfileService.getByUserId(userId);
    }
}
