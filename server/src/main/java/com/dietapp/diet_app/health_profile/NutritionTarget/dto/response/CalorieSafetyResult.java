package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;

import java.math.BigDecimal;
import java.util.List;

public record CalorieSafetyResult(
        BigDecimal originalCalories,
        BigDecimal finalCalories,
        CalorieSafetyStatus status,
        boolean safetyAdjusted,
        boolean requiresClinicalReview,
        List<CalorieWarning> warnings
) {
}
