package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.WaterCalculationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.WaterWarningCode;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WaterCalculator}.
 *
 * <p>Covers all 17 scenarios specified in the requirements plus additional
 * edge-case and rounding verification.</p>
 */
class WaterCalculatorTest {

    private final WaterCalculator calculator = new WaterCalculator();

    // ================================================================
    // Helper builders — uses ActivityProfile.builder() (Lombok)
    // ================================================================

    /**
     * Builds a minimal valid ActivityProfile without a parent WorkoutProfile.
     * The parent association is not needed by WaterCalculator.
     */
    private ActivityProfile activity(
            WorkoutIntensity intensity,
            int durationMinutes,
            int daysPerWeek) {

        return ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .experienceLevel(ExperienceLevel.ONE_TO_TWO_YEARS)
                .intensity(intensity)
                .durationMinutes(durationMinutes)
                .daysPerWeek(daysPerWeek)
                .build();
    }

    private WaterCalculationInput input(double weightKg, Set<ActivityProfile> activities) {
        return new WaterCalculationInput(BigDecimal.valueOf(weightKg), activities);
    }

    private WaterCalculationInput inputNoActivities(double weightKg) {
        return new WaterCalculationInput(BigDecimal.valueOf(weightKg), null);
    }

    // ================================================================
    // TEST 1: 70 kg, no activities → baseline only
    // ================================================================

    /**
     * Test 1 — No activities: baseline = 70 × 35 = 2450 ml, exercise = 0, final = 2450 ml.
     */
    @Test
    void test01_noActivities_baselineOnly() {
        WaterCalculationResult result = calculator.calculate(inputNoActivities(70));

        assertNotNull(result);
        // 70 × 35 = 2450 ml (exact, rounds to 2450)
        assertEquals(0, new BigDecimal("2450").compareTo(result.waterMl()),
                "Expected 2450 ml, got " + result.waterMl());
        assertEquals(0, new BigDecimal("2450").compareTo(result.baselineWaterMl()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.exerciseWaterMl()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.averageDailyExerciseMinutes()));
        assertTrue(result.feasible());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.NO_ACTIVITIES),
                "Should emit NO_ACTIVITIES warning when no activities provided");
    }

    // ================================================================
    // TEST 2: LOW intensity
    // ================================================================

    /**
     * Test 2 — LOW intensity: 60 min × 5 days × 4 ml/min = 1200 ml/week → 171.428... /day.
     * baseline = 70 × 35 = 2450
     * exercise = 1200 / 7 ≈ 171.428…
     * total ≈ 2621.428 → rounds to 2620
     */
    @Test
    void test02_lowIntensity() {
        ActivityProfile act = activity(WorkoutIntensity.LOW, 60, 5);
        WaterCalculationResult result = calculator.calculate(input(70, Set.of(act)));

        // 60 × 5 × 4 = 1200 ml/week; 1200/7 = 171.428571... ml/day
        // baseline 2450 + 171.428... = 2621.428... → nearest 10 = 2620
        assertEquals(0, new BigDecimal("2620").compareTo(result.waterMl()),
                "LOW 60min×5days: expected 2620 ml");
        assertEquals(0, new BigDecimal("2450").compareTo(result.baselineWaterMl()));
        assertTrue(result.warnings().isEmpty(),
                "No warnings expected for a valid single activity");
    }

    // ================================================================
    // TEST 3: MODERATE uses 6 ml/min
    // ================================================================

    /**
     * Test 3 — MODERATE intensity: verify rate is 6 ml/min.
     * 60 min × 4 days × 6 = 1440 ml/week → 1440/7 ≈ 205.714 ml/day
     * baseline 70 kg → 2450
     * total ≈ 2655.714 → 2660
     */
    @Test
    void test03_moderateIntensity_rate6() {
        ActivityProfile act = activity(WorkoutIntensity.MODERATE, 60, 4);
        WaterCalculationResult result = calculator.calculate(input(70, Set.of(act)));

        // 60 × 4 × 6 = 1440 / 7 = 205.714… → total 2655.714… → 2660
        assertEquals(0, new BigDecimal("2660").compareTo(result.waterMl()),
                "MODERATE 60min×4days: expected 2660 ml");
        assertEquals(0, WaterCalculator.RATE_MODERATE.compareTo(
                        new BigDecimal("6")),
                "MODERATE rate must be 6 ml/min");
    }

    // ================================================================
    // TEST 4: HIGH uses 8 ml/min
    // ================================================================

    /**
     * Test 4 — HIGH intensity: verify rate is 8 ml/min.
     * 45 min × 3 days × 8 = 1080 ml/week → 1080/7 ≈ 154.285 ml/day
     * baseline 70 kg → 2450
     * total ≈ 2604.285 → 2600
     */
    @Test
    void test04_highIntensity_rate8() {
        ActivityProfile act = activity(WorkoutIntensity.HIGH, 45, 3);
        WaterCalculationResult result = calculator.calculate(input(70, Set.of(act)));

        // 45 × 3 × 8 = 1080 / 7 = 154.2857… → total 2604.285… → 2600
        assertEquals(0, new BigDecimal("2600").compareTo(result.waterMl()),
                "HIGH 45min×3days: expected 2600 ml");
        assertEquals(0, WaterCalculator.RATE_HIGH.compareTo(new BigDecimal("8")),
                "HIGH rate must be 8 ml/min");
    }

    // ================================================================
    // TEST 5: ATHLETE uses 10 ml/min
    // ================================================================

    /**
     * Test 5 — ATHLETE intensity: verify rate is 10 ml/min.
     * 90 min × 6 days × 10 = 5400 ml/week → 5400/7 ≈ 771.428 ml/day
     * baseline 70 kg → 2450
     * total ≈ 3221.428 → 3220
     */
    @Test
    void test05_athleteIntensity_rate10() {
        ActivityProfile act = activity(WorkoutIntensity.ATHLETE, 90, 6);
        WaterCalculationResult result = calculator.calculate(input(70, Set.of(act)));

        // 90 × 6 × 10 = 5400 / 7 = 771.4285… → total 3221.428… → 3220
        assertEquals(0, new BigDecimal("3220").compareTo(result.waterMl()),
                "ATHLETE 90min×6days: expected 3220 ml");
        assertEquals(0, WaterCalculator.RATE_ATHLETE.compareTo(new BigDecimal("10")),
                "ATHLETE rate must be 10 ml/min");
    }

    // ================================================================
    // TEST 6: Multiple activities are aggregated (spec example)
    // ================================================================

    /**
     * Test 6 — Multiple activities: spec example from requirements.
     * Weight training: 60 min × 5 days × 6 ml/min = 1800 ml/week
     * Running:         30 min × 3 days × 8 ml/min =  720 ml/week
     * Total weekly: 2520 ml → daily: 2520/7 = 360 ml
     * Baseline: 70 × 35 = 2450
     * Total: 2450 + 360 = 2810 → rounds to 2810
     */
    @Test
    void test06_multipleActivities_aggregated() {
        ActivityProfile weightTraining = activity(WorkoutIntensity.MODERATE, 60, 5);
        ActivityProfile running        = activity(WorkoutIntensity.HIGH, 30, 3);

        WaterCalculationResult result = calculator.calculate(
                input(70, Set.of(weightTraining, running)));

        // exercise = (1800 + 720) / 7 = 2520 / 7 = 360 exactly
        BigDecimal expectedExercise = new BigDecimal("360.000000");
        // total = 2450 + 360 = 2810 → rounds to 2810
        assertEquals(0, new BigDecimal("2810").compareTo(result.waterMl()),
                "Spec example: expected 2810 ml");
        assertEquals(0, new BigDecimal("2450").compareTo(result.baselineWaterMl()));
        // exerciseWaterMl ≈ 360 (within a tiny epsilon due to scale)
        assertTrue(result.exerciseWaterMl().compareTo(new BigDecimal("359.999")) > 0
                && result.exerciseWaterMl().compareTo(new BigDecimal("360.001")) < 0,
                "Exercise water ml should be ~360, got " + result.exerciseWaterMl());
    }

    // ================================================================
    // TEST 7: Fractional daily exercise volume (BigDecimal precision)
    // ================================================================

    /**
     * Test 7 — Fractional daily volume handled with BigDecimal precision.
     * 60 min × 5 days = 300 min/week → 300/7 = 42.857142... min/day (non-terminating)
     */
    @Test
    void test07_fractionalDailyVolume_bigDecimal() {
        ActivityProfile act = activity(WorkoutIntensity.LOW, 60, 5);
        WaterCalculationResult result = calculator.calculate(input(70, Set.of(act)));

        // averageDailyExerciseMinutes = 300/7 = 42.857142…
        assertNotNull(result.averageDailyExerciseMinutes());
        // Must be in range (42, 43) — confirming non-integer result is handled
        assertTrue(result.averageDailyExerciseMinutes().compareTo(new BigDecimal("42")) > 0
                && result.averageDailyExerciseMinutes().compareTo(new BigDecimal("43")) < 0,
                "Fractional daily minutes should be ~42.857, got "
                + result.averageDailyExerciseMinutes());
    }

    // ================================================================
    // TEST 8: Final water rounded to nearest 10 ml
    // ================================================================

    /**
     * Test 8 — Rounding to nearest 10 ml.
     * Tests the roundToNearest10 helper directly for accuracy.
     */
    @Test
    void test08_roundingToNearest10ml() {
        // 2806 → 2810
        assertEquals(0, new BigDecimal("2810")
                .compareTo(WaterCalculator.roundToNearest10(new BigDecimal("2806"))),
                "2806 should round to 2810");
        // 2814 → 2810
        assertEquals(0, new BigDecimal("2810")
                .compareTo(WaterCalculator.roundToNearest10(new BigDecimal("2814"))),
                "2814 should round to 2810");
        // 2815 → 2820 (HALF_UP)
        assertEquals(0, new BigDecimal("2820")
                .compareTo(WaterCalculator.roundToNearest10(new BigDecimal("2815"))),
                "2815 should round to 2820 (HALF_UP)");
        // 2816 → 2820
        assertEquals(0, new BigDecimal("2820")
                .compareTo(WaterCalculator.roundToNearest10(new BigDecimal("2816"))),
                "2816 should round to 2820");
        // Exact multiple: 2450 → 2450
        assertEquals(0, new BigDecimal("2450")
                .compareTo(WaterCalculator.roundToNearest10(new BigDecimal("2450"))),
                "Exact multiple 2450 should stay 2450");
    }

    // ================================================================
    // TEST 9: Null input
    // ================================================================

    /** Test 9 — Null input throws IllegalArgumentException. */
    @Test
    void test09_nullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null),
                "Null input must throw");
    }

    // ================================================================
    // TEST 10: Null weight
    // ================================================================

    /** Test 10 — Null weight throws IllegalArgumentException. */
    @Test
    void test10_nullWeight() {
        WaterCalculationInput input = new WaterCalculationInput(null, null);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Null weight must throw");
    }

    // ================================================================
    // TEST 11: Zero weight
    // ================================================================

    /** Test 11 — Zero weight throws IllegalArgumentException. */
    @Test
    void test11_zeroWeight() {
        WaterCalculationInput input = new WaterCalculationInput(BigDecimal.ZERO, null);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Zero weight must throw");
    }

    // ================================================================
    // TEST 12: Negative weight
    // ================================================================

    /** Test 12 — Negative weight throws IllegalArgumentException. */
    @Test
    void test12_negativeWeight() {
        WaterCalculationInput input = new WaterCalculationInput(
                new BigDecimal("-10"), null);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Negative weight must throw");
    }

    // ================================================================
    // TEST 13: Invalid activity duration
    // ================================================================

    /**
     * Test 13 — Invalid activity duration (0 minutes) is rejected with a warning.
     * The invalid activity is skipped; result still contains baseline-only water.
     */
    @Test
    void test13_invalidActivityDuration() {
        ActivityProfile badActivity = ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .experienceLevel(ExperienceLevel.ONE_TO_TWO_YEARS)
                .intensity(WorkoutIntensity.MODERATE)
                .durationMinutes(0)     // ← invalid
                .daysPerWeek(3)
                .build();

        WaterCalculationResult result = calculator.calculate(
                input(70, Set.of(badActivity)));

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.INVALID_ACTIVITY_DATA),
                "Should emit INVALID_ACTIVITY_DATA for zero duration");

        // Invalid activity was skipped → exercise water = 0 → baseline only = 2450
        assertEquals(0, new BigDecimal("2450").compareTo(result.waterMl()),
                "Invalid activity skipped → baseline only → 2450 ml");
    }

    // ================================================================
    // TEST 14: Invalid daysPerWeek
    // ================================================================

    /**
     * Test 14 — Invalid daysPerWeek (0 days) is rejected with a warning.
     */
    @Test
    void test14_invalidDaysPerWeek() {
        ActivityProfile badActivity = ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .experienceLevel(ExperienceLevel.ONE_TO_TWO_YEARS)
                .intensity(WorkoutIntensity.HIGH)
                .durationMinutes(60)
                .daysPerWeek(0)         // ← invalid
                .build();

        WaterCalculationResult result = calculator.calculate(
                input(70, Set.of(badActivity)));

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.INVALID_ACTIVITY_DATA),
                "Should emit INVALID_ACTIVITY_DATA for zero daysPerWeek");

        assertEquals(0, new BigDecimal("2450").compareTo(result.waterMl()),
                "Invalid activity skipped → baseline only → 2450 ml");
    }

    // ================================================================
    // TEST 15: Null intensity
    // ================================================================

    /**
     * Test 15 — Activity with null intensity is rejected with a warning.
     */
    @Test
    void test15_nullIntensity() {
        ActivityProfile badActivity = ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .experienceLevel(ExperienceLevel.ONE_TO_TWO_YEARS)
                .intensity(null)        // ← invalid
                .durationMinutes(60)
                .daysPerWeek(4)
                .build();

        WaterCalculationResult result = calculator.calculate(
                input(70, Set.of(badActivity)));

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.INVALID_ACTIVITY_DATA),
                "Should emit INVALID_ACTIVITY_DATA for null intensity");

        assertEquals(0, new BigDecimal("2450").compareTo(result.waterMl()),
                "Invalid activity skipped → baseline only → 2450 ml");
    }

    // ================================================================
    // TEST 16: Empty activity set → baseline only
    // ================================================================

    /**
     * Test 16 — Empty activities set produces baseline-only hydration.
     */
    @Test
    void test16_emptyActivities_baselineOnly() {
        WaterCalculationInput input = new WaterCalculationInput(
                BigDecimal.valueOf(70), Set.of());

        WaterCalculationResult result = calculator.calculate(input);

        assertEquals(0, new BigDecimal("2450").compareTo(result.waterMl()),
                "Empty activities → baseline only → 2450 ml");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.exerciseWaterMl()));
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.NO_ACTIVITIES),
                "Should emit NO_ACTIVITIES for empty set");
    }

    // ================================================================
    // TEST 17: No floating-point arithmetic
    // ================================================================

    /**
     * Test 17 — All results are BigDecimal with proper scale; no float/double.
     */
    @Test
    void test17_noFloatingPoint() {
        ActivityProfile act = activity(WorkoutIntensity.MODERATE, 45, 4);
        WaterCalculationResult result = calculator.calculate(input(75, Set.of(act)));

        assertNotNull(result.waterMl());
        assertNotNull(result.baselineWaterMl());
        assertNotNull(result.exerciseWaterMl());
        assertNotNull(result.averageDailyExerciseMinutes());

        // waterMl is rounded to nearest 10 → scale 0 (e.g. 2770)
        assertEquals(0, result.waterMl().scale(),
                "Final waterMl must have scale 0 (multiple of 10)");

        // Baseline: 75 × 35 = 2625.0 — integer multiplication
        assertEquals(0, new BigDecimal("2625").compareTo(result.baselineWaterMl()));
    }

    // ================================================================
    // Additional precision test: exact spec example values
    // ================================================================

    /**
     * Additional — verify baseline constant WATER_ML_PER_KG = 35.
     */
    @Test
    void testBaselineConstant_35mlPerKg() {
        assertEquals(0, new BigDecimal("35").compareTo(WaterCalculator.WATER_ML_PER_KG),
                "WATER_ML_PER_KG must be 35");
    }

    /**
     * Additional — different body weight: 80 kg with no activities → 80 × 35 = 2800.
     */
    @Test
    void test_80kg_noActivities() {
        WaterCalculationResult result = calculator.calculate(inputNoActivities(80));

        assertEquals(0, new BigDecimal("2800").compareTo(result.waterMl()),
                "80 kg, no activities → 80 × 35 = 2800 ml");
    }

    /**
     * Additional — mixed valid and invalid activities: valid one contributes,
     * invalid one is skipped.
     */
    @Test
    void test_mixedValidAndInvalid_validContributes() {
        ActivityProfile valid   = activity(WorkoutIntensity.MODERATE, 60, 4);
        ActivityProfile invalid = ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .experienceLevel(ExperienceLevel.LESS_THAN_A_YEAR)
                .intensity(null)
                .durationMinutes(60)
                .daysPerWeek(3)
                .build();

        WaterCalculationResult result = calculator.calculate(
                input(70, Set.of(valid, invalid)));

        // Valid: 60 × 4 × 6 = 1440 ml/week → 1440/7 ≈ 205.714 ml/day
        // baseline 2450 + 205.714 = 2655.714 → 2660
        assertEquals(0, new BigDecimal("2660").compareTo(result.waterMl()),
                "Mixed activities: valid contributes, invalid skipped → 2660 ml");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.INVALID_ACTIVITY_DATA),
                "INVALID_ACTIVITY_DATA warning expected");
        // No NO_ACTIVITIES warning because the valid activity was present
        assertFalse(result.warnings().stream()
                .anyMatch(w -> w.code() == WaterWarningCode.NO_ACTIVITIES),
                "NO_ACTIVITIES should not appear when at least one valid activity exists");
    }
}
