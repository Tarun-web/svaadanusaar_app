package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.enums.Goal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalorieTargetResult(

        Goal goal,

        BigDecimal currentWeightKg,

        BigDecimal targetWeightKg,

        LocalDate targetDate,

        BigDecimal tdeeCalories,

        BigDecimal requestedWeeklyWeightChangeKg,

        BigDecimal effectiveWeeklyWeightChangeKg,

        BigDecimal dailyCalorieAdjustment,

        BigDecimal targetCalories,

        BigDecimal minimumAllowedCalories,

        BigDecimal maximumAllowedCalories,

        boolean targetTimelineAchievable

) {
}
