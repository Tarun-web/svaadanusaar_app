package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * The output of {@code ProteinCalculator}.
 *
 * <p>Contains only the <em>long-term</em> physiological protein target —
 * it does not contain adaptation state or the user's current recommended
 * target. That is handled by {@link ProteinAdaptationResult}.</p>
 *
 * <h3>Bioavailability note</h3>
 * <p>The {@code longTermTargetGrams} value is the dietary protein target
 * expressed in grams. No bioavailability multiplier is applied here.
 * Protein quality / digestibility is evaluated later when actual foods
 * and recipes are selected.</p>
 */
public record ProteinTargetResult(

        /**
         * The calculated long-term daily protein target in grams.
         *
         * <p>Represents what the user should ultimately consume each day
         * once fully adapted.</p>
         */
        BigDecimal longTermTargetGrams,

        /**
         * The weight basis used for the calculation.
         *
         * <p>Either {@code "LBM"} (lean body mass) or {@code "BODY_WEIGHT"}.</p>
         */
        String calculationBasis,

        /**
         * The actual weight value (in kg) used for the protein-per-kg calculation.
         *
         * <p>Either LBM or total body weight, depending on {@link #calculationBasis}.</p>
         */
        BigDecimal weightUsedKg,

        /**
         * The protein-per-kg multiplier applied for this goal/training combination.
         */
        BigDecimal proteinPerKg,

        /**
         * The overall safety status of the calculated target.
         *
         * <p>Uses the same {@link CalorieSafetyStatus} taxonomy as the calorie
         * safety pipeline for consistency.</p>
         */
        CalorieSafetyStatus safetyStatus,

        /**
         * Whether clinical review is required before this target should be used.
         *
         * <p>Set to {@code true} when KIDNEY_DISEASE or LIVER_DISEASE is present,
         * or when another condition prevents an automated safe recommendation.</p>
         */
        boolean requiresClinicalReview,

        /**
         * Any warnings or safety notices associated with this calculation.
         */
        List<ProteinWarning> warnings

) {
}
