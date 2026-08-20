package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.enums.Occupation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NeatCalculator {

    /**
     * Estimates daily non-exercise activity expenditure
     * as a percentage of BMR.
     *
     * This is intentionally conservative because we do not
     * currently collect daily step count.
     */
    public BigDecimal calculate(
            BigDecimal bmr,
            Occupation occupation
    ) {

        if (bmr == null || bmr.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        /*
         * If occupation is unknown, use a conservative
         * baseline rather than failing.
         */
        double percentage =
                occupation == null
                        ? 0.15
                        : switch (occupation) {

                    case OFFICE ->
                            0.15;

                    case WFH ->
                            0.12;

                    case HOSTEL_STUDENT,
                         COLLEGE_STUDENT ->
                            0.20;

                    case HOME_MAKER ->
                            0.30;

                    case RETIRED ->
                            0.15;

                    case SHIFT_WORKER ->
                            0.20;

                    case MANUAL_LABOUR ->
                            0.45;
                };

        return bmr
                .multiply(BigDecimal.valueOf(percentage))
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }
}
