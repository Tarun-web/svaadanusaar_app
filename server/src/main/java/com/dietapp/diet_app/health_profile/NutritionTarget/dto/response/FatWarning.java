package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.FatWarningCode;

/**
 * A single fat-calculation warning or safety message.
 *
 * <p>Follows the same convention as {@link ProteinWarning} and
 * {@link CalorieWarning}.</p>
 */
public record FatWarning(
        FatWarningCode code,
        String message
) {
}
