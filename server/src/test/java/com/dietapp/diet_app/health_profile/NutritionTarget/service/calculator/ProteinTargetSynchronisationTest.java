package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests that an existing {@link ProteinAdaptationProfile} has its long-term
 * target synchronised with a fresh {@link ProteinCalculator} result, without
 * resetting adaptation progress.
 *
 * <p>The real {@link ProteinCalculator}, {@link ProteinAdaptationService} and
 * {@link MacroCalculator} are used; only the repository is mocked. At 75 kg
 * with resistance training: FAT_LOSS = 1.8 g/kg = 135 g,
 * BODY_RECOMPOSITION = 2.0 g/kg = 150 g. At 80 kg FAT_LOSS = 144 g.</p>
 */
class ProteinTargetSynchronisationTest {

    private static final LocalDate LONG_AGO = LocalDate.now().minusDays(20);

    private ProteinAdaptationProfileRepository repository;
    private MacroCalculator macroCalculator;
    private HealthProfile healthProfile;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);
        when(repository.save(any(ProteinAdaptationProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByHealthProfile(any())).thenReturn(Optional.empty());

        macroCalculator = new MacroCalculator(
                new ProteinCalculator(),
                new ProteinAdaptationService(repository),
                mock(FatCalculator.class),
                mock(CarbohydrateCalculator.class)
        );
        healthProfile = HealthProfile.builder().build();
    }

    // ── helpers ────────────────────────────────────────────────────────

    private ProteinCalculationInput input(
            String weightKg, Goal goal, String currentProtein, Set<MedicalCondition> conditions) {

        return new ProteinCalculationInput(
                new BigDecimal(weightKg),
                null,
                goal,
                true,
                null,
                null,
                currentProtein == null ? null : new BigDecimal(currentProtein),
                conditions
        );
    }

    private ProteinCalculationInput input(String weightKg, Goal goal) {
        return input(weightKg, goal, "70", Set.of());
    }

    private ProteinAdaptationProfile profile(
            String current,
            String longTerm,
            ProteinAdaptationStage stage,
            int days,
            ProteinToleranceStatus tolerance,
            String currentIntake) {

        return ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentProteinGrams(currentIntake == null ? null : new BigDecimal(currentIntake))
                .currentTargetGrams(new BigDecimal(current))
                .longTermTargetGrams(new BigDecimal(longTerm))
                .adaptationStage(stage)
                .daysAtCurrentTarget(days)
                .toleranceStatus(tolerance)
                .lastTargetChangeAt(LONG_AGO)
                .build();
    }

    private void assertGrams(String expected, BigDecimal actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                message + " (expected " + expected + " but was " + actual + ")");
    }

    // ── 1. Weight change ───────────────────────────────────────────────

    @Test
    void weightChange_updatesLongTermTarget_andKeepsProgress() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.ADAPTING, 3, ProteinToleranceStatus.GOOD, "70");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("80", Goal.FAT_LOSS), p);

        assertGrams("144", p.getLongTermTargetGrams(), "long-term target updated on entity");
        assertGrams("144", r.longTermTargetGrams(), "long-term target in result");
        assertGrams("110", p.getCurrentTargetGrams(), "current target preserved");
        assertEquals(ProteinAdaptationStage.ADAPTING, p.getAdaptationStage());
        assertEquals(3, p.getDaysAtCurrentTarget());
        assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());
        assertEquals(LONG_AGO, p.getLastTargetChangeAt());
        verify(repository, never()).findByHealthProfile(any());
    }

    // ── 2. Goal change ─────────────────────────────────────────────────

    @Test
    void goalChange_updatesLongTermTarget_andKeepsProgress() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.STARTING, 2, ProteinToleranceStatus.UNKNOWN, "70");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.BODY_RECOMPOSITION), p);

        assertGrams("150", p.getLongTermTargetGrams(), "long-term target updated on entity");
        assertGrams("150", r.longTermTargetGrams(), "long-term target in result");
        assertGrams("110", r.currentTargetGrams(), "current target preserved");
        assertEquals(ProteinAdaptationStage.STARTING, p.getAdaptationStage());
        assertEquals(2, p.getDaysAtCurrentTarget());
        assertTrue(r.rampUpRequired());
        verify(repository).save(p);
    }

    // ── 3. Unchanged target ────────────────────────────────────────────

    @Test
    void unchangedTarget_preservesEverything_andDoesNotWrite() {
        ProteinAdaptationProfile p = profile("120", "135",
                ProteinAdaptationStage.ADAPTING, 5, ProteinToleranceStatus.GOOD, "70");

        macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS), p);

        assertGrams("135", p.getLongTermTargetGrams(), "long-term target");
        assertGrams("120", p.getCurrentTargetGrams(), "current target");
        assertEquals(ProteinAdaptationStage.ADAPTING, p.getAdaptationStage());
        assertEquals(5, p.getDaysAtCurrentTarget());
        assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());
        assertEquals(LONG_AGO, p.getLastTargetChangeAt());
        verify(repository, never()).save(any());
    }

    // ── 4. New target lower than current target ────────────────────────

    @Test
    void newTargetLowerThanCurrent_clampsCurrent_andMarksTargetReached() {
        // current 140 is above the new long-term target of 135
        ProteinAdaptationProfile p = profile("140", "160",
                ProteinAdaptationStage.ADAPTING, 4, ProteinToleranceStatus.GOOD, "70");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS), p);

        assertGrams("135", p.getLongTermTargetGrams(), "long-term target");
        assertGrams("135", p.getCurrentTargetGrams(), "current clamped to new long-term target");
        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
        assertEquals(0, p.getDaysAtCurrentTarget());
        assertEquals(LocalDate.now(), p.getLastTargetChangeAt());
        assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());
        assertGrams("135", r.currentTargetGrams(), "result current target");
        assertFalse(r.rampUpRequired());
        assertTrue(p.getCurrentTargetGrams().compareTo(p.getLongTermTargetGrams()) <= 0);
    }

    // ── 5. TARGET_REACHED, then a higher target ────────────────────────

    @Test
    void targetReached_thenHigherTarget_resumesAdapting() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.TARGET_REACHED, 12, ProteinToleranceStatus.GOOD, "135");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("80", Goal.FAT_LOSS, "135", Set.of()), p);

        assertGrams("144", p.getLongTermTargetGrams(), "long-term target");
        assertGrams("135", p.getCurrentTargetGrams(),
                "current target is NOT jumped to the new target");
        assertEquals(ProteinAdaptationStage.ADAPTING, p.getAdaptationStage());
        assertEquals(0, p.getDaysAtCurrentTarget(), "day clock restarts");
        assertEquals(LocalDate.now(), p.getLastTargetChangeAt());
        assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());
        assertTrue(r.rampUpRequired());
    }

    @Test
    void targetReached_thenLowerTarget_staysTargetReached_atNewTarget() {
        ProteinAdaptationProfile p = profile("150", "150",
                ProteinAdaptationStage.TARGET_REACHED, 12, ProteinToleranceStatus.GOOD, "150");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS, "150", Set.of()), p);

        assertGrams("135", p.getCurrentTargetGrams(), "current clamped");
        assertGrams("135", p.getLongTermTargetGrams(), "long-term");
        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
        assertFalse(r.rampUpRequired());
    }

    @Test
    void currentEqualsNewLongTerm_becomesTargetReached_keepingDays() {
        ProteinAdaptationProfile p = profile("135", "150",
                ProteinAdaptationStage.ADAPTING, 6, ProteinToleranceStatus.GOOD, "70");

        macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS), p);

        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
        assertGrams("135", p.getCurrentTargetGrams(), "current unchanged");
        assertEquals(6, p.getDaysAtCurrentTarget());
    }

    // ── 6. Unknown current intake ──────────────────────────────────────

    @Test
    void unknownIntake_unchangedTarget_doesNotReinitialiseOrWrite() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.NOT_STARTED, 4, ProteinToleranceStatus.UNKNOWN, null);

        // Two consecutive requests
        macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS, null, Set.of()), p);
        macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS, null, Set.of()), p);

        verify(repository, never()).save(any());
        verify(repository, never()).findByHealthProfile(any());
        assertEquals(4, p.getDaysAtCurrentTarget(), "state must not be reset");
        assertEquals(LONG_AGO, p.getLastTargetChangeAt(), "last change date must not be reset");
        assertEquals(ProteinAdaptationStage.NOT_STARTED, p.getAdaptationStage());
    }

    @Test
    void unknownIntake_changedTarget_updatesWithoutReset() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.NOT_STARTED, 4, ProteinToleranceStatus.UNKNOWN, null);

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input("80", Goal.FAT_LOSS, null, Set.of()), p);

        assertGrams("144", p.getLongTermTargetGrams(), "long-term");
        assertGrams("144", p.getCurrentTargetGrams(),
                "no ramp without intake: current follows long-term");
        assertEquals(ProteinAdaptationStage.NOT_STARTED, p.getAdaptationStage());
        assertEquals(4, p.getDaysAtCurrentTarget());
        assertEquals(LONG_AGO, p.getLastTargetChangeAt());
        assertGrams("144", r.currentTargetGrams(), "result");
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));
    }

    @Test
    void notStarted_withIntakeNowKnown_isInitialisedOnce() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.NOT_STARTED, 0, ProteinToleranceStatus.UNKNOWN, null);
        when(repository.findByHealthProfile(any())).thenReturn(Optional.of(p));

        macroCalculator.calculateProtein(
                healthProfile, input("75", Goal.FAT_LOSS, "70", Set.of()), p);

        // initialise() path: a ramp now starts below the long-term target
        assertEquals(ProteinAdaptationStage.STARTING, p.getAdaptationStage());
        assertGrams("70", p.getCurrentProteinGrams(), "intake recorded");
        assertTrue(p.getCurrentTargetGrams().compareTo(p.getLongTermTargetGrams()) < 0);
    }

    // ── 7. Medical warnings remain intact ──────────────────────────────

    @Test
    void medicalWarnings_andClinicalReview_survivesTargetChange() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.ADAPTING, 3, ProteinToleranceStatus.MILD_ISSUES, "70");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile,
                input("80", Goal.FAT_LOSS, "70", Set.of(MedicalCondition.KIDNEY_DISEASE)),
                p);

        assertGrams("144", p.getLongTermTargetGrams(), "target still synchronised");
        assertTrue(r.requiresClinicalReview());
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.KIDNEY_DISEASE_DETECTED));
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
        assertEquals(ProteinToleranceStatus.MILD_ISSUES, p.getToleranceStatus(),
                "tolerance status must be preserved");
        assertEquals(ProteinToleranceStatus.MILD_ISSUES, r.toleranceStatus());
    }

    @Test
    void medicalWarnings_survivesUnchangedTarget() {
        ProteinAdaptationProfile p = profile("120", "135",
                ProteinAdaptationStage.ADAPTING, 5, ProteinToleranceStatus.GOOD, "70");

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile,
                input("75", Goal.FAT_LOSS, "70", Set.of(MedicalCondition.LIVER_DISEASE)),
                p);

        assertTrue(r.requiresClinicalReview());
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.LIVER_DISEASE_DETECTED));
        verify(repository, never()).save(any());
    }
}
