package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FibreCalculator {

    private static final BigDecimal FIBRE_PER_1000_KCAL =
            new BigDecimal("14");

    public BigDecimal calculate(BigDecimal targetCalories) {

        if (targetCalories == null || targetCalories.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target calories must be greater than zero");
        }

        return targetCalories
                .multiply(FIBRE_PER_1000_KCAL)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
    }
}
