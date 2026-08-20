package com.dietapp.diet_app.health_profile.NutritionTarget.dto.request;

import java.math.BigDecimal;

public record BmrResult(
        int age,
        BigDecimal bmr
) {
}
