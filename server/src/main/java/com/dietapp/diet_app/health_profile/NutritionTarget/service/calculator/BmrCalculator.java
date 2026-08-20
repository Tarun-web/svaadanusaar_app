package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.BmrResult;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Gender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Component
public class BmrCalculator {

    /**
     * Calculates Basal Metabolic Rate using
     * the Mifflin-St Jeor equation.
     *
     * Male:
     *
     * BMR = (10 × weight)
     *     + (6.25 × height)
     *     - (5 × age)
     *     + 5
     *
     * Female:
     *
     * BMR = (10 × weight)
     *     + (6.25 × height)
     *     - (5 × age)
     *     - 161
     */
    public BmrResult calculate(
            PersonalProfile personalProfile
    ) {

        if (personalProfile == null) {
            throw new IllegalArgumentException(
                    "Personal profile is required."
            );
        }

        if (personalProfile.getDateOfBirth() == null) {
            throw new IllegalArgumentException(
                    "Date of birth is required to calculate BMR."
            );
        }

        if (personalProfile.getWeightKg() == null) {
            throw new IllegalArgumentException(
                    "Weight is required to calculate BMR."
            );
        }

        if (personalProfile.getHeightCm() == null) {
            throw new IllegalArgumentException(
                    "Height is required to calculate BMR."
            );
        }

        if (personalProfile.getGender() == null) {
            throw new IllegalArgumentException(
                    "Gender is required to calculate BMR."
            );
        }

        int age = calculateAge(
                personalProfile.getDateOfBirth()
        );

        double weight =
                personalProfile
                        .getWeightKg()
                        .doubleValue();

        double height =
                personalProfile
                        .getHeightCm();

        double bmr =
                (10.0 * weight)
                        + (6.25 * height)
                        - (5.0 * age);

        if (personalProfile.getGender()
                == Gender.MALE) {

            bmr += 5.0;

        } else {

            bmr -= 161.0;
        }

        BigDecimal roundedBmr =
                BigDecimal
                        .valueOf(bmr)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new BmrResult(
                age,
                roundedBmr
        );
    }


    /**
     * Calculates completed age in years.
     */
    private int calculateAge(
            LocalDate dateOfBirth
    ) {

        LocalDate today =
                LocalDate.now();

        if (dateOfBirth.isAfter(today)) {
            throw new IllegalArgumentException(
                    "Date of birth cannot be in the future."
            );
        }

        return Period
                .between(
                        dateOfBirth,
                        today
                )
                .getYears();
    }
}
