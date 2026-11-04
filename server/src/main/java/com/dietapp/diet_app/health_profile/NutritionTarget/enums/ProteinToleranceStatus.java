package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Records the user's GI / digestive tolerance at the current protein target.
 *
 * <p>Managed by {@code ProteinAdaptationService} based on user-reported
 * or diet-layer-reported symptom data.</p>
 *
 * <p>This is NOT a medical diagnosis — it is a soft signal used only to
 * pace the protein ramp-up schedule.</p>
 */
public enum ProteinToleranceStatus {

    /**
     * No symptom data has been collected yet.
     *
     * <p>Progression is allowed after the minimum adaptation period.</p>
     */
    UNKNOWN,

    /**
     * No GI symptoms reported. Progression is permitted when the
     * minimum adaptation period has elapsed.
     */
    GOOD,

    /**
     * Mild symptoms reported (e.g. occasional bloating or gas).
     *
     * <p>Hold the current target — do not increase.</p>
     */
    MILD_ISSUES,

    /**
     * Significant or persistent symptoms reported.
     *
     * <p>Hold the current target and generate a safety/medical
     * review warning. Do not automatically increase or decrease.</p>
     */
    SIGNIFICANT_ISSUES
}
