package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalculationMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionTargetResponse {

    private UUID id;

    private BigDecimal weightUsedKg;

    private BigDecimal bmr;

    private BigDecimal tdee;

    private BigDecimal targetCalories;

    private BigDecimal proteinGrams;

    private BigDecimal carbohydrateGrams;

    private BigDecimal fatGrams;

    private BigDecimal fiberGrams;

    private BigDecimal waterMl;

    private BigDecimal calorieAdjustment;

    private ActivityLevel activityLevel;

    private CalculationMethod calculationMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}