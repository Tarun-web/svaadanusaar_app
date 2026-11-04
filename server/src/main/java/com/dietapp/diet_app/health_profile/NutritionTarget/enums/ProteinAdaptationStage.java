package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Represents the current phase in the user's protein adaptation journey.
 *
 * <p>Managed exclusively by {@code ProteinAdaptationService} — the user
 * never sets this manually.</p>
 */
public enum ProteinAdaptationStage {

    /**
     * Adaptation has not started yet.
     *
     * <p>This is the initial state when current protein intake is unknown
     * or when no adaptation profile has been initialised.</p>
     */
    NOT_STARTED,

    /**
     * The user has been given a starting target below the long-term target.
     *
     * <p>Progression will begin once tolerance is confirmed as GOOD
     * and the minimum adaptation period has elapsed.</p>
     */
    STARTING,

    /**
     * The user is actively increasing protein in measured increments.
     */
    ADAPTING,

    /**
     * The user's current target equals the long-term calculated target.
     *
     * <p>No further ramp-up is required unless the profile changes.</p>
     */
    TARGET_REACHED
}
