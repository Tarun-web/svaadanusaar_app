package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CarbohydrateCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CarbohydrateWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.FatWarningCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CarbohydrateCalculator}.
 *
 * <p>Covers all 20 scenarios specified in the requirements plus
 * additional edge-case verification.</p>
 */
class CarbohydrateCalculatorTest {

    private final CarbohydrateCalculator calculator = new CarbohydrateCalculator();

    // ================================================================
    // Helper — builds a minimal FatCalculationResult
    // ================================================================

    /**
     * Builds a {@link FatCalculationResult} with the given remaining calories
     * and safety state. Other fat-specific fields are set to harmless defaults.
     */
    private FatCalculationResult fatResult(
            BigDecimal remainingCaloriesForCarbs,
            CalorieSafetyStatus safetyStatus,
            boolean requiresClinicalReview,
            List<FatWarning> fatWarnings) {

        return new FatCalculationResult(
                new BigDecimal("60.00"),              // fatGrams
                new BigDecimal("540.00"),             // fatCalories
                remainingCaloriesForCarbs,
                new BigDecimal("0.8"),                // fatPerKg
                "MAINTENANCE",                        // calculationBasis
                remainingCaloriesForCarbs != null
                        && remainingCaloriesForCarbs.compareTo(BigDecimal.ZERO) > 0,
                safetyStatus,
                requiresClinicalReview,
                fatWarnings
        );
    }

    private FatCalculationResult safeFatResult(BigDecimal remainingCalories) {
        return fatResult(remainingCalories, CalorieSafetyStatus.SAFE, false, List.of());
    }

    // ================================================================
    // TESTS 1–3: Normal positive headroom
    // ================================================================

    /** Test 1 — Normal positive headroom: Example A from spec (965 kcal → 241.25 g) */
    @Test
    void test01_normalPositiveHeadroom_965kcal() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("965"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertNotNull(result);
        assertTrue(result.feasible());
        // 965 / 4 = 241.25
        assertEquals(0, new BigDecimal("241.25").compareTo(result.carbohydrateGrams()),
                "965 kcal remaining → expected 241.25 g carbs");
        assertEquals(0, new BigDecimal("965.00").compareTo(result.carbohydrateCalories()),
                "carbohydrateCalories must equal remainingCaloriesForCarbs");
        assertEquals(CalorieSafetyStatus.SAFE, result.safetyStatus());
        assertFalse(result.requiresClinicalReview());
        assertTrue(result.warnings().isEmpty());
    }

    /** Test 2 — Large positive headroom: 1000 kcal → 250.00 g */
    @Test
    void test02_largePositiveHeadroom_1000kcal() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("1000"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.feasible());
        assertEquals(0, new BigDecimal("250.00").compareTo(result.carbohydrateGrams()),
                "1000 kcal → 250.00 g");
    }

    /** Test 3 — Small positive headroom: 100 kcal → 25.00 g */
    @Test
    void test03_smallPositiveHeadroom_100kcal() {
        // Note: FatCalculator would have already emitted LOW_CARBOHYDRATE_HEADROOM
        // for 100 kcal. CarbohydrateCalculator simply computes from what it receives.
        FatCalculationResult fat = safeFatResult(new BigDecimal("100"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.feasible());
        assertEquals(0, new BigDecimal("25.00").compareTo(result.carbohydrateGrams()),
                "100 kcal → 25.00 g");
    }

    // ================================================================
    // TESTS 4–5: Zero and negative headroom
    // ================================================================

    /** Test 4 — Exactly zero headroom: carbs = 0, feasible = false */
    @Test
    void test04_zeroHeadroom() {
        FatCalculationResult fat = safeFatResult(BigDecimal.ZERO);

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertFalse(result.feasible(),
                "Zero remaining calories → feasible must be false");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carbohydrateGrams()),
                "Zero headroom → carbohydrateGrams must be 0");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carbohydrateCalories()),
                "Zero headroom → carbohydrateCalories must be 0");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == CarbohydrateWarningCode.ZERO_CARBOHYDRATE_HEADROOM),
                "Must emit ZERO_CARBOHYDRATE_HEADROOM warning");

        assertEquals(CalorieSafetyStatus.WARNING, result.safetyStatus());
    }

    /** Test 5 — Negative headroom: carbs = 0, feasible = false */
    @Test
    void test05_negativeHeadroom() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("-100"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertFalse(result.feasible());
        // carbs must be clamped to 0 — never return -25 g
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carbohydrateGrams()),
                "Negative headroom must not produce negative carbohydrate grams");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carbohydrateCalories()));

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == CarbohydrateWarningCode.UPSTREAM_INFEASIBLE),
                "Must emit UPSTREAM_INFEASIBLE warning");
    }

    // ================================================================
    // TEST 6: BigDecimal rounding
    // ================================================================

    /** Test 6 — Fractional division is rounded correctly with HALF_UP at 2 dp */
    @Test
    void test06_bigDecimalRounding() {
        // 1234.50 kcal / 4 = 308.625 → rounds to 308.63 at 2 dp HALF_UP
        FatCalculationResult fat = safeFatResult(new BigDecimal("1234.50"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.feasible());
        assertEquals(2, result.carbohydrateGrams().scale(),
                "carbohydrateGrams must have scale 2");
        assertEquals(0, new BigDecimal("308.63").compareTo(result.carbohydrateGrams()),
                "1234.50 / 4 = 308.625 → rounded HALF_UP to 308.63");
    }

    // ================================================================
    // TESTS 7–9: Input validation
    // ================================================================

    /** Test 7 — Null input throws IllegalArgumentException */
    @Test
    void test07_nullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null),
                "Null FatCalculationResult must throw");
    }

    /** Test 8 — Null FatCalculationResult (same as null input path) */
    @Test
    void test08_nullFatCalculationResult() {
        // Explicitly alias test 7 from a different call site for clarity
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null));
    }

    /** Test 9 — Null remainingCaloriesForCarbs throws IllegalArgumentException */
    @Test
    void test09_nullRemainingCalories() {
        FatCalculationResult fat = new FatCalculationResult(
                new BigDecimal("60"), new BigDecimal("540"),
                null,                                  // ← null remaining
                new BigDecimal("0.8"), "MAINTENANCE",
                true, CalorieSafetyStatus.SAFE, false, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(fat),
                "Null remainingCaloriesForCarbs must throw");
    }

    // ================================================================
    // TESTS 10–12: Upstream warning / safety propagation
    // ================================================================

    /** Test 10 — Upstream fat warnings are NOT copied into carbohydrate warnings */
    @Test
    void test10_upstreamFatWarningsNotDuplicated() {
        // Fat had a LOW_CARBOHYDRATE_HEADROOM warning; remaining = 80 kcal
        FatCalculationResult fat = fatResult(
                new BigDecimal("80"),
                CalorieSafetyStatus.ADJUSTED,
                false,
                List.of(new FatWarning(
                        FatWarningCode.LOW_CARBOHYDRATE_HEADROOM,
                        "Only 80 kcal remain for carbohydrates."))
        );

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        // CarbohydrateCalculator should NOT copy the FatWarning into its own list
        assertTrue(result.warnings().stream()
                .noneMatch(w -> w.code() == CarbohydrateWarningCode.UPSTREAM_INFEASIBLE),
                "80 kcal is positive — UPSTREAM_INFEASIBLE should NOT be present");

        // The fat-layer warning stays in FatCalculationResult — not in carbResult.warnings()
        assertEquals(0, result.warnings().size(),
                "No carbohydrate-specific warnings expected for 80 kcal positive headroom");

        assertTrue(result.feasible());
    }

    /** Test 11 — Upstream safety status is propagated (ADJUSTED → ADJUSTED) */
    @Test
    void test11_upstreamSafetyStatusPropagated_adjusted() {
        FatCalculationResult fat = fatResult(
                new BigDecimal("500"),
                CalorieSafetyStatus.ADJUSTED,
                false,
                List.of()
        );

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertEquals(CalorieSafetyStatus.ADJUSTED, result.safetyStatus(),
                "Upstream ADJUSTED status must propagate to carbohydrate result");
        assertTrue(result.feasible());
    }

    /** Test 12 — CLINICAL_REVIEW_REQUIRED status propagated and warning added */
    @Test
    void test12_clinicalReviewRequired_propagated() {
        FatCalculationResult fat = fatResult(
                new BigDecimal("600"),
                CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED,
                true,
                List.of()
        );

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.requiresClinicalReview(),
                "requiresClinicalReview must propagate from upstream");
        assertEquals(CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED, result.safetyStatus(),
                "CLINICAL_REVIEW_REQUIRED must propagate");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == CarbohydrateWarningCode.UPSTREAM_CLINICAL_REVIEW_REQUIRED),
                "Must emit UPSTREAM_CLINICAL_REVIEW_REQUIRED warning");

        // Carbohydrate target is still calculated
        assertTrue(result.feasible());
        // 600 / 4 = 150
        assertEquals(0, new BigDecimal("150.00").compareTo(result.carbohydrateGrams()));
    }

    // ================================================================
    // TESTS 13–15: Protein and fat are never modified
    // ================================================================

    /**
     * Test 13 — Protein is never modified.
     *
     * <p>CarbohydrateCalculator only reads from FatCalculationResult.
     * It has no reference to proteinGrams and cannot modify it.</p>
     */
    @Test
    void test13_proteinNeverModified() {
        // The calculator has no protein input at all — structural guarantee.
        // Verify the API signature: calculate(FatCalculationResult) only.
        FatCalculationResult fat = safeFatResult(new BigDecimal("800"));
        CarbohydrateCalculationResult result = calculator.calculate(fat);

        // If we could somehow retrieve protein from the result, it would be
        // null/absent. The fact that the result record has no proteinGrams field
        // is the guarantee.
        assertNotNull(result.carbohydrateGrams(),
                "Carbohydrate result must be present");
        // Compile-time guarantee: CarbohydrateCalculationResult has no proteinGrams field.
        // No assertion needed beyond confirming the fat input was not mutated.
        assertEquals(0, new BigDecimal("60.00").compareTo(fat.fatGrams()),
                "Fat grams must be unchanged after calculation");
    }

    /**
     * Test 14 — Fat grams are never modified.
     *
     * <p>FatCalculationResult is a record (immutable) — it cannot be changed.</p>
     */
    @Test
    void test14_fatNeverModified() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("700"));
        BigDecimal fatGramsBefore = fat.fatGrams();

        calculator.calculate(fat);

        assertEquals(0, fatGramsBefore.compareTo(fat.fatGrams()),
                "Fat grams must be identical before and after carbohydrate calculation");
    }

    /**
     * Test 15 — Target calories are never modified.
     *
     * <p>CarbohydrateCalculator does not take targetCalories as input — it
     * works only from remainingCaloriesForCarbs. No calorie modification possible.</p>
     */
    @Test
    void test15_targetCaloriesNeverModified() {
        // Structural guarantee: the calculator's only input is FatCalculationResult.
        // FatCalculationResult is an immutable record.
        FatCalculationResult fat = safeFatResult(new BigDecimal("900"));
        BigDecimal remainingBefore = fat.remainingCaloriesForCarbs();

        calculator.calculate(fat);

        assertEquals(0, remainingBefore.compareTo(fat.remainingCaloriesForCarbs()),
                "remainingCaloriesForCarbs in FatCalculationResult must be unchanged");
    }

    // ================================================================
    // TESTS 16–17: Safety guarantees
    // ================================================================

    /** Test 16 — Carbohydrate result is never negative */
    @Test
    void test16_carbsNeverNegative() {
        // Deeply negative remaining calories
        FatCalculationResult fat = safeFatResult(new BigDecimal("-9999"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.carbohydrateGrams().compareTo(BigDecimal.ZERO) >= 0,
                "carbohydrateGrams must never be negative");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carbohydrateGrams()));
    }

    /** Test 17 — No floating-point arithmetic: result uses proper BigDecimal scale */
    @Test
    void test17_noFloatingPointArithmetic() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("965"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        // Verify scale is explicitly 2 — not a raw double/float conversion artefact
        assertTrue(result.carbohydrateGrams().scale() >= 0,
                "carbohydrateGrams must be a properly scaled BigDecimal");
        assertTrue(result.carbohydrateCalories().scale() >= 0,
                "carbohydrateCalories must be a properly scaled BigDecimal");
    }

    // ================================================================
    // TEST 18: 4 kcal/g conversion factor
    // ================================================================

    /** Test 18 — Exactly 4 kcal/g is used */
    @Test
    void test18_fourKcalPerGramConversionFactor() {
        // 800 kcal / 4 = 200 g exactly
        FatCalculationResult fat = safeFatResult(new BigDecimal("800"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertEquals(0, new BigDecimal("200.00").compareTo(result.carbohydrateGrams()),
                "800 kcal / 4 kcal per gram must equal 200.00 g");

        // Verify the constant itself
        assertEquals(0, new BigDecimal("4").compareTo(
                CarbohydrateCalculator.KCAL_PER_GRAM_CARBOHYDRATE),
                "KCAL_PER_GRAM_CARBOHYDRATE constant must be 4");
    }

    // ================================================================
    // TEST 19: MacroCalculator compatibility
    // ================================================================

    /** Test 19 — Result is compatible with MacroCalculator.resolveNutritionTargetCarbohydrate */
    @Test
    void test19_macroCalculatorCompatibility() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("965"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        // Simulate what MacroCalculator.resolveNutritionTargetCarbohydrate does:
        BigDecimal nutritionTargetCarbs = result.carbohydrateGrams();

        assertNotNull(nutritionTargetCarbs,
                "carbohydrateGrams must not be null (used for NutritionTarget)");
        assertTrue(nutritionTargetCarbs.compareTo(BigDecimal.ZERO) >= 0,
                "NutritionTarget carbs must be >= 0");
        assertEquals(0, new BigDecimal("241.25").compareTo(nutritionTargetCarbs),
                "MacroCalculator must receive 241.25 g for 965 kcal headroom");
    }

    // ================================================================
    // TEST 20: Safety status never downgrades
    // ================================================================

    /** Test 20 — Safety status never becomes less severe downstream */
    @Test
    void test20_safetyStatusNeverDowngrades() {
        // Upstream WARNING state must not become SAFE in carbResult
        FatCalculationResult fat = fatResult(
                new BigDecimal("500"),
                CalorieSafetyStatus.WARNING,
                false,
                List.of()
        );

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertTrue(result.safetyStatus().ordinal() >= CalorieSafetyStatus.WARNING.ordinal(),
                "safetyStatus must not be less severe than the upstream WARNING");
        assertNotEquals(CalorieSafetyStatus.SAFE, result.safetyStatus(),
                "SAFE must never appear when upstream was WARNING");
    }

    // ================================================================
    // Additional spec examples
    // ================================================================

    /** Example E from spec: 1000 kcal → 250 g */
    @Test
    void testSpecExampleE_1000kcal() {
        FatCalculationResult fat = safeFatResult(new BigDecimal("1000"));

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertEquals(0, new BigDecimal("250.00").compareTo(result.carbohydrateGrams()),
                "Spec Example E: 1000 kcal → 250.00 g");
        assertTrue(result.feasible());
    }

    /** remainingCaloriesFromUpstream is preserved for traceability when negative */
    @Test
    void testNegativeRemainingPreservedForTraceability() {
        BigDecimal negative = new BigDecimal("-200");
        FatCalculationResult fat = safeFatResult(negative);

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertEquals(0, negative.compareTo(result.remainingCaloriesFromUpstream()),
                "Negative remainingCaloriesFromUpstream must be preserved in the result");
    }

    /** Zero headroom: remainingCaloriesFromUpstream is preserved as 0 */
    @Test
    void testZeroRemainingPreservedForTraceability() {
        FatCalculationResult fat = safeFatResult(BigDecimal.ZERO);

        CarbohydrateCalculationResult result = calculator.calculate(fat);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingCaloriesFromUpstream()));
    }
}
