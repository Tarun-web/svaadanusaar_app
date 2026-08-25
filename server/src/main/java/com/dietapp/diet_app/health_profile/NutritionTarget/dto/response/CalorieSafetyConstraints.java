package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CalorieSafetyConstraints(
        BigDecimal minimumCalories,

        BigDecimal maximumCalories,

        BigDecimal maximumDailyDeficit,

        BigDecimal maximumDailySurplus,

        BigDecimal maximumWeeklyWeightLoss,

        BigDecimal maximumWeeklyWeightGain,

        boolean requiresClinicalReview,

        List<CalorieWarning> warnings
) {
}
