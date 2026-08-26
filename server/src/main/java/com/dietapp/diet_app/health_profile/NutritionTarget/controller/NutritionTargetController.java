package com.dietapp.diet_app.health_profile.NutritionTarget.controller;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.NutritionTargetResponse;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.NutritionTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/nutrition-targets")
@RequiredArgsConstructor
public class NutritionTargetController {
    private final NutritionTargetService nutritionTargetService;

    @GetMapping
    public ResponseEntity<NutritionTargetResponse> getMyNutritionTarget() {

        return ResponseEntity.ok(
                nutritionTargetService.calculateForCurrentUser()
        );
    }
}
