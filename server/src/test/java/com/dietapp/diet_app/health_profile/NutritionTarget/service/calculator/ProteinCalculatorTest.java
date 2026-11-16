package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalorieSafetyStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProteinCalculator}.
 *
 * <p>Covers all 30 scenarios specified in the implementation requirements,
 * including goal-based multipliers, LBM fallback, medical conditions,
 * safety clamping, and bioavailability guarantees.</p>
 */
class ProteinCalculatorTest {

    private final ProteinCalculator calculator = new ProteinCalculator();

    // ================================================================
    // Helper builders
    // ================================================================

    private ProteinCalculationInput input(
            double weightKg,
            Goal goal,
            boolean resistanceTraining) {

        return new ProteinCalculationInput(
                BigDecimal.valueOf(weightKg),
                null,
                goal,
                resistanceTraining,
                null,
                null,
                null,
                null
        );
    }

    private ProteinCalculationInput inputWithCurrentProtein(
            double weightKg,
            Goal goal,
            boolean resistanceTraining,
            Double currentProteinGrams) {

        return new ProteinCalculationInput(
                BigDecimal.valueOf(weightKg),
                null,
                goal,
                resistanceTraining,
                null,
                null,
                currentProteinGrams != null
                        ? BigDecimal.valueOf(currentProteinGrams)
                        : null,
                null
        );
    }

    private ProteinCalculationInput inputWithBodyFat(
            double weightKg,
            Goal goal,
            boolean resistanceTraining,
            Double bodyFatPct) {

        return new ProteinCalculationInput(
                BigDecimal.valueOf(weightKg),
                bodyFatPct != null ? BigDecimal.valueOf(bodyFatPct) : null,
                goal,
                resistanceTraining,
                null,
                null,
                null,
                null
        );
    }

    private ProteinCalculationInput inputWithConditions(
            double weightKg,
            Goal goal,
            boolean resistanceTraining,
            Set<MedicalCondition> conditions) {

        return new ProteinCalculationInput(
                BigDecimal.valueOf(weightKg),
                null,
                goal,
                resistanceTraining,
                null,
                null,
                null,
                conditions
        );
    }

    // ================================================================
    // TESTS 1–9: Goal-based multipliers
    // ================================================================

    /** Test 1 — General health uses 1.2 g/kg */
    @Test
    void test01_generalHealth() {
        ProteinTargetResult result = calculator.calculate(
                input(70, Goal.GENERAL_HEALTH, false));

        assertNotNull(result);
        // 70 × 1.2 = 84
        assertEquals(0, new BigDecimal("84.00").compareTo(result.longTermTargetGrams()),
                "GENERAL_HEALTH: expected 84.00 g, got " + result.longTermTargetGrams());
        assertEquals("BODY_WEIGHT", result.calculationBasis());
        assertEquals(0, ProteinCalculator.MULTIPLIER_GENERAL_HEALTH.compareTo(result.proteinPerKg()));
        assertEquals(CalorieSafetyStatus.SAFE, result.safetyStatus());
        assertFalse(result.requiresClinicalReview());
    }

    /** Test 2 — Maintenance uses same multiplier as general health */
    @Test
    void test02_maintenance() {
        ProteinTargetResult result = calculator.calculate(
                input(70, Goal.MAINTENANCE, false));

        // 70 × 1.2 = 84
        assertEquals(0, new BigDecimal("84.00").compareTo(result.longTermTargetGrams()));
        assertEquals(0, ProteinCalculator.MULTIPLIER_GENERAL_HEALTH.compareTo(result.proteinPerKg()));
    }

    /** Test 3 — Fat loss without resistance training: 1.6 g/kg */
    @Test
    void test03_fatLossNoResistanceTraining() {
        ProteinTargetResult result = calculator.calculate(
                input(75, Goal.FAT_LOSS, false));

        // 75 × 1.6 = 120
        assertEquals(0, new BigDecimal("120.00").compareTo(result.longTermTargetGrams()),
                "FAT_LOSS no RT: expected 120.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_FAT_LOSS_NO_RT.compareTo(result.proteinPerKg()));
    }

    /** Test 4 — Fat loss with resistance training: 1.8 g/kg */
    @Test
    void test04_fatLossWithResistanceTraining() {
        ProteinTargetResult result = calculator.calculate(
                input(75, Goal.FAT_LOSS, true));

        // 75 × 1.8 = 135
        assertEquals(0, new BigDecimal("135.00").compareTo(result.longTermTargetGrams()),
                "FAT_LOSS with RT: expected 135.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_FAT_LOSS_RT.compareTo(result.proteinPerKg()));
    }

    /** Test 5 — Muscle gain: 1.8 g/kg */
    @Test
    void test05_muscleGain() {
        ProteinTargetResult result = calculator.calculate(
                input(80, Goal.MUSCLE_GAIN, false));

        // 80 × 1.8 = 144
        assertEquals(0, new BigDecimal("144.00").compareTo(result.longTermTargetGrams()),
                "MUSCLE_GAIN: expected 144.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_MUSCLE_GAIN.compareTo(result.proteinPerKg()));
    }

    /** Test 6 — Body recomposition: 2.0 g/kg */
    @Test
    void test06_bodyRecomposition() {
        ProteinTargetResult result = calculator.calculate(
                input(70, Goal.BODY_RECOMPOSITION, true));

        // 70 × 2.0 = 140
        assertEquals(0, new BigDecimal("140.00").compareTo(result.longTermTargetGrams()),
                "BODY_RECOMPOSITION: expected 140.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_BODY_RECOMPOSITION.compareTo(result.proteinPerKg()));
    }

    /** Test 7 — Weight gain without resistance training: 1.4 g/kg */
    @Test
    void test07_weightGainNoResistanceTraining() {
        ProteinTargetResult result = calculator.calculate(
                input(65, Goal.WEIGHT_GAIN, false));

        // 65 × 1.4 = 91
        assertEquals(0, new BigDecimal("91.00").compareTo(result.longTermTargetGrams()),
                "WEIGHT_GAIN no RT: expected 91.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_WEIGHT_GAIN_NO_RT.compareTo(result.proteinPerKg()));
    }

    /** Test 8 — Weight gain with resistance training: 1.8 g/kg */
    @Test
    void test08_weightGainWithResistanceTraining() {
        ProteinTargetResult result = calculator.calculate(
                input(65, Goal.WEIGHT_GAIN, true));

        // 65 × 1.8 = 117
        assertEquals(0, new BigDecimal("117.00").compareTo(result.longTermTargetGrams()),
                "WEIGHT_GAIN with RT: expected 117.00 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_WEIGHT_GAIN_RT.compareTo(result.proteinPerKg()));
    }

    /** Test 9 — Sports performance: 1.8 g/kg */
    @Test
    void test09_sportsPerformance() {
        ProteinTargetResult result = calculator.calculate(
                input(72, Goal.SPORTS_PERFORMANCE, false));

        // 72 × 1.8 = 129.6
        assertEquals(0, new BigDecimal("129.60").compareTo(result.longTermTargetGrams()),
                "SPORTS_PERFORMANCE: expected 129.60 g");
        assertEquals(0, ProteinCalculator.MULTIPLIER_SPORTS_PERFORMANCE.compareTo(result.proteinPerKg()));
    }

    // ================================================================
    // TESTS 10–14: Current protein / adaptation edge cases
    // ================================================================

    /** Test 10 — Missing current protein is accepted (no exception thrown) */
    @Test
    void test10_missingCurrentProtein() {
        ProteinCalculationInput input = inputWithCurrentProtein(
                75, Goal.FAT_LOSS, true, null);

        assertDoesNotThrow(() -> calculator.calculate(input));
        ProteinTargetResult result = calculator.calculate(input);
        assertNotNull(result);
        assertTrue(result.longTermTargetGrams().compareTo(BigDecimal.ZERO) > 0);
    }

    /** Test 11 — Current protein already above long-term target: calculator still works */
    @Test
    void test11_currentProteinAboveTarget() {
        // Weight = 60 kg, MAINTENANCE → target = 72 g
        // currentProtein = 100 g (above target)
        ProteinCalculationInput input = inputWithCurrentProtein(
                60, Goal.MAINTENANCE, false, 100.0);

        ProteinTargetResult result = calculator.calculate(input);
        // 60 × 1.2 = 72 — calculator produces target regardless of current intake
        assertEquals(0, new BigDecimal("72.00").compareTo(result.longTermTargetGrams()));
    }

    /** Test 12 — Small gap (≤ 15 g): starting target should equal long-term */
    @Test
    void test12_smallGap() {
        // FAT_LOSS + RT → 75 × 1.8 = 135
        // currentProtein = 125 g → gap = 10 g ≤ 15 → starting = 135
        BigDecimal startingTarget = ProteinAdaptationService.calculateStartingTarget(
                new BigDecimal("125"), new BigDecimal("135"));

        assertEquals(0, new BigDecimal("135").compareTo(startingTarget),
                "Small gap (10 g): starting target should equal long-term target");
    }

    /** Test 13 — Medium gap (> 15 g and ≤ 30 g): starting target = longTerm - 10 */
    @Test
    void test13_mediumGap() {
        // currentProtein = 110 g, longTerm = 135 → gap = 25 g
        BigDecimal startingTarget = ProteinAdaptationService.calculateStartingTarget(
                new BigDecimal("110"), new BigDecimal("135"));

        assertEquals(0, new BigDecimal("125").compareTo(startingTarget),
                "Medium gap (25 g): starting target should be 125 g");
    }

    /** Test 14 — Large gap (> 50 g): starting target = longTerm - 25 */
    @Test
    void test14_largeGap() {
        // currentProtein = 70 g, longTerm = 135 → gap = 65 g
        BigDecimal startingTarget = ProteinAdaptationService.calculateStartingTarget(
                new BigDecimal("70"), new BigDecimal("135"));

        assertEquals(0, new BigDecimal("110").compareTo(startingTarget),
                "Large gap (65 g): starting target should be 110 g");
    }

    // ================================================================
    // TESTS 15–17: Body-fat percentage / LBM
    // ================================================================

    /** Test 15 — Valid body-fat % uses LBM as calculation basis */
    @Test
    void test15_bodyFatAvailable_usesLbm() {
        // weight = 80 kg, BF = 25% → LBM = 60 kg
        // MUSCLE_GAIN: 60 × 1.8 = 108
        ProteinCalculationInput input = inputWithBodyFat(
                80, Goal.MUSCLE_GAIN, true, 25.0);

        ProteinTargetResult result = calculator.calculate(input);

        assertEquals("LBM", result.calculationBasis());
        assertEquals(0, new BigDecimal("60.00").compareTo(result.weightUsedKg()));
        assertEquals(0, new BigDecimal("108.00").compareTo(result.longTermTargetGrams()),
                "LBM-based MUSCLE_GAIN: expected 108 g");
    }

    /** Test 16 — Body-fat % missing: falls back to total body weight */
    @Test
    void test16_bodyFatMissing_usesBodyWeight() {
        ProteinCalculationInput input = inputWithBodyFat(
                80, Goal.MUSCLE_GAIN, true, null);

        ProteinTargetResult result = calculator.calculate(input);

        assertEquals("BODY_WEIGHT", result.calculationBasis());
        assertEquals(0, new BigDecimal("80.00").compareTo(result.weightUsedKg()));
        // 80 × 1.8 = 144
        assertEquals(0, new BigDecimal("144.00").compareTo(result.longTermTargetGrams()));
    }

    /** Test 17 — Invalid body-fat % (> 60) is ignored; warning is emitted; falls back to weight */
    @Test
    void test17_invalidBodyFat_isIgnored() {
        ProteinCalculationInput input = inputWithBodyFat(
                80, Goal.MUSCLE_GAIN, true, 75.0);   // 75% is invalid

        ProteinTargetResult result = calculator.calculate(input);

        assertEquals("BODY_WEIGHT", result.calculationBasis(),
                "Invalid body-fat % should be ignored; should fall back to BODY_WEIGHT");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.BODY_FAT_INVALID_IGNORED),
                "Should emit BODY_FAT_INVALID_IGNORED warning");
    }

    // ================================================================
    // TESTS 18–19: Input validation
    // ================================================================

    /** Test 18 — Zero weight throws IllegalArgumentException */
    @Test
    void test18_invalidWeight_zero() {
        ProteinCalculationInput input = new ProteinCalculationInput(
                BigDecimal.ZERO, null, Goal.FAT_LOSS, false,
                null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Weight of 0 should throw IllegalArgumentException");
    }

    /** Test 19 — Null goal throws IllegalArgumentException */
    @Test
    void test19_nullGoal() {
        ProteinCalculationInput input = new ProteinCalculationInput(
                BigDecimal.valueOf(75), null, null, false,
                null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(input),
                "Null goal should throw IllegalArgumentException");
    }

    // ================================================================
    // TESTS 20–23: Adaptation progression (ProteinAdaptationService)
    // ================================================================

    /** Test 20 — Good tolerance and elapsed days → progression allowed */
    @Test
    void test20_goodTolerance_progressionAllowed() {
        ProteinAdaptationProfile profile = buildProfileAtCurrentTarget(
                new BigDecimal("110"),
                new BigDecimal("135"),
                ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT,
                ProteinToleranceStatus.GOOD,
                ProteinAdaptationStage.ADAPTING
        );

        ProteinAdaptationService service = new ProteinAdaptationService(
                mockRepository(profile));

        com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult result =
                service.evaluate(profile);

        // Should have advanced from 110 to 120
        assertEquals(0, new BigDecimal("120").compareTo(result.currentTargetGrams()),
                "Good tolerance: target should advance by 10 g");
    }

    /** Test 21 — Mild GI issues → hold current target */
    @Test
    void test21_mildGiIssues_hold() {
        ProteinAdaptationProfile profile = buildProfileAtCurrentTarget(
                new BigDecimal("110"),
                new BigDecimal("135"),
                ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT + 1,
                ProteinToleranceStatus.MILD_ISSUES,
                ProteinAdaptationStage.ADAPTING
        );

        ProteinAdaptationService service = new ProteinAdaptationService(
                mockRepository(profile));

        com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult result =
                service.evaluate(profile);

        // Target must NOT increase
        assertEquals(0, new BigDecimal("110").compareTo(result.currentTargetGrams()),
                "Mild issues: target must be held at 110 g");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.TOLERANCE_HOLD),
                "Should emit TOLERANCE_HOLD warning");
    }

    /** Test 22 — Significant GI issues → hold + warning */
    @Test
    void test22_significantGiIssues_holdAndWarn() {
        ProteinAdaptationProfile profile = buildProfileAtCurrentTarget(
                new BigDecimal("110"),
                new BigDecimal("135"),
                ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT + 3,
                ProteinToleranceStatus.SIGNIFICANT_ISSUES,
                ProteinAdaptationStage.ADAPTING
        );

        ProteinAdaptationService service = new ProteinAdaptationService(
                mockRepository(profile));

        com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult result =
                service.evaluate(profile);

        assertEquals(0, new BigDecimal("110").compareTo(result.currentTargetGrams()),
                "Significant issues: target must be held");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.SIGNIFICANT_GI_SYMPTOMS),
                "Should emit SIGNIFICANT_GI_SYMPTOMS warning");
    }

    /** Test 23 — Long-term target already reached */
    @Test
    void test23_targetAlreadyReached() {
        ProteinAdaptationProfile profile = buildProfileAtCurrentTarget(
                new BigDecimal("135"),
                new BigDecimal("135"),
                14,
                ProteinToleranceStatus.GOOD,
                ProteinAdaptationStage.TARGET_REACHED
        );

        ProteinAdaptationService service = new ProteinAdaptationService(
                mockRepository(profile));

        com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult result =
                service.evaluate(profile);

        assertEquals(0, new BigDecimal("135").compareTo(result.currentTargetGrams()),
                "Target reached: current target should remain at 135 g");

        assertFalse(result.rampUpRequired(),
                "Target reached: ramp-up should NOT be required");

        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.TARGET_ALREADY_REACHED),
                "Should emit TARGET_ALREADY_REACHED warning");
    }

    // ================================================================
    // TESTS 24–27: Medical conditions
    // ================================================================

    /** Test 24 — Kidney disease → CLINICAL_REVIEW_REQUIRED */
    @Test
    void test24_kidneyDisease_clinicalReview() {
        ProteinCalculationInput input = inputWithConditions(
                75, Goal.FAT_LOSS, true,
                Set.of(MedicalCondition.KIDNEY_DISEASE));

        ProteinTargetResult result = calculator.calculate(input);

        assertTrue(result.requiresClinicalReview(),
                "KIDNEY_DISEASE should require clinical review");
        assertEquals(CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED, result.safetyStatus());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.KIDNEY_DISEASE_DETECTED),
                "Should emit KIDNEY_DISEASE_DETECTED warning");
    }

    /** Test 25 — Liver disease → CLINICAL_REVIEW_REQUIRED */
    @Test
    void test25_liverDisease_clinicalReview() {
        ProteinCalculationInput input = inputWithConditions(
                75, Goal.MAINTENANCE, false,
                Set.of(MedicalCondition.LIVER_DISEASE));

        ProteinTargetResult result = calculator.calculate(input);

        assertTrue(result.requiresClinicalReview(),
                "LIVER_DISEASE should require clinical review");
        assertEquals(CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED, result.safetyStatus());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.LIVER_DISEASE_DETECTED),
                "Should emit LIVER_DISEASE_DETECTED warning");
    }

    /**
     * Test 26 — Celiac disease → appropriate dietary warning,
     * but does NOT trigger CLINICAL_REVIEW_REQUIRED.
     * Protein quantity is not restricted by celiac disease.
     */
    @Test
    void test26_celiac_noProteinRestriction() {
        ProteinCalculationInput input = inputWithConditions(
                70, Goal.MUSCLE_GAIN, true,
                Set.of(MedicalCondition.CELIAC));

        ProteinTargetResult result = calculator.calculate(input);

        // Celiac does NOT require clinical review for protein quantity
        assertFalse(result.requiresClinicalReview(),
                "CELIAC should not require clinical review for protein quantity");

        // Celiac produces a dietary guidance warning about source selection
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CELIAC_DISEASE_DETECTED),
                "Should emit CELIAC_DISEASE_DETECTED warning about source selection");

        // Protein target itself is still calculated
        // 70 × 1.8 = 126
        assertEquals(0, new BigDecimal("126.00").compareTo(result.longTermTargetGrams()),
                "Celiac: protein target should not be restricted");
    }

    /** Test 27 — Hyperthyroidism → appropriate safety handling (warning, not invention) */
    @Test
    void test27_hyperthyroid_safetyHandling() {
        ProteinCalculationInput input = inputWithConditions(
                70, Goal.MAINTENANCE, false,
                Set.of(MedicalCondition.HYPERTHYROID));

        ProteinTargetResult result = calculator.calculate(input);

        // HYPERTHYROID is not a clinical review condition for protein in our model —
        // it gets a HYPERTHYROID_DETECTED warning but does not block the target
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.HYPERTHYROID_DETECTED),
                "Should emit HYPERTHYROID_DETECTED warning");
    }

    // ================================================================
    // TESTS 28–30: Safety guarantees
    // ================================================================

    /**
     * Test 28 — No bioavailability multiplier > 100% is ever applied.
     *
     * <p>The calculated target must equal weight × proteinPerKg (clamped to
     * safety bounds). No factor can make the target exceed this value "because
     * of bioavailability". The test verifies the target is never inflated
     * beyond what the multiplier alone would produce.</p>
     */
    @Test
    void test28_noBioavailabilityMultiplierAbove100Percent() {
        double weightKg = 75;
        ProteinCalculationInput input = input(weightKg, Goal.FAT_LOSS, true);

        ProteinTargetResult result = calculator.calculate(input);

        // The target must be exactly weight × multiplier (within clamping bounds)
        BigDecimal expectedMax = BigDecimal.valueOf(weightKg)
                .multiply(result.proteinPerKg());

        // Target must never exceed the raw product (no bioavailability inflation)
        assertTrue(result.longTermTargetGrams().compareTo(expectedMax) <= 0,
                "Target must never exceed weight × proteinPerKg (no bioavailability inflation). " +
                "Expected ≤ " + expectedMax + ", got " + result.longTermTargetGrams());
    }

    /**
     * Test 29 — Protein target is never negative.
     */
    @Test
    void test29_targetNeverNegative() {
        // Extremely small but valid weight
        ProteinCalculationInput input = new ProteinCalculationInput(
                new BigDecimal("0.01"),   // nearly zero but technically positive — will clamp to MIN
                null, Goal.MAINTENANCE, false, null, null, null, null);

        // Weight is > 0, so should not throw; result should be clamped to MIN_PROTEIN_GRAMS
        ProteinTargetResult result = calculator.calculate(input);
        assertTrue(result.longTermTargetGrams().compareTo(BigDecimal.ZERO) > 0,
                "Protein target must never be negative or zero");
    }

    /**
     * Test 30 — Adaptation target can never exceed the long-term target.
     */
    @Test
    void test30_adaptationNeverExceedsLongTermTarget() {
        // Start at target minus one increment
        BigDecimal longTerm = new BigDecimal("135");
        BigDecimal current  = longTerm.subtract(ProteinAdaptationService.DEFAULT_INCREMENT_GRAMS);

        ProteinAdaptationProfile profile = buildProfileAtCurrentTarget(
                current,
                longTerm,
                ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT,
                ProteinToleranceStatus.GOOD,
                ProteinAdaptationStage.ADAPTING
        );

        ProteinAdaptationService service = new ProteinAdaptationService(
                mockRepository(profile));

        com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult result =
                service.evaluate(profile);

        assertTrue(result.currentTargetGrams().compareTo(longTerm) <= 0,
                "Adaptation target must never exceed the long-term target");
    }

    // ================================================================
    // Test helpers
    // ================================================================

    /**
     * Builds a {@link ProteinAdaptationProfile} without a database.
     */
    private ProteinAdaptationProfile buildProfileAtCurrentTarget(
            BigDecimal currentTarget,
            BigDecimal longTermTarget,
            int daysHeld,
            com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus tolerance,
            com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage stage) {

        return ProteinAdaptationProfile.builder()
                .currentTargetGrams(currentTarget)
                .longTermTargetGrams(longTermTarget)
                .currentProteinGrams(null)
                .adaptationStage(stage)
                .daysAtCurrentTarget(daysHeld)
                .toleranceStatus(tolerance)
                .lastTargetChangeAt(java.time.LocalDate.now()
                        .minusDays(daysHeld))
                .build();
    }

    /**
     * Returns a minimal mock of {@link ProteinAdaptationProfileRepository}
     * that persists to an in-memory reference (no real DB required).
     */
    private com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository
    mockRepository(ProteinAdaptationProfile profile) {

        return new com.dietapp.diet_app.health_profile.NutritionTarget.repository
                .ProteinAdaptationProfileRepository() {

            private ProteinAdaptationProfile stored = profile;

            @Override
            public java.util.Optional<ProteinAdaptationProfile>
            findByHealthProfile(
                    com.dietapp.diet_app.health_profile.entity.HealthProfile hp) {
                return java.util.Optional.ofNullable(stored);
            }

            @Override
            public java.util.Optional<ProteinAdaptationProfile>
            findByHealthProfileId(java.util.UUID id) {
                return java.util.Optional.ofNullable(stored);
            }
            @Override
            public java.util.List<com.dietapp.diet_app.health_profile.NutritionTarget
                    .scheduler.EligibleProteinProfile>
            findEligibleForScheduledEvaluation(
                    java.util.Collection<com.dietapp.diet_app.health_profile
                            .NutritionTarget.enums.ProteinAdaptationStage> stages,
                    java.time.LocalDate today,
                    java.util.UUID afterId,
                    org.springframework.data.domain.Pageable pageable) {
                return java.util.List.of();
            }



            // ── JpaRepository pass-through stubs ──────────────────────

            @Override
            public <S extends ProteinAdaptationProfile> S save(S entity) {
                //noinspection unchecked
                stored = entity;
                return entity;
            }

            @Override
            public java.util.Optional<ProteinAdaptationProfile>
            findById(java.util.UUID uuid) {
                return java.util.Optional.ofNullable(stored);
            }

            @Override
            public void deleteById(java.util.UUID uuid) { }

            @Override
            public void delete(ProteinAdaptationProfile entity) { }

            @Override
            public boolean existsById(java.util.UUID uuid) { return stored != null; }

            @Override
            public long count() { return stored != null ? 1 : 0; }

            @Override
            public java.util.List<ProteinAdaptationProfile> findAll() {
                return stored != null
                        ? java.util.List.of(stored)
                        : java.util.List.of();
            }

            @Override
            public java.util.List<ProteinAdaptationProfile>
            findAllById(Iterable<java.util.UUID> ids) {
                return java.util.List.of();
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            java.util.List<S> saveAll(Iterable<S> entities) {
                return java.util.List.of();
            }

            @Override
            public void deleteAll() { stored = null; }

            @Override
            public void deleteAll(
                    Iterable<? extends ProteinAdaptationProfile> entities) { }

            @Override
            public void deleteAllById(Iterable<? extends java.util.UUID> ids) { }

            @Override
            public void flush() { }

            @Override
            public <S extends ProteinAdaptationProfile> S saveAndFlush(S entity) {
                return save(entity);
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            java.util.List<S> saveAllAndFlush(Iterable<S> entities) {
                return java.util.List.of();
            }

            @Override
            public void deleteAllInBatch(
                    Iterable<ProteinAdaptationProfile> entities) { }

            @Override
            public void deleteAllByIdInBatch(
                    Iterable<java.util.UUID> ids) { }

            @Override
            public void deleteAllInBatch() { stored = null; }

            @Override
            public ProteinAdaptationProfile getOne(java.util.UUID uuid) {
                return stored;
            }

            @Override
            public ProteinAdaptationProfile getById(java.util.UUID uuid) {
                return stored;
            }

            @Override
            public ProteinAdaptationProfile getReferenceById(java.util.UUID uuid) {
                return stored;
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            java.util.Optional<S> findOne(
                    org.springframework.data.domain.Example<S> example) {
                return java.util.Optional.empty();
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            java.util.List<S> findAll(
                    org.springframework.data.domain.Example<S> example) {
                return java.util.List.of();
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            java.util.List<S> findAll(
                    org.springframework.data.domain.Example<S> example,
                    org.springframework.data.domain.Sort sort) {
                return java.util.List.of();
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            org.springframework.data.domain.Page<S> findAll(
                    org.springframework.data.domain.Example<S> example,
                    org.springframework.data.domain.Pageable pageable) {
                return org.springframework.data.domain.Page.empty();
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            long count(org.springframework.data.domain.Example<S> example) {
                return 0;
            }

            @Override
            public <S extends ProteinAdaptationProfile>
            boolean exists(org.springframework.data.domain.Example<S> example) {
                return false;
            }

            @Override
            public <S extends ProteinAdaptationProfile, R>
            R findBy(org.springframework.data.domain.Example<S> example,
                     java.util.function.Function<
                             org.springframework.data.repository.query
                                     .FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
                return null;
            }

            @Override
            public org.springframework.data.domain.Page<ProteinAdaptationProfile>
            findAll(org.springframework.data.domain.Pageable pageable) {
                return org.springframework.data.domain.Page.empty();
            }

            @Override
            public java.util.List<ProteinAdaptationProfile>
            findAll(org.springframework.data.domain.Sort sort) {
                return java.util.List.of();
            }
        };
    }
}
