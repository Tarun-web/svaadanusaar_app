package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;

import java.math.BigDecimal;

public record TdeeResult(

        int age,

        BigDecimal bmr,

        ActivityLevel activityLevel,

        BigDecimal activityFactor,

        BigDecimal neat,

        BigDecimal exerciseCalories,

        BigDecimal energyBeforeTef,

        BigDecimal tef,

        BigDecimal tdee
) {
}
