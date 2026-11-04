package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The output of {@code ProteinAdaptationService}.
 *
 * <p>Describes the user's <em>current</em> recommended protein target,
 * their progress toward the long-term target, and any warnings that
 * should be surfaced to the user or the diet engine.</p>
 *
 * <h3>Key distinction</h3>
 * <pre>
 *   currentProteinIntakeGrams  = what the user currently eats
 *   currentTargetGrams         = what Svaadanusaar currently recommends
 *   longTermTargetGrams        = the eventual calculated physiological target
 * </pre>
 */
public record ProteinAdaptationResult(

        /**
         * What Svaadanusaar currently recommends for this user.
         *
         * <p>This value should be written to
         * {@code NutritionTarget.proteinGrams} — it is the current
         * recommendation, not the long-term target.</p>
         */
        BigDecimal currentTargetGrams,

        /**
         * The eventual long-term daily protein target.
         */
        BigDecimal longTermTargetGrams,

        /**
         * The user's known current protein intake, if available.
         * {@code null} when intake is unknown.
         */
        BigDecimal currentProteinIntakeGrams,

        /**
         * The current phase of the adaptation journey.
         */
        ProteinAdaptationStage adaptationStage,

        /**
         * Whether a ramp-up schedule is active and progression is pending.
         */
        boolean rampUpRequired,

        /**
         * The earliest date the target may be increased, if applicable.
         * {@code null} when no ramp-up is in progress or when tolerance
         * prevents progression.
         */
        LocalDate nextEligibleIncreaseDate,

        /**
         * The user's GI / digestive tolerance status at the current target.
         */
        ProteinToleranceStatus toleranceStatus,

        /**
         * Warnings generated during adaptation evaluation.
         */
        List<ProteinWarning> warnings

) {
}
