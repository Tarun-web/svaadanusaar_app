package com.dietapp.diet_app.diet.controller;

import com.dietapp.diet_app.diet.dto.DietTargets;
import com.dietapp.diet_app.diet.service.DietService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/diet")
public class DietController {

    @Autowired
    private final DietService dietService;

    // Generate diet plan based on user's health profile
    @PostMapping("/generate")
    public DietTargets generateDietPlan(Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        return dietService.generateDietPlan(userId);
    }

    // Get active diet plan for the user
    @GetMapping("/active")
    public DietTargets getActiveDietPlan(Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        return dietService.getActiveDietPlan(userId);
    }
    // Get current diet details in JSON format for frontend display
    @GetMapping("/current")
    public Map<String, Object> getCurrentDiet(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return dietService.getCurrentDietJson(userId);
    }

}
