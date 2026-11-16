package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Describes what a scheduled protein-adaptation tick did for one profile.
 *
 * <p>Returned inside {@code ScheduledEvaluationResult} by
 * {@code ProteinAdaptationService.evaluateScheduled}.</p>
 */
public enum ScheduledEvaluationOutcome {

    /** The profile was already evaluated on or after today; nothing changed. */
    ALREADY_EVALUATED_TODAY,

    /** Stage is NOT_STARTED or TARGET_REACHED; there is no ramp to count. */
    SKIPPED_NOT_RAMPING,

    /** A clinical-review condition is present; nothing changed. */
    SKIPPED_CLINICAL_REVIEW,

    /** GI tolerance holds progression; no day was counted. */
    HELD_TOLERANCE,

    /** A day was counted but the minimum period has not yet elapsed. */
    DAY_COUNTED,

    /** A day was counted and the protein target was increased. */
    TARGET_ADVANCED
}
