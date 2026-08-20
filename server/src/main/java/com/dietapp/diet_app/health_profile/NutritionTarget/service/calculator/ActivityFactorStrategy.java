package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ActivityFactorStrategy {
    public BigDecimal getFactor(
            ActivityLevel activityLevel
    ) {

        if (activityLevel == null) {
            return BigDecimal.valueOf(1.20);
        }

        return switch (activityLevel) {

            case SEDENTARY ->
                    BigDecimal.valueOf(1.20);

            case LIGHTLY_ACTIVE ->
                    BigDecimal.valueOf(1.375);

            case MODERATELY_ACTIVE ->
                    BigDecimal.valueOf(1.55);

            case VERY_ACTIVE ->
                    BigDecimal.valueOf(1.725);

            case EXTREMELY_ACTIVE ->
                    BigDecimal.valueOf(1.90);
        };
    }

}
