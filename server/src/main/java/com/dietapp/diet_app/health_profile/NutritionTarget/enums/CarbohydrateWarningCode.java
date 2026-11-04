package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Machine-readable codes for carbohydrate-calculation warnings.
 *
 * <p>Kept intentionally small. Upstream macro-infeasibility and
 * low-headroom conditions are already captured by
 * {@link FatWarningCode#MACRO_COMBINATION_INFEASIBLE} and
 * {@link FatWarningCode#LOW_CARBOHYDRATE_HEADROOM} inside
 * {@code FatCalculationResult} — those codes are not duplicated here.</p>
 *
 * <p>Follows the same convention as {@link FatWarningCode},
 * {@link ProteinWarningCode}, and {@link CalorieWarningCode}.</p>
 */
public enum CarbohydrateWarningCode {

    /**
     * Remaining calories for carbohydrates are exactly zero.
     *
     * <p>Carbohydrate grams will be zero. This is distinct from the
     * upstream {@code LOW_CARBOHYDRATE_HEADROOM} warning and indicates
     * that no carbohydrates can be allocated at the current macro settings.</p>
     */
    ZERO_CARBOHYDRATE_HEADROOM,

    /**
     * Remaining calories for carbohydrates are negative — the upstream
     * macro combination was already infeasible at the fat layer.
     *
     * <p>Carbohydrate grams will be clamped to zero.
     * This warning propagates the upstream infeasibility state into the
     * carbohydrate result so that callers have a single summary point.</p>
     */
    UPSTREAM_INFEASIBLE,

    /**
     * The upstream {@code FatCalculationResult} carries a
     * {@code CLINICAL_REVIEW_REQUIRED} safety status.
     *
     * <p>This warning is added when the carbohydrate result is being
     * produced despite an upstream clinical-review flag, so that the
     * carbohydrate result alone surfaces that the full macro split
     * requires review.</p>
     */
    UPSTREAM_CLINICAL_REVIEW_REQUIRED
}
