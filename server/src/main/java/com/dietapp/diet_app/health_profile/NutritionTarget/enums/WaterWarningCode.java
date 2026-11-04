package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

/**
 * Machine-readable codes for water-calculation warnings.
 *
 * <p>Kept intentionally small — only codes directly produced by
 * {@code WaterCalculator} are defined here.
 * No medical-condition codes are included; medical hydration
 * restrictions belong to a downstream safety layer.</p>
 *
 * <p>Follows the same convention as {@link FatWarningCode},
 * {@link ProteinWarningCode}, and {@link CarbohydrateWarningCode}.</p>
 */
public enum WaterWarningCode {

    /**
     * Input weight was invalid (null, zero, or negative).
     *
     * <p>The calculator throws {@link IllegalArgumentException} in this
     * case, so this code is reserved for edge-case logging or future
     * soft-validation paths.</p>
     */
    INVALID_WEIGHT,

    /**
     * No activity data was provided.
     *
     * <p>Hydration target is based on body weight only; no exercise
     * adjustment has been applied.</p>
     */
    NO_ACTIVITIES,

    /**
     * At least one {@code ActivityProfile} entry contained invalid data
     * (null intensity, non-positive duration, or non-positive daysPerWeek).
     *
     * <p>The offending activity was skipped to avoid a misleading total.</p>
     */
    INVALID_ACTIVITY_DATA
}
