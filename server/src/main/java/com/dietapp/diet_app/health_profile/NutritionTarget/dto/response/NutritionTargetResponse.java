package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record NutritionTargetResponse(

        BigDecimal tdee,

        BigDecimal proposedCalories,

        BigDecimal finalCalories,

        boolean safetyAdjusted,

        CalorieSafetyStatus status,

        boolean requiresClinicalReview,

        List<CalorieWarning> warnings

) {
}
