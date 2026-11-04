package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CarbohydrateCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Orchestrates macro-nutrient calculation for a user.
 *
 * <h3>Implemented phases</h3>
 * <ul>
 *   <li><strong>Phase 1 — Protein:</strong> {@link ProteinCalculator} +
 *       {@link ProteinAdaptationService}</li>
 *   <li><strong>Phase 2 — Fat:</strong> {@link FatCalculator}</li>
 *   <li><strong>Phase 3 — Carbohydrate:</strong> {@link CarbohydrateCalculator}</li>
 * </ul>
 *
 * <p>Fibre and water will be added in future phases following the same pattern.</p>
 *
 * <h3>Protein flow</h3>
 * <pre>
 *   HealthProfile
 *       ↓
 *   ProteinCalculator       →  long-term protein target
 *       ↓
 *   ProteinAdaptationService  →  current recommended protein target
 *       ↓
 *   NutritionTarget.proteinGrams  ← currentTargetGrams
 * </pre>
 *
 * <h3>Fat flow</h3>
 * <pre>
 *   ProteinAdaptationResult.currentTargetGrams
 *       +
 *   targetCalories, goal, training context
 *       ↓
 *   FatCalculator  →  FatCalculationResult
 *       ↓
 *   NutritionTarget.fatGrams  ← fatGrams
 * </pre>
 *
 * <h3>Important: proteinGrams vs longTermTarget</h3>
 * <p>{@code NutritionTarget.proteinGrams} must always store the
 * <em>current adaptation target</em>, not the long-term target, while the
 * user is still adapting.  Fat feasibility is always checked against the
 * <em>current</em> protein target.</p>
 */
@Component
@RequiredArgsConstructor
public class MacroCalculator {

    private final ProteinCalculator         proteinCalculator;
    private final ProteinAdaptationService  proteinAdaptationService;
    private final FatCalculator             fatCalculator;
    private final CarbohydrateCalculator    carbohydrateCalculator;

    // ================================================================
    // Protein
    // ================================================================

    /**
     * Calculates the current recommended protein target for the user.
     *
     * @param healthProfile      the user's full health profile
     * @param input              protein calculation parameters
     * @param existingAdaptation the current adaptation profile, if one exists;
     *                           pass {@code null} to force initialisation
     * @return the current recommended protein target for use in
     *         {@code NutritionTarget.proteinGrams}
     */
    public ProteinAdaptationResult calculateProtein(
            HealthProfile healthProfile,
            ProteinCalculationInput input,
            ProteinAdaptationProfile existingAdaptation) {

        ProteinTargetResult longTermResult =
                proteinCalculator.calculate(input);

        if (existingAdaptation == null
                || existingAdaptation.getAdaptationStage()
                        == ProteinAdaptationStage.NOT_STARTED) {

            ProteinAdaptationProfile profile =
                    proteinAdaptationService.initialise(
                            healthProfile,
                            longTermResult,
                            input.currentProteinGrams()
                    );
            return proteinAdaptationService.toResult(profile);
        }

        return proteinAdaptationService.toResult(existingAdaptation);
    }

    /**
     * Returns the protein value that should be written to
     * {@code NutritionTarget.proteinGrams}.
     *
     * <p>Always the <em>current adaptation target</em>, not the long-term goal.</p>
     */
    public BigDecimal resolveNutritionTargetProtein(
            ProteinAdaptationResult adaptationResult) {

        return adaptationResult.currentTargetGrams();
    }

    // ================================================================
    // Fat
    // ================================================================

    /**
     * Calculates the daily fat target for the user.
     *
     * <p>The {@code proteinGrams} in {@code fatInput} must be the
     * <em>current adaptation target</em> from
     * {@link ProteinAdaptationResult#currentTargetGrams()}, not the
     * long-term protein target.</p>
     *
     * @param fatInput fat calculation parameters (must include current protein target)
     * @return a {@link FatCalculationResult} with fat grams, fat calories,
     *         carbohydrate headroom, and any warnings
     */
    public FatCalculationResult calculateFat(FatCalculationInput fatInput) {
        return fatCalculator.calculate(fatInput);
    }

    /**
     * Returns the fat value that should be written to
     * {@code NutritionTarget.fatGrams}.
     */
    public BigDecimal resolveNutritionTargetFat(FatCalculationResult fatResult) {
        return fatResult.fatGrams();
    }

    // ================================================================
    // Carbohydrate
    // ================================================================

    /**
     * Calculates the daily carbohydrate target from the upstream
     * {@link FatCalculationResult}.
     *
     * <p>Carbohydrates receive whatever calories remain after protein and
     * fat have been allocated — there are no goal-specific multipliers.</p>
     *
     * @param fatResult the result from {@link #calculateFat}
     * @return a {@link CarbohydrateCalculationResult} with grams, calories,
     *         feasibility, and any warnings
     */
    public CarbohydrateCalculationResult calculateCarbohydrate(
            FatCalculationResult fatResult) {
        return carbohydrateCalculator.calculate(fatResult);
    }

    /**
     * Returns the carbohydrate value that should be written to
     * {@code NutritionTarget.carbohydrateGrams}.
     */
    public BigDecimal resolveNutritionTargetCarbohydrate(
            CarbohydrateCalculationResult carbResult) {
        return carbResult.carbohydrateGrams();
    }
}
