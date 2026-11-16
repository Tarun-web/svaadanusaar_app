package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The full nutrition target response returned by
 * {@code NutritionTargetService.calculateForCurrentUser()}.
 *
 * <h3>Calorie fields</h3>
 * <ul>
 *   <li>{@link #tdee} — Total Daily Energy Expenditure before goal adjustment.</li>
 *   <li>{@link #proposedCalories} — Calories proposed by GoalCalorieCalculator
 *       before safety evaluation.</li>
 *   <li>{@link #finalCalories} — Calories after CalorieSafetyEngine evaluation.
 *       This is what the user's macro budget is calculated against.</li>
 * </ul>
 *
 * <h3>Macro fields</h3>
 * <ul>
 *   <li>{@link #proteinGrams} — Current adaptation target (NOT the long-term target).</li>
 *   <li>{@link #longTermProteinGrams} — The eventual physiological protein target.</li>
 *   <li>{@link #fatGrams} — Daily fat target.</li>
 *   <li>{@link #carbohydrateGrams} — Daily carbohydrate target (flexible remainder).</li>
 *   <li>{@link #fibreGrams} — Daily fibre target (14 g / 1000 kcal).</li>
 *   <li>{@link #waterMl} — Daily hydration planning target.</li>
 * </ul>
 *
 * <h3>Protein adaptation fields</h3>
 * <ul>
 *   <li>{@link #adaptationStage} — Current phase of the protein ramp-up journey.</li>
 *   <li>{@link #rampUpRequired} — Whether a ramp-up schedule is active.</li>
 *   <li>{@link #nextEligibleProteinIncreaseDate} — Earliest date the protein
 *       target may be increased.</li>
 *   <li>{@link #currentProteinIntakeGrams} — Latest user-reported intake.</li>
 *   <li>{@link #adaptationBaselineProteinGrams} — Intake stored when the
 *       adaptation was initialised.</li>
 *   <li>{@link #adaptationBaselineOutOfDate} — Whether the two differ.</li>
 * </ul>
 */
@Builder
public record NutritionTargetResponse(

        // ── Calorie pipeline ─────────────────────────────────────────────

        /** Total Daily Energy Expenditure before goal adjustment. */
        BigDecimal tdee,

        /** Calories proposed before safety evaluation. */
        BigDecimal proposedCalories,

        /** Calories after safety evaluation — the macro budget. */
        BigDecimal finalCalories,

        boolean safetyAdjusted,

        CalorieSafetyStatus status,

        boolean requiresClinicalReview,

        // ── Macro targets ────────────────────────────────────────────────

        /**
         * Current recommended daily protein target in grams.
         *
         * <p>This is the <strong>current adaptation target</strong>, not the
         * long-term goal.  While the user is adapting, this will be lower than
         * {@link #longTermProteinGrams}.</p>
         */
        BigDecimal proteinGrams,

        /**
         * The long-term physiological protein target in grams.
         *
         * <p>This is what the user will eventually reach once adaptation is complete.</p>
         */
        BigDecimal longTermProteinGrams,

        /** Daily fat target in grams. */
        BigDecimal fatGrams,

        /** Daily carbohydrate target in grams. */
        BigDecimal carbohydrateGrams,

        /** Daily fibre target in grams (14 g / 1000 kcal). */
        BigDecimal fibreGrams,

        /** Daily hydration planning target in millilitres. */
        BigDecimal waterMl,

        // ── Protein adaptation ───────────────────────────────────────────

        /** Current phase of the protein ramp-up journey. */
        ProteinAdaptationStage adaptationStage,

        /**
         * Whether an active protein ramp-up schedule is in place.
         *
         * <p>{@code true} means {@link #proteinGrams} is below
         * {@link #longTermProteinGrams} and the system is incrementally
         * adapting toward it.</p>
         */
        boolean rampUpRequired,

        /**
         * Earliest date the protein target may next be increased.
         *
         * <p>{@code null} when no ramp-up is active or when tolerance
         * prevents progression.</p>
         */
        LocalDate nextEligibleProteinIncreaseDate,

        /**
         * The user's latest reported protein intake in grams
         * ({@code HealthProfile.currentNutritionProfile.currentProteinGrams}).
         * {@code null} when unknown or explicitly cleared.
         */
        BigDecimal currentProteinIntakeGrams,

        /**
         * The intake stored on the adaptation profile when adaptation was
         * initialised. It is a historical baseline and is not overwritten
         * when the reported intake changes. {@code null} when none was captured.
         */
        BigDecimal adaptationBaselineProteinGrams,

        /**
         * {@code true} when exactly one of {@link #currentProteinIntakeGrams}
         * and {@link #adaptationBaselineProteinGrams} is {@code null}, or both
         * are non-null and differ. {@code false} when both are {@code null}
         * or both are equal.
         */
        boolean adaptationBaselineOutOfDate,

        // ── Aggregated warnings ──────────────────────────────────────────

        /**
         * All warnings from the full pipeline (calorie, protein, fat,
         * carbohydrate, water).
         */
        List<CalorieWarning> warnings

) {
}
