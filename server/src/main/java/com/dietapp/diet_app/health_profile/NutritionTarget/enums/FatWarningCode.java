package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Machine-readable codes for fat-calculation warnings and safety messages.
 *
 * <p>Used inside {@code FatCalculationResult} so that the frontend and
 * macro orchestrator can reliably react to specific conditions without
 * parsing human-readable strings.</p>
 *
 * <p>Follows the same convention as {@link ProteinWarningCode} and
 * {@link CalorieWarningCode}.</p>
 */
public enum FatWarningCode {

    /** A medical condition requires clinical review of the fat target. */
    CLINICAL_REVIEW_REQUIRED,

    /** High cholesterol detected — saturated fat sources should be evaluated at the diet layer. */
    HIGH_CHOLESTEROL_DETECTED,

    /** Fatty liver detected — overall fat intake may warrant clinical guidance. */
    FATTY_LIVER_DETECTED,

    /** Gout detected — dietary fat pattern may interact with uric acid levels. */
    GOUT_DETECTED,

    /** The fat target was clamped to the configured maximum. */
    TARGET_CLAMPED_MAX,

    /** The fat target was clamped to the configured minimum floor. */
    TARGET_CLAMPED_MIN,

    /**
     * After accounting for protein and fat, remaining calories for carbohydrates
     * are negative or zero — the macro combination is infeasible at these settings.
     */
    MACRO_COMBINATION_INFEASIBLE,

    /**
     * After accounting for protein and fat, remaining calories for carbohydrates
     * are very low (below a practical minimum threshold).
     */
    LOW_CARBOHYDRATE_HEADROOM
}
