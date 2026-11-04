package com.dietapp.diet_app.health_profile.NutritionTarget.enums;

public enum CalorieWarningCode {

    MEDICAL_CONDITION_PRESENT,

    TARGET_RATE_TOO_AGGRESSIVE,

    CALORIE_TARGET_ADJUSTED,

    LOW_BODY_WEIGHT,

    HIGH_BODY_WEIGHT,

    TARGET_DATE_TOO_SOON,

    MISSING_GENDER,

    CLINICAL_REVIEW_REQUIRED,

    /**
     * General advisory notice from a downstream macro/hydration calculator
     * (protein, fat, carbohydrate, or water layer).
     *
     * <p>Used when wrapping non-calorie-layer warnings into the unified
     * {@code NutritionTargetResponse.warnings} list.</p>
     */
    GENERAL_ADVISORY
}
