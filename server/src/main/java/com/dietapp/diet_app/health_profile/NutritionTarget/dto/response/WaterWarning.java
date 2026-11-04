package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.WaterWarningCode;

/**
 * A single water-calculation warning or informational message.
 *
 * <p>Follows the same convention as {@link FatWarning},
 * {@link CarbohydrateWarning}, and {@link ProteinWarning}.</p>
 */
public record WaterWarning(
        WaterWarningCode code,
        String message
) {
}
