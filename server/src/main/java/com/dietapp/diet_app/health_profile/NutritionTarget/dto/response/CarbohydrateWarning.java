package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CarbohydrateWarningCode;

/**
 * A single carbohydrate-calculation warning or safety message.
 *
 * <p>Follows the same convention as {@link FatWarning},
 * {@link ProteinWarning}, and {@link CalorieWarning}.</p>
 */
public record CarbohydrateWarning(
        CarbohydrateWarningCode code,
        String message
) {
}
