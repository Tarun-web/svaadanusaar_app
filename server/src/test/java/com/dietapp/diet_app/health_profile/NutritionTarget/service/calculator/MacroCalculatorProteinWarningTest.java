package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinWarning;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Focused tests for protein warning and clinical-review propagation in
 * {@link MacroCalculator#calculateProtein}.
 *
 * <p>A real {@link ProteinCalculator} is used so the warnings under test are
 * exactly the ones produced by the existing medical rules. Only the
 * adaptation service is mocked.</p>
 */
class MacroCalculatorProteinWarningTest {

    private static final BigDecimal CURRENT_TARGET = new BigDecimal("110");
    private static final BigDecimal LONG_TERM      = new BigDecimal("135");

    private ProteinAdaptationService adaptationService;
    private MacroCalculator          macroCalculator;
    private HealthProfile            healthProfile;

    @BeforeEach
    void setUp() {
        adaptationService = mock(ProteinAdaptationService.class);
        macroCalculator   = new MacroCalculator(
                new ProteinCalculator(),
                adaptationService,
                mock(FatCalculator.class),
                mock(CarbohydrateCalculator.class)
        );
        healthProfile = HealthProfile.builder().build();
    }

    // ── helpers ────────────────────────────────────────────────────────

    private ProteinCalculationInput input(Set<MedicalCondition> conditions) {
        return new ProteinCalculationInput(
                new BigDecimal("75"),
                null,
                Goal.FAT_LOSS,
                true,
                null,
                null,
                null,
                conditions
        );
    }

    private ProteinAdaptationResult adaptationResult(
            ProteinAdaptationStage stage, List<ProteinWarning> warnings) {

        return new ProteinAdaptationResult(
                CURRENT_TARGET, LONG_TERM, null,
                stage, true, null,
                ProteinToleranceStatus.UNKNOWN,
                warnings
        );
    }

    /** Stubs the "new profile" path: initialise() then toResult(). */
    private void stubInitialisePath(List<ProteinWarning> adaptationWarnings) {
        ProteinAdaptationProfile created = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .adaptationStage(ProteinAdaptationStage.STARTING)
                .build();

        when(adaptationService.initialise(any(), any(), any())).thenReturn(created);
        when(adaptationService.toResult(eq(created), any()))
                .thenReturn(adaptationResult(
                        ProteinAdaptationStage.STARTING, adaptationWarnings));
    }

    private boolean hasCode(ProteinAdaptationResult r, ProteinWarningCode code) {
        return r.warnings().stream().anyMatch(w -> w.code() == code);
    }

    private long countCode(ProteinAdaptationResult r, ProteinWarningCode code) {
        return r.warnings().stream().filter(w -> w.code() == code).count();
    }

    // ── 1. Kidney disease warning propagation ──────────────────────────

    @Test
    void kidneyDisease_warningsReachAdaptationResult() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);

        assertTrue(hasCode(r, ProteinWarningCode.KIDNEY_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
    }

    // ── 2. Liver disease warning propagation ───────────────────────────

    @Test
    void liverDisease_warningsReachAdaptationResult() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.LIVER_DISEASE)), null);

        assertTrue(hasCode(r, ProteinWarningCode.LIVER_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
    }

    // ── 3. Clinical review flag propagation ────────────────────────────

    @Test
    void kidneyDisease_clinicalReviewFlagPropagated() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);

        assertTrue(r.requiresClinicalReview());
    }

    @Test
    void liverDisease_clinicalReviewFlagPropagated() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.LIVER_DISEASE)), null);

        assertTrue(r.requiresClinicalReview());
    }

    @Test
    void warningOnlyCondition_doesNotSetClinicalReviewFlag() {
        // Existing ProteinCalculator rule: HYPERTHYROID warns but does not
        // require clinical review.
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.HYPERTHYROID)), null);

        assertTrue(hasCode(r, ProteinWarningCode.HYPERTHYROID_DETECTED));
        assertFalse(r.requiresClinicalReview());
    }

    // ── 4. Normal user: nothing added ──────────────────────────────────

    @Test
    void normalUser_noMedicalWarningsAndNoClinicalReview() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of()), null);

        assertTrue(r.warnings().isEmpty());
        assertFalse(r.requiresClinicalReview());
    }

    @Test
    void nullMedicalConditions_treatedAsNormalUser() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(null), null);

        assertTrue(r.warnings().isEmpty());
        assertFalse(r.requiresClinicalReview());
    }

    // ── 5. Existing adaptation profile ─────────────────────────────────

    @Test
    void existingProfile_calculatorWarningsStillPropagated() {
        ProteinAdaptationProfile existing = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .lastTargetChangeAt(LocalDate.now())
                .build();

        ProteinWarning tolerance = new ProteinWarning(
                ProteinWarningCode.TOLERANCE_HOLD, "Held.");
        when(adaptationService.toResult(eq(existing), any()))
                .thenReturn(adaptationResult(
                        ProteinAdaptationStage.ADAPTING, List.of(tolerance)));

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), existing);

        // Existing profile is used, not re-initialised
        verify(adaptationService, never()).initialise(any(), any(), any());

        // Adaptation warning is kept AND calculator warnings are added
        assertTrue(hasCode(r, ProteinWarningCode.TOLERANCE_HOLD));
        assertTrue(hasCode(r, ProteinWarningCode.KIDNEY_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
        assertTrue(r.requiresClinicalReview());
    }

    @Test
    void existingProfile_normalUser_keepsOnlyAdaptationWarnings() {
        ProteinAdaptationProfile existing = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .build();

        ProteinWarning tolerance = new ProteinWarning(
                ProteinWarningCode.TOLERANCE_HOLD, "Held.");
        when(adaptationService.toResult(eq(existing), any()))
                .thenReturn(adaptationResult(
                        ProteinAdaptationStage.ADAPTING, List.of(tolerance)));

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of()), existing);

        assertEquals(List.of(tolerance), r.warnings());
        assertFalse(r.requiresClinicalReview());
    }

    // ── 6. Multiple warnings preserved together ────────────────────────

    @Test
    void multipleWarnings_allPreservedTogether() {
        ProteinWarning intakeUnknown = new ProteinWarning(
                ProteinWarningCode.CURRENT_INTAKE_UNKNOWN, "Unknown intake.");
        stubInitialisePath(List.of(intakeUnknown));

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile,
                input(Set.of(
                        MedicalCondition.KIDNEY_DISEASE,
                        MedicalCondition.LIVER_DISEASE,
                        MedicalCondition.CELIAC)),
                null);

        assertTrue(hasCode(r, ProteinWarningCode.CURRENT_INTAKE_UNKNOWN));
        assertTrue(hasCode(r, ProteinWarningCode.KIDNEY_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.LIVER_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.CELIAC_DISEASE_DETECTED));
        assertTrue(hasCode(r, ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
        assertTrue(r.requiresClinicalReview());
    }

    @Test
    void adaptationWarningsAreNotOverwritten_andComeFirst() {
        ProteinWarning intakeUnknown = new ProteinWarning(
                ProteinWarningCode.CURRENT_INTAKE_UNKNOWN, "Unknown intake.");
        stubInitialisePath(List.of(intakeUnknown));

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);

        assertEquals(intakeUnknown, r.warnings().get(0));
    }

    // ── Duplicate avoidance ────────────────────────────────────────────

    @Test
    void identicalWarningFromBothSources_appearsOnce() {
        // Pre-seed the adaptation result with a warning that the calculator
        // will also emit (same code and same message).
        ProteinAdaptationResult calcOnly;
        {
            stubInitialisePath(List.of());
            calcOnly = macroCalculator.calculateProtein(
                    healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);
        }
        ProteinWarning kidney = calcOnly.warnings().stream()
                .filter(w -> w.code() == ProteinWarningCode.KIDNEY_DISEASE_DETECTED)
                .findFirst().orElseThrow();

        stubInitialisePath(List.of(kidney));

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);

        assertEquals(1, countCode(r, ProteinWarningCode.KIDNEY_DISEASE_DETECTED));
    }

    // ── Non-regression: target values are untouched ────────────────────

    @Test
    void targetValuesAreUnchangedByWarningMerge() {
        stubInitialisePath(List.of());

        ProteinAdaptationResult r = macroCalculator.calculateProtein(
                healthProfile, input(Set.of(MedicalCondition.KIDNEY_DISEASE)), null);

        assertEquals(0, CURRENT_TARGET.compareTo(r.currentTargetGrams()));
        assertEquals(0, LONG_TERM.compareTo(r.longTermTargetGrams()));
        assertEquals(ProteinAdaptationStage.STARTING, r.adaptationStage());
    }
}
