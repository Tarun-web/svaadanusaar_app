package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Machine-readable codes for protein-related warnings and safety messages.
 *
 * <p>Used inside {@code ProteinTargetResult} and {@code ProteinAdaptationResult}
 * so that the frontend can reliably react to specific conditions without
 * parsing human-readable strings.</p>
 */
public enum ProteinWarningCode {

    /** A relevant medical condition requires clinical review of the protein target. */
    CLINICAL_REVIEW_REQUIRED,

    /** Kidney disease detected — high-protein targets carry specific risks. */
    KIDNEY_DISEASE_DETECTED,

    /** Liver disease detected — protein intake may require clinical management. */
    LIVER_DISEASE_DETECTED,

    /** Celiac disease detected — dietary source selection is the primary concern, not quantity. */
    CELIAC_DISEASE_DETECTED,

    /** Hyperthyroidism detected — existing safety engine handles the evaluation. */
    HYPERTHYROID_DETECTED,

    /** GI tolerance is poor — protein ramp-up has been paused. */
    TOLERANCE_HOLD,

    /** GI symptoms are significant/persistent — medical review is advised. */
    SIGNIFICANT_GI_SYMPTOMS,

    /** The calculated target was clamped to a safe maximum. */
    TARGET_CLAMPED_MAX,

    /** Calculated target was below minimum safe threshold and has been adjusted. */
    TARGET_CLAMPED_MIN,

    /** Body-fat percentage was outside valid range and was ignored. */
    BODY_FAT_INVALID_IGNORED,

    /** Long-term target already reached — no further ramp-up needed. */
    TARGET_ALREADY_REACHED,

    /** Current protein intake is unknown — adaptation schedule deferred. */
    CURRENT_INTAKE_UNKNOWN
}
