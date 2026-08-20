package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TefCalculator {

    /**
     * Estimates thermic effect of food as approximately
     * 10% of energy intake.
     *
     * For the first implementation we use TDEE-before-TEF
     * as the base.
     */
    public BigDecimal calculate(
            BigDecimal energyBeforeTef
    ) {

        if (energyBeforeTef == null
                || energyBeforeTef.signum() <= 0) {

            return BigDecimal.ZERO;
        }

        return energyBeforeTef
                .multiply(BigDecimal.valueOf(0.10))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
