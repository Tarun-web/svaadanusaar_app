package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.TrainingStatus;
import com.dietapp.diet_app.health_profile.enums.Goal;

import java.math.BigDecimal;

public record ProteinCalculationInput(
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        Goal goal,
        TrainingStatus trainingStatus,
        BigDecimal currentProteinGrams
) {
}
