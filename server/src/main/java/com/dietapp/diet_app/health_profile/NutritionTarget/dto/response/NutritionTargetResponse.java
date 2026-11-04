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

        // ── Aggregated warnings ──────────────────────────────────────────

        /**
         * All warnings from the full pipeline (calorie, protein, fat,
         * carbohydrate, water).
         */
        List<CalorieWarning> warnings

) {
}
