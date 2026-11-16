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
 *   adaptationBaselineProteinGrams = intake captured when the adaptation was
 *                                    initialised (historical baseline)
 *   currentTargetGrams             = what Svaadanusaar currently recommends
 *   longTermTargetGrams            = the eventual calculated physiological target
 * </pre>
 *
 * <p>This is <strong>not</strong> the user's latest reported intake. That
 * lives in {@code HealthProfile.currentNutritionProfile.currentProteinGrams}
 * and is exposed separately on {@code NutritionTargetResponse}.</p>
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
         * The stored adaptation baseline
         * ({@code ProteinAdaptationProfile.currentProteinGrams}): the intake
         * recorded when adaptation was initialised. {@code null} when no
         * baseline was captured. It is never updated by later changes to the
         * user's reported intake.
         */
        BigDecimal adaptationBaselineProteinGrams,

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
         * Warnings generated during adaptation evaluation, combined with
         * the warnings produced by {@code ProteinCalculator} for the
         * long-term target.
         */
        List<ProteinWarning> warnings,

        /**
         * Whether clinical review is required before the protein target is
         * used. Propagated from {@link ProteinTargetResult#requiresClinicalReview()}.
         */
        boolean requiresClinicalReview

) {

    /**
     * Convenience constructor for results that carry no clinical-review
     * requirement ({@code requiresClinicalReview = false}).
     */
    public ProteinAdaptationResult(
            BigDecimal currentTargetGrams,
            BigDecimal longTermTargetGrams,
            BigDecimal adaptationBaselineProteinGrams,
            ProteinAdaptationStage adaptationStage,
            boolean rampUpRequired,
            LocalDate nextEligibleIncreaseDate,
            ProteinToleranceStatus toleranceStatus,
            List<ProteinWarning> warnings) {

        this(currentTargetGrams, longTermTargetGrams, adaptationBaselineProteinGrams,
                adaptationStage, rampUpRequired, nextEligibleIncreaseDate,
                toleranceStatus, warnings, false);
    }
}
