package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CalorieSafetyConstraints(BigDecimal minimumCalories,

                                       BigDecimal maximumCalories,

                                       BigDecimal maximumDailyDeficit,

                                       BigDecimal maximumDailySurplus,

                                       BigDecimal maximumWeeklyWeightLossKg,

                                       BigDecimal maximumWeeklyWeightGainKg,

                                       boolean requiresClinicalReview,

                                       List<String> warnings) {
}
