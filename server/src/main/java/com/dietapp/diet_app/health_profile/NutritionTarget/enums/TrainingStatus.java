package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Represents the user's resistance-training experience level.
 *
 * <p>Used by {@code ProteinCalculator} to contextualise the protein
 * target, and by {@code ProteinAdaptationService} to adjust the
 * starting target and ramp-up cadence.</p>
 */
public enum TrainingStatus {

    /** Less than 1 year of consistent training. */
    BEGINNER,

    /** 1–3 years of consistent training. */
    INTERMEDIATE,

    /** 3+ years of consistent training. */
    ADVANCED
}
