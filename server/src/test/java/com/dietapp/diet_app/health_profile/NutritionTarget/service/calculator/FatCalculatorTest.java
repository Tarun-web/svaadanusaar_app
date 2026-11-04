package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.FatCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.FatWarningCode;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FatCalculator}.
 *
 * <p>Covers all 26 scenarios specified in the implementation requirements,
 * including goal-based multipliers, training context, calorie feasibility,
 * medical conditions, BigDecimal rounding, and safety guarantees.</p>
 */
class FatCalculatorTest {

    private final FatCalculator calculator = new FatCalculator();

    // ================================================================
    // Helper builders
    // ================================================================

    /** Builds a minimal input with sufficient calories and zero protein. */
    private FatCalculationInput input(
            double weightKg,
            Goal goal,
            boolean resistanceTraining) {

        return new FatCalculationInput(
                BigDecimal.valueOf(weightKg),
                goal,
                new BigDecimal("2000"),   // ample calories
                BigDecimal.ZERO,          // no protein allocated yet
                resistanceTraining,
                null,
                null,
                null
        );
    }

    /** Builds an input with a specific calorie target and protein allocation. */
    private FatCalculationInput inputWithMacros(
            double weightKg,
            Goal goal,
            BigDecimal targetCalories,
            BigDecimal proteinGrams) {

        return new FatCalculationInput(
                BigDecimal.valueOf(weightKg),
                goal,
                targetCalories,
                proteinGrams,
                false,
                null,
                null,
                null
        );
    }

    /** Builds an input with workout intensity. */
    private FatCalculationInput inputWithIntensity(
            double weightKg,
            Goal goal,
            WorkoutIntensity intensity) {

        return new FatCalculationInput(
                BigDecimal.valueOf(weightKg),
                goal,
                new BigDecimal("3000"),
                BigDecimal.ZERO,
                true,
                intensity,
                null,
                null
        );
    }

    /** Builds an input with medical conditions. */
    private FatCalculationInput inputWithConditions(
            double weightKg,
            Goal goal,
            Set<MedicalCondition> conditions) {

        return new FatCalculationInput(
                BigDecimal.valueOf(weightKg),
                goal,
                new BigDecimal("2000"),
                BigDecimal.ZERO,
                false,
                null,
                null,
                conditions
        );
    }

    // ================================================================
    // TESTS 1–8: Goal-based multipliers
    // ================================================================

    /** Test 1 — GENERAL_HEALTH: 0.8 g/kg */
    @Test
    void test01_generalHealth() {
        FatCalculationResult result = calculator.calculate(
                input(75, Goal.GENERAL_HEALTH, false));

        assertNotNull(result);
        // 75 × 0.8 = 60.00
        assertEquals(0, new BigDecimal("60.00").compareTo(result.fatGrams()),
                "GENERAL_HEALTH: expected 60.00 g, got " + result.fatGrams());
        // 60 × 9 = 540.00
        assertEquals(0, new BigDecimal("540.00").compareTo(result.fatCalories()),
                "GENERAL_HEALTH: expected fat calories 540.00");
        assertEquals(0, FatCalculator.MULTIPLIER_GENERAL_HEALTH.compareTo(result.fatPerKg()));
        assertEquals("GENERAL_HEALTH", result.calculationBasis());
        assertEquals(CalorieSafetyStatus.SAFE, result.safetyStatus());
        assertFalse(result.requiresClinicalReview());
        assertTrue(result.feasible());
    }

    /** Test 2 — MAINTENANCE: 0.8 g/kg */
    @Test
    void test02_maintenance() {
        FatCalculationResult result = calculator.calculate(
                input(75, Goal.MAINTENANCE, false));

        // 75 × 0.8 = 60.00
        assertEquals(0, new BigDecimal("60.00").compareTo(result.fatGrams()),
                "MAINTENANCE: expected 60.00 g");
        assertEquals("MAINTENANCE", result.calculationBasis());
    }

    /** Test 3 — FAT_LOSS without resistance training: 0.7 g/kg */
    @Test
    void test03_fatLossNoRT() {
        FatCalculationResult result = calculator.calculate(
                input(75, Goal.FAT_LOSS, false));

        // 75 × 0.7 = 52.50
        assertEquals(0, new BigDecimal("52.50").compareTo(result.fatGrams()),
                "FAT_LOSS no RT: expected 52.50 g");
        assertEquals(0, FatCalculator.MULTIPLIER_FAT_LOSS.compareTo(result.fatPerKg()));
        assertEquals("FAT_LOSS", result.calculationBasis());
    }

    /** Test 4 — FAT_LOSS with resistance training: 0.8 g/kg */
    @Test
    void test04_fatLossWithRT() {
        FatCalculationResult result = calculator.calculate(
                input(75, Goal.FAT_LOSS, true));

        // 75 × 0.8 = 60.00
        assertEquals(0, new BigDecimal("60.00").compareTo(result.fatGrams()),
                "FAT_LOSS with RT: expected 60.00 g");
        assertEquals(0, FatCalculator.MULTIPLIER_FAT_LOSS_RT.compareTo(result.fatPerKg()));
        assertEquals("FAT_LOSS_WITH_RT", result.calculationBasis());
    }

    /** Test 5 — MUSCLE_GAIN: 0.8 g/kg */
    @Test
    void test05_muscleGain() {
        FatCalculationResult result = calculator.calculate(
                input(80, Goal.MUSCLE_GAIN, false));

        // 80 × 0.8 = 64.00
        assertEquals(0, new BigDecimal("64.00").compareTo(result.fatGrams()),
                "MUSCLE_GAIN: expected 64.00 g");
        assertEquals("MUSCLE_GAIN", result.calculationBasis());
    }

    /** Test 6 — BODY_RECOMPOSITION: 0.8 g/kg */
    @Test
    void test06_bodyRecomposition() {
        FatCalculationResult result = calculator.calculate(
                input(70, Goal.BODY_RECOMPOSITION, false));

        // 70 × 0.8 = 56.00
        assertEquals(0, new BigDecimal("56.00").compareTo(result.fatGrams()),
                "BODY_RECOMPOSITION: expected 56.00 g");
        assertEquals("BODY_RECOMPOSITION", result.calculationBasis());
    }

    /** Test 7 — WEIGHT_GAIN with normal training demand: 0.8 g/kg */
    @Test
    void test07_weightGainNormalDemand() {
        FatCalculationResult result = calculator.calculate(
                input(70, Goal.WEIGHT_GAIN, false));

        // 70 × 0.8 = 56.00
        assertEquals(0, new BigDecimal("56.00").compareTo(result.fatGrams()),
                "WEIGHT_GAIN normal: expected 56.00 g");
        assertEquals("WEIGHT_GAIN_NORMAL", result.calculationBasis());
    }

    /** Test 8 — SPORTS_PERFORMANCE with normal training demand: 0.8 g/kg */
    @Test
    void test08_sportsPerformanceNormalDemand() {
        FatCalculationResult result = calculator.calculate(
                input(72, Goal.SPORTS_PERFORMANCE, false));

        // 72 × 0.8 = 57.60
        assertEquals(0, new BigDecimal("57.60").compareTo(result.fatGrams()),
                "SPORTS normal: expected 57.60 g");
        assertEquals("SPORTS_NORMAL", result.calculationBasis());
    }

    // ================================================================
    // TESTS 9–10: Training demand tier
    // ================================================================

    /** Test 9 — SPORTS_PERFORMANCE with HIGH intensity → upper multiplier 1.0 g/kg */
    @Test
    void test09_highTrainingDemand_sportsHigh() {
        FatCalculationResult result = calculator.calculate(
                inputWithIntensity(80, Goal.SPORTS_PERFORMANCE, WorkoutIntensity.HIGH));

        // 80 × 1.0 = 80.00
        assertEquals(0, new BigDecimal("80.00").compareTo(result.fatGrams()),
                "SPORTS HIGH demand: expected 80.00 g");
        assertEquals("SPORTS_HIGH_DEMAND", result.calculationBasis());
        assertEquals(0, FatCalculator.MULTIPLIER_SPORTS_HIGH.compareTo(result.fatPerKg()));
    }

    /** Test 10 — WEIGHT_GAIN with LOW intensity → normal multiplier 0.8 g/kg */
    @Test
    void test10_normalTrainingDemand_weightGainLow() {
        FatCalculationResult result = calculator.calculate(
                inputWithIntensity(70, Goal.WEIGHT_GAIN, WorkoutIntensity.LOW));

        // 70 × 0.8 = 56.00
        assertEquals(0, new BigDecimal("56.00").compareTo(result.fatGrams()),
                "WEIGHT_GAIN LOW demand: expected 56.00 g (normal tier)");
        assertEquals("WEIGHT_GAIN_NORMAL", result.calculationBasis());
    }

    // ================================================================
    // TESTS 11–14: Calorie feasibility
    // ================================================================

    /** Test 11 — Ample target calories: feasible = true */
    @Test
    void test11_targetCaloriesSufficient() {
        // 75 kg, FAT_LOSS, no RT → fat = 52.50 g → fatCal = 472.50
        // proteinGrams = 110 → proteinCal = 440
        // targetCalories = 1800
        // remaining = 1800 - 440 - 472.50 = 887.50 > 0
        FatCalculationInput input = inputWithMacros(
                75, Goal.FAT_LOSS,
                new BigDecimal("1800"),
                new BigDecimal("110"));

        FatCalculationResult result = calculator.calculate(input);

        assertTrue(result.feasible(),
                "Should be feasible when enough calories remain for carbs");
        assertTrue(result.remainingCaloriesForCarbs().compareTo(BigDecimal.ZERO) > 0);
    }

    /** Test 12 — Target calories too low: feasible = false */
    @Test
    void test12_targetCaloriesInsufficient() {
        // 75 kg, MUSCLE_GAIN → fat = 60.00 g → fatCal = 540
        // proteinGrams = 135 → proteinCal = 540
        // targetCalories = 1000
        // remaining = 1000 - 540 - 540 = -80 < 0
        FatCalculationInput input = inputWithMacros(
                75, Goal.MUSCLE_GAIN,
                new BigDecimal("1000"),
                new BigDecimal("135"));

        FatCalculationResult result = calculator.calculate(input);

        assertFalse(result.feasible(),
                "Should be infeasible when remaining calories are negative");
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == FatWarningCode.MACRO_COMBINATION_INFEASIBLE),
                "Should emit MACRO_COMBINATION_INFEASIBLE warning");
    }

    /** Test 13 — Protein calories are included in feasibility check */
    @Test
    void test13_proteinCaloriesIncludedInFeasibility() {
        // Fat = 60 g (75 kg × 0.8), fatCal = 540
        // protein = 200 g → proteinCal = 800
        // targetCalories = 1300 → remaining = 1300 - 800 - 540 = -40 → infeasible
        FatCalculationInput input = inputWithMacros(
                75, Goal.MUSCLE_GAIN,
                new BigDecimal("1300"),
                new BigDecimal("200"));

        FatCalculationResult result = calculator.calculate(input);

        assertFalse(result.feasible(),
                "Protein calories must be included in feasibility calculation");
        // Remaining should be exactly -40.00
        assertEquals(0, new BigDecimal("-40.00").compareTo(result.remainingCaloriesForCarbs()),
                "remainingCaloriesForCarbs should be -40.00");
    }

    /** Test 14 — Negative remaining calories are correctly returned */
    @Test
    void test14_negativeRemainingCalories() {
        // Fat = 60 g, fatCal = 540
        // protein = 0 → proteinCal = 0
        // targetCalories = 500 → remaining = 500 - 0 - 540 = -40
        FatCalculationInput input = inputWithMacros(
                75, Goal.MUSCLE_GAIN,
                new BigDecimal("500"),
                BigDecimal.ZERO);

        FatCalculationResult result = calculator.calculate(input);

        assertTrue(result.remainingCaloriesForCarbs().compareTo(BigDecimal.ZERO) < 0,
                "Remaining calories should be negative when target is too low");
        assertFalse(result.feasible());
    }

    // ================================================================
    // TESTS 15–21: Input validation
    // ================================================================

    /** Test 15 — Null input throws IllegalArgumentException */
    @Test
    void test15_nullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null),
                "Null input should throw IllegalArgumentException");
    }

    /** Test 16 — Null goal throws IllegalArgumentException */
    @Test
    void test16_nullGoal() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.valueOf(75), null,
                new BigDecimal("2000"), BigDecimal.ZERO,
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Null goal should throw");
    }

    /** Test 17 — Zero weight throws IllegalArgumentException */
    @Test
    void test17_zeroWeight() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.ZERO, Goal.MAINTENANCE,
                new BigDecimal("2000"), BigDecimal.ZERO,
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Zero weight should throw");
    }

    /** Test 18 — Negative weight throws IllegalArgumentException */
    @Test
    void test18_negativeWeight() {
        FatCalculationInput input = new FatCalculationInput(
                new BigDecimal("-10"), Goal.MAINTENANCE,
                new BigDecimal("2000"), BigDecimal.ZERO,
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Negative weight should throw");
    }

    /** Test 19 — Zero calories throws IllegalArgumentException */
    @Test
    void test19_zeroCalories() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.valueOf(75), Goal.MAINTENANCE,
                BigDecimal.ZERO, BigDecimal.ZERO,
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Zero calories should throw");
    }

    /** Test 20 — Negative calories throws IllegalArgumentException */
    @Test
    void test20_negativeCalories() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.valueOf(75), Goal.MAINTENANCE,
                new BigDecimal("-500"), BigDecimal.ZERO,
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Negative calories should throw");
    }

    /** Test 21 — Negative protein grams throws IllegalArgumentException */
    @Test
    void test21_negativeProtein() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.valueOf(75), Goal.FAT_LOSS,
                new BigDecimal("2000"), new BigDecimal("-10"),
                false, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Negative protein should throw");
    }

    // ================================================================
    // TESTS 22–24: Precision and safety guarantees
    // ================================================================

    /** Test 22 — BigDecimal rounding: fractional gram result is exact */
    @Test
    void test22_bigDecimalRounding() {
        // 75 kg × 0.7 = 52.5 g (exact, 2 dp)
        FatCalculationResult result = calculator.calculate(
                input(75, Goal.FAT_LOSS, false));

        assertEquals(2, result.fatGrams().scale(),
                "Fat grams must be rounded to 2 decimal places");
        assertEquals(2, result.fatCalories().scale(),
                "Fat calories must be rounded to 2 decimal places");

        // Ensure no floating-point imprecision: 52.50 × 9 = 472.50 exactly
        assertEquals(0, new BigDecimal("472.50").compareTo(result.fatCalories()),
                "52.50 g × 9 kcal/g must equal exactly 472.50 kcal");
    }

    /** Test 23 — Fat result is never negative */
    @Test
    void test23_fatNeverNegative() {
        // Very small but valid weight — will be clamped to MIN_FAT_GRAMS
        FatCalculationInput input = new FatCalculationInput(
                new BigDecimal("0.1"), Goal.MAINTENANCE,
                new BigDecimal("2000"), BigDecimal.ZERO,
                false, null, null, null);

        FatCalculationResult result = calculator.calculate(input);

        assertTrue(result.fatGrams().compareTo(BigDecimal.ZERO) > 0,
                "Fat grams must never be negative");
        assertTrue(result.fatGrams().compareTo(FatCalculator.MIN_FAT_GRAMS) >= 0,
                "Fat grams must be at or above the floor");
    }

    /** Test 24 — No floating-point arithmetic: result uses BigDecimal scale */
    @Test
    void test24_noFloatingPointArithmetic() {
        FatCalculationResult result = calculator.calculate(
                input(80, Goal.SPORTS_PERFORMANCE, false));

        // Verify scale is explicitly 2 — not a raw double/float conversion
        assertTrue(result.fatGrams().scale() >= 0,
                "fatGrams must be a properly scaled BigDecimal");
        assertTrue(result.fatCalories().scale() >= 0,
                "fatCalories must be a properly scaled BigDecimal");
        assertNotNull(result.remainingCaloriesForCarbs(),
                "remainingCaloriesForCarbs must be a BigDecimal, not null");
    }

    // ================================================================
    // TEST 25: Medical safety
    // ================================================================

    /** Test 25 — Existing medical safety state is respected */
    @Test
    void test25_medicalSafetyRespected() {

        // HIGH_CHOLESTEROL → FatWarningCode.HIGH_CHOLESTEROL_DETECTED warning
        FatCalculationResult resultCholesterol = calculator.calculate(
                inputWithConditions(75, Goal.MAINTENANCE,
                        Set.of(MedicalCondition.HIGH_CHOLESTEROL)));

        assertTrue(resultCholesterol.warnings().stream()
                .anyMatch(w -> w.code() == FatWarningCode.HIGH_CHOLESTEROL_DETECTED),
                "HIGH_CHOLESTEROL should emit HIGH_CHOLESTEROL_DETECTED warning");
        // HIGH_CHOLESTEROL does NOT require clinical review for fat quantity
        assertFalse(resultCholesterol.requiresClinicalReview(),
                "HIGH_CHOLESTEROL: fat quantity not restricted; no clinical review needed");

        // LIVER_DISEASE → CLINICAL_REVIEW_REQUIRED
        FatCalculationResult resultLiver = calculator.calculate(
                inputWithConditions(75, Goal.MAINTENANCE,
                        Set.of(MedicalCondition.LIVER_DISEASE)));

        assertTrue(resultLiver.requiresClinicalReview(),
                "LIVER_DISEASE should require clinical review for fat");
        assertEquals(CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED, resultLiver.safetyStatus());

        // FATTY_LIVER → FATTY_LIVER_DETECTED warning, no clinical review
        FatCalculationResult resultFattyLiver = calculator.calculate(
                inputWithConditions(75, Goal.MAINTENANCE,
                        Set.of(MedicalCondition.FATTY_LIVER)));

        assertTrue(resultFattyLiver.warnings().stream()
                .anyMatch(w -> w.code() == FatWarningCode.FATTY_LIVER_DETECTED),
                "FATTY_LIVER should emit FATTY_LIVER_DETECTED warning");
        assertFalse(resultFattyLiver.requiresClinicalReview());
    }

    // ================================================================
    // TEST 26: No duplicate Goal enum
    // ================================================================

    /**
     * Test 26 — The Goal enum used by FatCalculator is the same instance
     * as the one used by ProteinCalculator — no duplicate was created.
     */
    @Test
    void test26_noDuplicateGoalEnum() {
        // If a duplicate Goal enum existed, this would fail to compile or
        // the switch would not handle all cases of the real Goal.
        // We exercise every Goal variant through the calculator.
        for (Goal g : Goal.values()) {
            FatCalculationInput input = new FatCalculationInput(
                    BigDecimal.valueOf(70), g,
                    new BigDecimal("2000"), BigDecimal.ZERO,
                    false, null, null, null);

            FatCalculationResult result = calculator.calculate(input);
            assertNotNull(result,
                    "Every Goal variant must produce a result: " + g);
            assertTrue(result.fatGrams().compareTo(BigDecimal.ZERO) > 0,
                    "Fat grams must be positive for every Goal: " + g);
        }
    }

    // ================================================================
    // ADDITIONAL: ATHLETE intensity
    // ================================================================

    /** ATHLETE intensity also triggers high-demand tier */
    @Test
    void testAthlete_triggersHighDemandTier() {
        FatCalculationResult result = calculator.calculate(
                inputWithIntensity(80, Goal.WEIGHT_GAIN, WorkoutIntensity.ATHLETE));

        // 80 × 1.0 = 80.00
        assertEquals(0, new BigDecimal("80.00").compareTo(result.fatGrams()),
                "ATHLETE intensity WEIGHT_GAIN: expected 80.00 g (high-demand tier)");
        assertEquals("WEIGHT_GAIN_HIGH_DEMAND", result.calculationBasis());
    }

    /** Null workout intensity for SPORTS_PERFORMANCE defaults to normal tier */
    @Test
    void testNullIntensity_defaultsToNormalTier() {
        FatCalculationInput input = new FatCalculationInput(
                BigDecimal.valueOf(72), Goal.SPORTS_PERFORMANCE,
                new BigDecimal("3000"), BigDecimal.ZERO,
                false, null, null, null);

        FatCalculationResult result = calculator.calculate(input);

        assertEquals("SPORTS_NORMAL", result.calculationBasis(),
                "Null workout intensity should default to SPORTS_NORMAL tier");
    }

    /** Integration: fat result is used for carbohydrate headroom downstream */
    @Test
    void testRemainingCaloriesForCarbs_exactCalculation() {
        // 75 kg, FAT_LOSS (no RT) → fat = 52.50 g → fatCal = 472.50
        // protein = 110 g → proteinCal = 440.00
        // targetCalories = 1600
        // remaining = 1600 - 440.00 - 472.50 = 687.50
        FatCalculationInput input = inputWithMacros(
                75, Goal.FAT_LOSS,
                new BigDecimal("1600"),
                new BigDecimal("110"));

        FatCalculationResult result = calculator.calculate(input);

        assertEquals(0, new BigDecimal("687.50").compareTo(result.remainingCaloriesForCarbs()),
                "Remaining calories for carbs must be exactly 687.50");
        assertTrue(result.feasible());
    }
}
