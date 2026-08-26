package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.NutritionTargetResponse;

public interface NutritionTargetService {
    NutritionTargetResponse calculateForCurrentUser();
}
