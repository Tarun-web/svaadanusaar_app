package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.*;
import com.dietapp.diet_app.health_profile.entity.*;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Policy A: the latest reported intake ({@code HealthProfile.currentNutritionProfile})
 * is separate from the adaptation baseline ({@code ProteinAdaptationProfile.currentProteinGrams}).
 *
 * <p>The real {@link ProteinCalculator}, {@link ProteinAdaptationService} and
 * {@link MacroCalculator} run end to end through
 * {@link NutritionTargetServiceImpl#calculateForCurrentUser()}; only the
 * repository (kept stateful) and the non-protein calculators are mocked.
 * At 75 kg, FAT_LOSS, no resistance training the long-term target is 120 g.</p>
 */
class ProteinIntakeBaselineSeparationTest {

    private static final LocalDate LONG_AGO = LocalDate.now().minusDays(20);

    private AtomicReference<ProteinAdaptationProfile> stored;
    private ProteinAdaptationProfileRepository repository;
    private NutritionTargetServiceImpl service;
    private HealthProfile healthProfile;

    @BeforeEach
    void setUp() {
        stored = new AtomicReference<>();
        repository = mock(ProteinAdaptationProfileRepository.class);
        when(repository.findByHealthProfile(any()))
                .thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(repository.save(any(ProteinAdaptationProfile.class)))
                .thenAnswer(inv -> {
                    ProteinAdaptationProfile p = inv.getArgument(0);
                    stored.set(p);
                    return p;
                });

        FatCalculator fatCalculator = mock(FatCalculator.class);
        when(fatCalculator.calculate(any())).thenReturn(new FatCalculationResult(
                new BigDecimal("60"), new BigDecimal("540"), new BigDecimal("600"),
                new BigDecimal("0.8"), "FAT_LOSS", true, CalorieSafetyStatus.SAFE,
                false, Collections.emptyList()));
        CarbohydrateCalculator carbCalculator = mock(CarbohydrateCalculator.class);
        when(carbCalculator.calculate(any())).thenReturn(new CarbohydrateCalculationResult(
                new BigDecimal("150"), new BigDecimal("600"), new BigDecimal("600"),
                true, CalorieSafetyStatus.SAFE, false, Collections.emptyList()));

        MacroCalculator macroCalculator = new MacroCalculator(
                new ProteinCalculator(),
                new ProteinAdaptationService(repository),
                fatCalculator,
                carbCalculator);

        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(new BigDecimal("75")).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder().primaryGoal(Goal.FAT_LOSS).build();
        healthProfile = HealthProfile.builder().build();
        pp.setHealthProfile(healthProfile);
        fg.setHealthProfile(healthProfile);
        healthProfile.setPersonalProfile(pp);
        healthProfile.setFitnessGoalProfile(fg);

        HealthProfileContextService context = mock(HealthProfileContextService.class);
        when(context.getCurrentUserHealthProfile()).thenReturn(healthProfile);

        GoalCalorieCalculator goalCalculator = mock(GoalCalorieCalculator.class);
        when(goalCalculator.calculate(any(), any(), any())).thenReturn(
                CalorieTargetResult.builder()
                        .tdee(new BigDecimal("2200"))
                        .proposedCalories(new BigDecimal("1700"))
                        .goal(Goal.FAT_LOSS).build());
        CalorieSafetyEngine safetyEngine = mock(CalorieSafetyEngine.class);
        when(safetyEngine.evaluate(any(), any(), any(), any())).thenReturn(
                new CalorieSafetyResult(new BigDecimal("1700"), new BigDecimal("1700"),
                        CalorieSafetyStatus.SAFE, false, false, Collections.emptyList()));
        FibreCalculator fibreCalculator = mock(FibreCalculator.class);
        when(fibreCalculator.calculate(any())).thenReturn(new BigDecimal("23.80"));
        WaterCalculator waterCalculator = mock(WaterCalculator.class);
        when(waterCalculator.calculate(any())).thenReturn(new WaterCalculationResult(
                new BigDecimal("2630"), new BigDecimal("2625"), BigDecimal.ZERO,
                BigDecimal.ZERO, true, Collections.emptyList()));

        service = new NutritionTargetServiceImpl(
                context, goalCalculator, safetyEngine, macroCalculator,
                fibreCalculator, waterCalculator, repository);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private void reportIntake(String grams) {
        healthProfile.setCurrentNutritionProfile(CurrentNutritionProfile.builder()
                .currentProteinGrams(grams == null ? null : new BigDecimal(grams))
                .build());
    }

    private void assertGrams(String expected, BigDecimal actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual, "expected " + expected + " but was null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    private boolean hasUnknownIntakeWarning(NutritionTargetResponse r) {
        return r.warnings().stream().anyMatch(w ->
                w.message().contains("[Protein]")
                        && w.message().contains("Current protein intake is unknown"));
    }

    /** Overwrites the persisted profile's progress so a scenario starts in a known state. */
    private ProteinAdaptationProfile forceState(
            ProteinAdaptationStage stage, String currentTarget, int days) {

        ProteinAdaptationProfile p = stored.get();
        p.setAdaptationStage(stage);
        p.setCurrentTargetGrams(new BigDecimal(currentTarget));
        p.setDaysAtCurrentTarget(days);
        p.setToleranceStatus(ProteinToleranceStatus.GOOD);
        p.setLastTargetChangeAt(LONG_AGO);
        return p;
    }

    private void assertProgressUnchanged(
            ProteinAdaptationProfile p, NutritionTargetResponse before, NutritionTargetResponse after,
            ProteinAdaptationStage stage, String currentTarget, int days) {

        assertEquals(stage, p.getAdaptationStage());
        assertGrams(currentTarget, p.getCurrentTargetGrams());
        assertEquals(days, p.getDaysAtCurrentTarget());
        assertEquals(LONG_AGO, p.getLastTargetChangeAt());
        assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());

        assertEquals(before.proteinGrams(), after.proteinGrams());
        assertEquals(before.longTermProteinGrams(), after.longTermProteinGrams());
        assertEquals(before.adaptationStage(), after.adaptationStage());
        assertEquals(before.rampUpRequired(), after.rampUpRequired());
        assertEquals(before.nextEligibleProteinIncreaseDate(),
                after.nextEligibleProteinIncreaseDate());
    }

    // ── 1-5. adaptationBaselineOutOfDate definition ────────────────────

    @Test
    void outOfDate_bothEqual_isFalse() {
        assertFalse(NutritionTargetServiceImpl.isBaselineOutOfDate(
                new BigDecimal("70"), new BigDecimal("70")));
        // numerically equal, different scale
        assertFalse(NutritionTargetServiceImpl.isBaselineOutOfDate(
                new BigDecimal("70.00"), new BigDecimal("70")));
    }

    @Test
    void outOfDate_bothDifferent_isTrue() {
        assertTrue(NutritionTargetServiceImpl.isBaselineOutOfDate(
                new BigDecimal("85"), new BigDecimal("70")));
    }

    @Test
    void outOfDate_latestPresentBaselineNull_isTrue() {
        assertTrue(NutritionTargetServiceImpl.isBaselineOutOfDate(
                new BigDecimal("70"), null));
    }

    @Test
    void outOfDate_latestNullBaselinePresent_isTrue() {
        assertTrue(NutritionTargetServiceImpl.isBaselineOutOfDate(
                null, new BigDecimal("70")));
    }

    @Test
    void outOfDate_bothNull_isFalse() {
        assertFalse(NutritionTargetServiceImpl.isBaselineOutOfDate(null, null));
    }

    @Test
    void response_equalValues_reportsBothAndNotOutOfDate() {
        reportIntake("70");

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertGrams("70", r.currentProteinIntakeGrams());
        assertGrams("70", r.adaptationBaselineProteinGrams());
        assertFalse(r.adaptationBaselineOutOfDate());
    }

    @Test
    void response_bothNull_reportsNullsAndNotOutOfDate() {
        // no intake ever reported -> no adaptation baseline captured
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertNull(r.currentProteinIntakeGrams());
        assertNull(r.adaptationBaselineProteinGrams());
        assertFalse(r.adaptationBaselineOutOfDate());
    }

    @Test
    void response_latestPresent_baselineNull_isOutOfDate() {
        // existing ADAPTING profile that never captured a baseline
        stored.set(ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentProteinGrams(null)
                .longTermTargetGrams(new BigDecimal("120"))
                .currentTargetGrams(new BigDecimal("110"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(3)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LONG_AGO)
                .build());
        reportIntake("80");

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertGrams("80", r.currentProteinIntakeGrams());
        assertNull(r.adaptationBaselineProteinGrams());
        assertTrue(r.adaptationBaselineOutOfDate());
        assertNull(stored.get().getCurrentProteinGrams(), "baseline must not be filled in");
    }

    @Test
    void response_latestNull_baselinePresent_isOutOfDate() {
        reportIntake("70");
        service.calculateForCurrentUser();
        reportIntake(null);

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertNull(r.currentProteinIntakeGrams());
        assertGrams("70", r.adaptationBaselineProteinGrams());
        assertTrue(r.adaptationBaselineOutOfDate());
    }

    @Test
    void response_bothDifferent_isOutOfDate() {
        reportIntake("70");
        service.calculateForCurrentUser();
        reportIntake("85");

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertGrams("85", r.currentProteinIntakeGrams());
        assertGrams("70", r.adaptationBaselineProteinGrams());
        assertTrue(r.adaptationBaselineOutOfDate());
    }

    // ── 6. Intake changes during STARTING ──────────────────────────────

    @Test
    void intakeChange_duringStarting_keepsBaselineAndProgress() {
        reportIntake("70");
        NutritionTargetResponse initial = service.calculateForCurrentUser();
        assertEquals(ProteinAdaptationStage.STARTING, initial.adaptationStage());
        ProteinAdaptationProfile p = forceState(
                ProteinAdaptationStage.STARTING, initial.proteinGrams().toPlainString(), 2);
        NutritionTargetResponse before = service.calculateForCurrentUser();
        clearInvocations(repository);

        reportIntake("90");
        NutritionTargetResponse after = service.calculateForCurrentUser();

        assertGrams("90", after.currentProteinIntakeGrams());
        assertGrams("70", after.adaptationBaselineProteinGrams());
        assertGrams("70", p.getCurrentProteinGrams());
        assertTrue(after.adaptationBaselineOutOfDate());
        assertProgressUnchanged(p, before, after,
                ProteinAdaptationStage.STARTING, initial.proteinGrams().toPlainString(), 2);
        verify(repository, never()).save(any());
    }

    // ── 7. Intake changes during ADAPTING ──────────────────────────────

    @Test
    void intakeChange_duringAdapting_keepsBaselineAndProgress() {
        reportIntake("70");
        service.calculateForCurrentUser();
        ProteinAdaptationProfile p = forceState(ProteinAdaptationStage.ADAPTING, "110", 4);
        NutritionTargetResponse before = service.calculateForCurrentUser();
        clearInvocations(repository);

        reportIntake("100");
        NutritionTargetResponse after = service.calculateForCurrentUser();

        assertGrams("100", after.currentProteinIntakeGrams());
        assertGrams("70", after.adaptationBaselineProteinGrams());
        assertGrams("70", p.getCurrentProteinGrams());
        assertTrue(after.adaptationBaselineOutOfDate());
        assertProgressUnchanged(p, before, after, ProteinAdaptationStage.ADAPTING, "110", 4);
        assertGrams("110", after.proteinGrams());
        assertTrue(after.rampUpRequired());
        verify(repository, never()).save(any());
    }

    // ── 8. Intake changes after TARGET_REACHED ─────────────────────────

    @Test
    void intakeChange_afterTargetReached_keepsBaselineAndProgress() {
        reportIntake("70");
        service.calculateForCurrentUser();
        ProteinAdaptationProfile p = forceState(ProteinAdaptationStage.TARGET_REACHED, "120", 9);
        NutritionTargetResponse before = service.calculateForCurrentUser();
        clearInvocations(repository);

        reportIntake("125");
        NutritionTargetResponse after = service.calculateForCurrentUser();

        assertGrams("125", after.currentProteinIntakeGrams());
        assertGrams("70", after.adaptationBaselineProteinGrams());
        assertGrams("70", p.getCurrentProteinGrams());
        assertTrue(after.adaptationBaselineOutOfDate());
        assertProgressUnchanged(p, before, after,
                ProteinAdaptationStage.TARGET_REACHED, "120", 9);
        assertFalse(after.rampUpRequired());
        verify(repository, never()).save(any());
    }

    // ── 9. Intake cleared during adaptation ────────────────────────────

    @Test
    void intakeCleared_duringAdaptation_keepsBaselineAndProgress_andWarns() {
        reportIntake("70");
        service.calculateForCurrentUser();
        ProteinAdaptationProfile p = forceState(ProteinAdaptationStage.ADAPTING, "110", 5);
        NutritionTargetResponse before = service.calculateForCurrentUser();
        assertFalse(hasUnknownIntakeWarning(before));
        clearInvocations(repository);

        reportIntake(null);
        NutritionTargetResponse after = service.calculateForCurrentUser();

        assertNull(after.currentProteinIntakeGrams());
        assertGrams("70", after.adaptationBaselineProteinGrams());
        assertGrams("70", p.getCurrentProteinGrams());
        assertTrue(after.adaptationBaselineOutOfDate());
        assertTrue(hasUnknownIntakeWarning(after));
        assertProgressUnchanged(p, before, after, ProteinAdaptationStage.ADAPTING, "110", 5);
        verify(repository, never()).save(any());
    }

    @Test
    void intakeCleared_whenNoCurrentNutritionProfileAtAll_behavesTheSame() {
        reportIntake("70");
        service.calculateForCurrentUser();
        healthProfile.setCurrentNutritionProfile(null);

        NutritionTargetResponse after = service.calculateForCurrentUser();

        assertNull(after.currentProteinIntakeGrams());
        assertGrams("70", after.adaptationBaselineProteinGrams());
        assertTrue(hasUnknownIntakeWarning(after));
    }

    // ── 10. Unknown-intake warning follows the latest reported intake ──

    @Test
    void unknownIntakeWarning_followsLatestIntake_notBaseline() {
        // baseline present (70), latest cleared -> warning
        reportIntake("70");
        service.calculateForCurrentUser();
        reportIntake(null);
        assertTrue(hasUnknownIntakeWarning(service.calculateForCurrentUser()));

        // latest reported again -> warning gone, baseline still 70
        reportIntake("75");
        NutritionTargetResponse reported = service.calculateForCurrentUser();
        assertFalse(hasUnknownIntakeWarning(reported));
        assertGrams("70", reported.adaptationBaselineProteinGrams());
    }

    @Test
    void unknownIntakeWarning_absentWhenLatestPresent_evenIfBaselineNull() {
        stored.set(ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentProteinGrams(null)
                .longTermTargetGrams(new BigDecimal("120"))
                .currentTargetGrams(new BigDecimal("110"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(3)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LONG_AGO)
                .build());
        reportIntake("80");

        assertFalse(hasUnknownIntakeWarning(service.calculateForCurrentUser()));
    }

    @Test
    void unknownIntakeWarning_presentWhenLatestNull_evenIfBaselinePresent_codeLevel() {
        ProteinAdaptationService adaptation = new ProteinAdaptationService(repository);
        ProteinAdaptationProfile p = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentProteinGrams(new BigDecimal("70"))
                .longTermTargetGrams(new BigDecimal("120"))
                .currentTargetGrams(new BigDecimal("110"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(3)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LONG_AGO)
                .build();

        ProteinAdaptationResult cleared = adaptation.toResult(p, null);
        ProteinAdaptationResult reported = adaptation.toResult(p, new BigDecimal("70"));

        assertTrue(cleared.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));
        assertTrue(reported.warnings().stream()
                .noneMatch(w -> w.code() == ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));
        // the baseline is surfaced unchanged under its own name
        assertGrams("70", cleared.adaptationBaselineProteinGrams());
        assertGrams("70", p.getCurrentProteinGrams());
    }

    @Test
    void toResult_withoutExplicitIntake_readsLatestFromHealthProfile() {
        ProteinAdaptationService adaptation = new ProteinAdaptationService(repository);
        ProteinAdaptationProfile p = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentProteinGrams(new BigDecimal("70"))
                .longTermTargetGrams(new BigDecimal("120"))
                .currentTargetGrams(new BigDecimal("110"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(3)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LONG_AGO)
                .build();

        reportIntake(null);
        assertTrue(adaptation.toResult(p).warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));

        reportIntake("65");
        assertTrue(adaptation.toResult(p).warnings().stream()
                .noneMatch(w -> w.code() == ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));
    }

    // ── 11 & 12. Existing ramp fields keep their meaning and values ────

    @Test
    void existingFields_unchangedByIntakeUpdate_nextEligibleDateIncluded() {
        reportIntake("70");
        service.calculateForCurrentUser();
        ProteinAdaptationProfile p = forceState(ProteinAdaptationStage.ADAPTING, "110", 3);
        NutritionTargetResponse before = service.calculateForCurrentUser();

        // Meaning of the existing fields
        assertGrams("110", before.proteinGrams());                     // current target
        assertGrams("120", before.longTermProteinGrams());             // long-term target
        assertEquals(ProteinAdaptationStage.ADAPTING, before.adaptationStage());
        assertTrue(before.rampUpRequired());
        assertEquals(LocalDate.now().plusDays(
                        ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT - 3),
                before.nextEligibleProteinIncreaseDate());

        reportIntake("95");
        NutritionTargetResponse after = service.calculateForCurrentUser();
        reportIntake(null);
        NutritionTargetResponse cleared = service.calculateForCurrentUser();

        for (NutritionTargetResponse r : new NutritionTargetResponse[]{after, cleared}) {
            assertEquals(before.proteinGrams(), r.proteinGrams());
            assertEquals(before.longTermProteinGrams(), r.longTermProteinGrams());
            assertEquals(before.adaptationStage(), r.adaptationStage());
            assertEquals(before.rampUpRequired(), r.rampUpRequired());
            assertEquals(before.nextEligibleProteinIncreaseDate(),
                    r.nextEligibleProteinIncreaseDate());
            assertEquals(before.fatGrams(), r.fatGrams());
            assertEquals(before.carbohydrateGrams(), r.carbohydrateGrams());
            assertEquals(before.finalCalories(), r.finalCalories());
        }
        assertEquals(3, p.getDaysAtCurrentTarget());
        assertGrams("110", p.getCurrentTargetGrams());
        assertEquals(LONG_AGO, p.getLastTargetChangeAt());
        assertNull(p.getLastEvaluatedOn());
    }

    @Test
    void adaptationResult_baselineComponent_isTheStoredBaseline_notLatestIntake() {
        reportIntake("70");
        service.calculateForCurrentUser();
        reportIntake("99");

        ProteinAdaptationResult r = new ProteinAdaptationService(repository)
                .toResult(stored.get());

        assertGrams("70", r.adaptationBaselineProteinGrams());
    }
}
