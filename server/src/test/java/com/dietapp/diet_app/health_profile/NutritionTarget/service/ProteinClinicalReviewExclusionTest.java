package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ProteinAdaptationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.ProteinCalculator;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Tests that users whose current medical conditions require clinical review
 * cannot progress automatically through
 * {@link ProteinAdaptationService#evaluate}, while everything else keeps
 * working.
 *
 * <p>The fixture profile is eligible to advance (110 g → 120 g) so any block
 * is attributable to the medical condition alone.</p>
 */
class ProteinClinicalReviewExclusionTest {

    private static final LocalDate LAST_CHANGE = LocalDate.now().minusDays(10);

    private ProteinAdaptationProfileRepository repository;
    private ProteinAdaptationService service;

    private MedicalProfile medicalProfile;
    private HealthProfile healthProfile;
    private ProteinAdaptationProfile profile;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);
        when(repository.save(any(ProteinAdaptationProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        service = new ProteinAdaptationService(repository);

        medicalProfile = MedicalProfile.builder().build();
        healthProfile = HealthProfile.builder().build();
        healthProfile.setMedicalProfile(medicalProfile);
        medicalProfile.setHealthProfile(healthProfile);

        profile = ProteinAdaptationProfile.builder()
                .healthProfile(healthProfile)
                .currentTargetGrams(new BigDecimal("110"))
                .longTermTargetGrams(new BigDecimal("135"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT + 1)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LAST_CHANGE)
                .build();
    }

    // ── helpers ────────────────────────────────────────────────────────

    private void setConditions(MedicalCondition... conditions) {
        medicalProfile.setMedicalConditions(new HashSet<>(List.of(conditions)));
    }

    private void assertStateUntouched() {
        assertEquals(0, new BigDecimal("110").compareTo(profile.getCurrentTargetGrams()),
                "current target must be preserved");
        assertEquals(0, new BigDecimal("135").compareTo(profile.getLongTermTargetGrams()));
        assertEquals(ProteinAdaptationStage.ADAPTING, profile.getAdaptationStage());
        assertEquals(ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT + 1,
                profile.getDaysAtCurrentTarget());
        assertEquals(ProteinToleranceStatus.GOOD, profile.getToleranceStatus());
        assertEquals(LAST_CHANGE, profile.getLastTargetChangeAt());
        verify(repository, never()).save(any());
    }

    private void assertBlocked(ProteinAdaptationResult r) {
        assertTrue(r.requiresClinicalReview(), "result must flag clinical review");
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
        assertEquals(0, new BigDecimal("110").compareTo(r.currentTargetGrams()));
        assertEquals(0, new BigDecimal("135").compareTo(r.longTermTargetGrams()));
        assertEquals(ProteinAdaptationStage.ADAPTING, r.adaptationStage());
        assertNull(r.nextEligibleIncreaseDate());
        assertStateUntouched();
    }

    // ── 1. Kidney disease ──────────────────────────────────────────────

    @Test
    void kidneyDisease_blocksProgression_andPreservesState() {
        setConditions(MedicalCondition.KIDNEY_DISEASE);

        assertBlocked(service.evaluate(profile));
    }

    // ── 2. Liver disease ───────────────────────────────────────────────

    @Test
    void liverDisease_blocksProgression_andPreservesState() {
        setConditions(MedicalCondition.LIVER_DISEASE);

        assertBlocked(service.evaluate(profile));
    }

    // ── 3. Both conditions together ────────────────────────────────────

    @Test
    void kidneyAndLiverTogether_blockProgression_andPreserveState() {
        setConditions(MedicalCondition.KIDNEY_DISEASE, MedicalCondition.LIVER_DISEASE);

        assertBlocked(service.evaluate(profile));
    }

    // ── 4. No relevant conditions ──────────────────────────────────────

    @Test
    void noConditions_progressesNormally() {
        ProteinAdaptationResult r = service.evaluate(profile);

        assertFalse(r.requiresClinicalReview());
        assertEquals(0, new BigDecimal("120").compareTo(r.currentTargetGrams()),
                "eligible profile advances by one 10 g step");
        assertEquals(0, new BigDecimal("120").compareTo(profile.getCurrentTargetGrams()));
        verify(repository).save(profile);
    }

    @Test
    void nonReviewConditionsOnly_progressNormally() {
        // Existing ProteinCalculator rule: these warn but do not require review.
        setConditions(MedicalCondition.HYPERTHYROID, MedicalCondition.CELIAC,
                MedicalCondition.DIABETES, MedicalCondition.FATTY_LIVER);

        ProteinAdaptationResult r = service.evaluate(profile);

        assertFalse(r.requiresClinicalReview());
        assertEquals(0, new BigDecimal("120").compareTo(r.currentTargetGrams()));
    }

    @Test
    void missingHealthProfileOrMedicalProfile_treatedAsNoConditions() {
        profile.setHealthProfile(null);
        assertFalse(service.evaluate(profile).requiresClinicalReview());

        ProteinAdaptationProfile noMedical = ProteinAdaptationProfile.builder()
                .healthProfile(HealthProfile.builder().build())
                .currentTargetGrams(new BigDecimal("110"))
                .longTermTargetGrams(new BigDecimal("135"))
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .daysAtCurrentTarget(8)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .lastTargetChangeAt(LAST_CHANGE)
                .build();
        assertFalse(service.evaluate(noMedical).requiresClinicalReview());
    }

    // ── 5. Condition removed after a previous clinical review ──────────

    @Test
    void conditionRemoved_progressionResumesFromPreservedState() {
        setConditions(MedicalCondition.KIDNEY_DISEASE);
        assertBlocked(service.evaluate(profile));

        // Clinician clears the condition
        medicalProfile.setMedicalConditions(new HashSet<>());

        ProteinAdaptationResult r = service.evaluate(profile);

        assertFalse(r.requiresClinicalReview());
        assertEquals(0, new BigDecimal("120").compareTo(r.currentTargetGrams()),
                "progress resumes from the preserved 110 g, not from a reset");
        assertEquals(0, profile.getDaysAtCurrentTarget(), "advanced: counter restarts");
    }

    // ── 6. Condition added after adaptation has started ────────────────

    @Test
    void conditionAddedMidAdaptation_blocksFurtherProgression() {
        // Adaptation proceeds normally first
        ProteinAdaptationResult first = service.evaluate(profile);
        assertEquals(0, new BigDecimal("120").compareTo(first.currentTargetGrams()));

        // Make the next step otherwise due, then a condition appears
        profile.setDaysAtCurrentTarget(ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT);
        profile.setToleranceStatus(ProteinToleranceStatus.GOOD);
        clearInvocations(repository);
        setConditions(MedicalCondition.LIVER_DISEASE);

        ProteinAdaptationResult second = service.evaluate(profile);

        assertTrue(second.requiresClinicalReview());
        assertEquals(0, new BigDecimal("120").compareTo(second.currentTargetGrams()),
                "target stays at the last approved step");
        assertEquals(0, new BigDecimal("120").compareTo(profile.getCurrentTargetGrams()));
        assertEquals(ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT,
                profile.getDaysAtCurrentTarget());
        verify(repository, never()).save(any());
    }

    // ── Guard ordering / other branches ────────────────────────────────

    @Test
    void clinicalReview_blocksEvenWhenProfileAlreadyAtTarget_withoutWriting() {
        profile.setCurrentTargetGrams(new BigDecimal("135"));
        profile.setAdaptationStage(ProteinAdaptationStage.ADAPTING);
        setConditions(MedicalCondition.KIDNEY_DISEASE);

        ProteinAdaptationResult r = service.evaluate(profile);

        assertTrue(r.requiresClinicalReview());
        assertEquals(ProteinAdaptationStage.ADAPTING, profile.getAdaptationStage(),
                "the 'already at target' branch must not run and rewrite the stage");
        verify(repository, never()).save(any());
    }

    @Test
    void explicitConditionsOverload_isHonoured() {
        // Caller-supplied (batch-loaded) conditions override the entity graph.
        ProteinAdaptationResult blocked =
                service.evaluate(profile, Set.of(MedicalCondition.KIDNEY_DISEASE));
        assertTrue(blocked.requiresClinicalReview());
        verify(repository, never()).save(any());

        ProteinAdaptationResult allowed = service.evaluate(profile, Set.of());
        assertFalse(allowed.requiresClinicalReview());
        assertEquals(0, new BigDecimal("120").compareTo(allowed.currentTargetGrams()));

        // null means "no conditions"
        profile.setDaysAtCurrentTarget(8);
        assertFalse(service.evaluate(profile, null).requiresClinicalReview());
    }

    // ── Single source of truth for the rule ────────────────────────────

    @Test
    void clinicalReviewConditionSet_matchesExistingRule() {
        assertEquals(
                EnumSet.of(MedicalCondition.KIDNEY_DISEASE, MedicalCondition.LIVER_DISEASE),
                ProteinCalculator.CLINICAL_REVIEW_CONDITIONS);

        for (MedicalCondition c : MedicalCondition.values()) {
            assertEquals(ProteinCalculator.requiresClinicalReview(c),
                    ProteinCalculator.CLINICAL_REVIEW_CONDITIONS.contains(c),
                    "set and rule must agree for " + c);
        }
    }

    // ── Batch resolver: one query, no N+1 ──────────────────────────────

    @Test
    void resolver_usesSingleQuery_withReviewConditionsOnly() {
        MedicalProfileRepository medicalRepo = mock(MedicalProfileRepository.class);
        ProteinClinicalReviewResolver resolver =
                new ProteinClinicalReviewResolver(medicalRepo);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(medicalRepo.findHealthProfileIdsWithAnyCondition(anyCollection(), anyCollection()))
                .thenReturn(List.of(b));

        Set<UUID> flagged = resolver.findRequiringClinicalReview(List.of(a, b, c));

        assertEquals(Set.of(b), flagged);
        verify(medicalRepo, times(1)).findHealthProfileIdsWithAnyCondition(
                List.of(a, b, c), ProteinCalculator.CLINICAL_REVIEW_CONDITIONS);
        verifyNoMoreInteractions(medicalRepo);
    }

    @Test
    void resolver_emptyOrNullInput_doesNotQuery() {
        MedicalProfileRepository medicalRepo = mock(MedicalProfileRepository.class);
        ProteinClinicalReviewResolver resolver =
                new ProteinClinicalReviewResolver(medicalRepo);

        assertTrue(resolver.findRequiringClinicalReview(List.of()).isEmpty());
        assertTrue(resolver.findRequiringClinicalReview(null).isEmpty());
        verifyNoInteractions(medicalRepo);
    }
}
