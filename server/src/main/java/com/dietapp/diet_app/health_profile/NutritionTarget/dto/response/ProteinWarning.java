package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;

/**
 * A single protein-related warning or safety message.
 *
 * <p>Analogous to {@link CalorieWarning} in the calorie safety pipeline.</p>
 */
public record ProteinWarning(
        ProteinWarningCode code,
        String message
) {
}
