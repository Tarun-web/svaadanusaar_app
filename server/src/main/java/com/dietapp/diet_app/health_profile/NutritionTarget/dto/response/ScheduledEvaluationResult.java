package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;

/**
 * The result of one scheduled protein-adaptation tick for one profile.
 *
 * @param outcome    what the tick did
 * @param adaptation the profile's state after the tick
 */
public record ScheduledEvaluationResult(
        ScheduledEvaluationOutcome outcome,
        ProteinAdaptationResult adaptation
) {
}
