package com.dietapp.diet_app.health_profile.NutritionTarget.scheduler;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;

import java.util.Map;

/**
 * Aggregate, non-identifying outcome of one scheduler run.
 *
 * @param date      the calendar date that was processed
 * @param examined  profiles returned by the eligibility query
 * @param outcomes  count of each {@link ScheduledEvaluationOutcome}
 * @param conflicts profiles skipped because another worker updated them first
 * @param failures  profiles that failed for any other reason
 */
public record ProteinAdaptationRunSummary(
        java.time.LocalDate date,
        int examined,
        Map<ScheduledEvaluationOutcome, Integer> outcomes,
        int conflicts,
        int failures
) {
}
