package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieWarningCode;

public record CalorieWarning(
        CalorieWarningCode code,
        String message
) {
}

//This means the frontend can reliably react to:
//
//        {
//        "code": "TARGET_RATE_TOO_AGGRESSIVE",
//        "message": "Your requested rate of weight loss is too aggressive for automated calorie planning."
//        }
