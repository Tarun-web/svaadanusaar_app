package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.ScheduledEvaluationResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinWarningCode;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the idempotent scheduled tick
 * {@link ProteinAdaptationService#evaluateScheduled}.
 */
class ProteinScheduledEvaluationTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 3, 1);
    private static final int MIN_DAYS = ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT;

    private ProteinAdaptationProfileRepository repository;
    private ProteinAdaptationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);
        when(repository.save(any(ProteinAdaptationProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        service = new ProteinAdaptationService(repository);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private ProteinAdaptationProfile profile(
            String current,
            String longTerm,
            ProteinAdaptationStage stage,
            int days,
            ProteinToleranceStatus tolerance,
            LocalDate lastEvaluatedOn) {

        return ProteinAdaptationProfile.builder()
                .currentTargetGrams(new BigDecimal(current))
                .longTermTargetGrams(new BigDecimal(longTerm))
                .adaptationStage(stage)
                .daysAtCurrentTarget(days)
                .toleranceStatus(tolerance)
                .lastTargetChangeAt(DAY_1.minusDays(30))
                .lastEvaluatedOn(lastEvaluatedOn)
                .build();
    }

    private ProteinAdaptationProfile ramping(int days, LocalDate lastEvaluatedOn) {
        return profile("110", "135", ProteinAdaptationStage.ADAPTING, days,
                ProteinToleranceStatus.GOOD, lastEvaluatedOn);
    }

    private void assertGrams(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    // ── 1. First evaluation on a date ──────────────────────────────────

    @Test
    void firstEvaluation_countsOneDay_andStampsMarker_inOneSave() {
        ProteinAdaptationProfile p = ramping(2, DAY_1.minusDays(1));

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, r.outcome());
        assertEquals(3, p.getDaysAtCurrentTarget());
        assertEquals(DAY_1, p.getLastEvaluatedOn());
        assertGrams("110", p.getCurrentTargetGrams());
        verify(repository, times(1)).save(p);
    }

    // ── 2. Repeated evaluation on the same date ────────────────────────

    @Test
    void repeatedEvaluationSameDate_doesNotCountAgain() {
        ProteinAdaptationProfile p = ramping(2, DAY_1.minusDays(1));

        service.evaluateScheduled(p, Set.of(), DAY_1);
        clearInvocations(repository);
        ScheduledEvaluationResult second = service.evaluateScheduled(p, Set.of(), DAY_1);
        ScheduledEvaluationResult third = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY, second.outcome());
        assertEquals(ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY, third.outcome());
        assertEquals(3, p.getDaysAtCurrentTarget(), "counter must not move again");
        assertEquals(DAY_1, p.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    @Test
    void markerInTheFuture_isTreatedAsAlreadyEvaluated() {
        ProteinAdaptationProfile p = ramping(2, DAY_1.plusDays(3));

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY, r.outcome());
        assertEquals(2, p.getDaysAtCurrentTarget());
        verify(repository, never()).save(any());
    }

    // ── 3. Evaluation on the following date ────────────────────────────

    @Test
    void nextDay_countsAgain() {
        ProteinAdaptationProfile p = ramping(2, DAY_1.minusDays(1));

        service.evaluateScheduled(p, Set.of(), DAY_1);
        ScheduledEvaluationResult next =
                service.evaluateScheduled(p, Set.of(), DAY_1.plusDays(1));

        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, next.outcome());
        assertEquals(4, p.getDaysAtCurrentTarget());
        assertEquals(DAY_1.plusDays(1), p.getLastEvaluatedOn());
    }

    // ── 4. Seven-day eligibility ───────────────────────────────────────

    @Test
    void sevenConsecutiveDays_advanceOnlyOnTheSeventh() {
        ProteinAdaptationProfile p = ramping(0, null);

        for (int i = 1; i < MIN_DAYS; i++) {
            ScheduledEvaluationResult r =
                    service.evaluateScheduled(p, Set.of(), DAY_1.plusDays(i));
            assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, r.outcome(), "day " + i);
            assertGrams("110", p.getCurrentTargetGrams());
        }
        assertEquals(MIN_DAYS - 1, p.getDaysAtCurrentTarget());

        ScheduledEvaluationResult seventh =
                service.evaluateScheduled(p, Set.of(), DAY_1.plusDays(MIN_DAYS));

        assertEquals(ScheduledEvaluationOutcome.TARGET_ADVANCED, seventh.outcome());
        assertGrams("120", p.getCurrentTargetGrams());
        assertEquals(0, p.getDaysAtCurrentTarget(), "counter restarts after a step");
        assertEquals(ProteinToleranceStatus.UNKNOWN, p.getToleranceStatus());
        assertEquals(DAY_1.plusDays(MIN_DAYS), p.getLastEvaluatedOn());
        assertEquals(DAY_1.plusDays(MIN_DAYS), p.getLastTargetChangeAt());
    }

    @Test
    void advance_isAppliedOnlyOnce_evenIfTickedTwiceOnThatDay() {
        ProteinAdaptationProfile p = ramping(MIN_DAYS - 1, DAY_1.minusDays(1));

        service.evaluateScheduled(p, Set.of(), DAY_1);
        service.evaluateScheduled(p, Set.of(), DAY_1);

        assertGrams("120", p.getCurrentTargetGrams());
        assertEquals(0, p.getDaysAtCurrentTarget());
    }

    @Test
    void advance_neverExceedsLongTermTarget_andReachesTargetStage() {
        ProteinAdaptationProfile p = profile("130", "135",
                ProteinAdaptationStage.ADAPTING, MIN_DAYS - 1,
                ProteinToleranceStatus.GOOD, DAY_1.minusDays(1));

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.TARGET_ADVANCED, r.outcome());
        assertGrams("135", p.getCurrentTargetGrams());
        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
        assertFalse(r.adaptation().rampUpRequired());
    }

    @Test
    void unknownTolerance_countsLikeGood() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.STARTING, 0,
                ProteinToleranceStatus.UNKNOWN, null);

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, r.outcome());
        assertEquals(1, p.getDaysAtCurrentTarget());
    }

    // ── 5. Clinical-review exclusion ───────────────────────────────────

    @Test
    void clinicalReview_blocksCounting_andPreservesEverything() {
        for (MedicalCondition c :
                new MedicalCondition[]{MedicalCondition.KIDNEY_DISEASE, MedicalCondition.LIVER_DISEASE}) {

            ProteinAdaptationProfile p = ramping(MIN_DAYS - 1, DAY_1.minusDays(1));

            ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(c), DAY_1);

            assertEquals(ScheduledEvaluationOutcome.SKIPPED_CLINICAL_REVIEW, r.outcome(), c.name());
            assertTrue(r.adaptation().requiresClinicalReview());
            assertTrue(r.adaptation().warnings().stream()
                    .anyMatch(w -> w.code() == ProteinWarningCode.CLINICAL_REVIEW_REQUIRED));
            assertEquals(MIN_DAYS - 1, p.getDaysAtCurrentTarget());
            assertGrams("110", p.getCurrentTargetGrams());
            assertEquals(ProteinAdaptationStage.ADAPTING, p.getAdaptationStage());
            assertEquals(ProteinToleranceStatus.GOOD, p.getToleranceStatus());
            assertEquals(DAY_1.minusDays(1), p.getLastEvaluatedOn(), "marker not stamped");
        }
        verify(repository, never()).save(any());
    }

    @Test
    void clinicalReview_cleared_resumesFromPreservedState() {
        ProteinAdaptationProfile p = ramping(MIN_DAYS - 1, DAY_1.minusDays(1));

        service.evaluateScheduled(p, Set.of(MedicalCondition.KIDNEY_DISEASE), DAY_1);
        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.TARGET_ADVANCED, r.outcome(),
                "the blocked tick must not have consumed today");
        assertGrams("120", p.getCurrentTargetGrams());
    }

    @Test
    void nonReviewConditions_doNotBlock() {
        ProteinAdaptationProfile p = ramping(0, null);

        ScheduledEvaluationResult r = service.evaluateScheduled(
                p, Set.of(MedicalCondition.HYPERTHYROID, MedicalCondition.CELIAC), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, r.outcome());
    }

    // ── 6. Mild tolerance hold ─────────────────────────────────────────

    @Test
    void mildIssues_doNotAccumulateDays_orWrite() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.ADAPTING, 3,
                ProteinToleranceStatus.MILD_ISSUES, DAY_1.minusDays(1));

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.HELD_TOLERANCE, r.outcome());
        assertTrue(r.adaptation().warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.TOLERANCE_HOLD));
        assertEquals(3, p.getDaysAtCurrentTarget());
        assertGrams("110", p.getCurrentTargetGrams());
        assertEquals(ProteinToleranceStatus.MILD_ISSUES, p.getToleranceStatus());
        assertEquals(DAY_1.minusDays(1), p.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    // ── 7. Significant tolerance hold ──────────────────────────────────

    @Test
    void significantIssues_doNotAccumulateDays_orWrite() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.ADAPTING, MIN_DAYS + 5,
                ProteinToleranceStatus.SIGNIFICANT_ISSUES, null);

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.HELD_TOLERANCE, r.outcome());
        assertTrue(r.adaptation().warnings().stream()
                .anyMatch(w -> w.code() == ProteinWarningCode.SIGNIFICANT_GI_SYMPTOMS));
        assertEquals(MIN_DAYS + 5, p.getDaysAtCurrentTarget());
        assertGrams("110", p.getCurrentTargetGrams());
        assertNull(p.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    @Test
    void toleranceRecovered_countingResumes() {
        ProteinAdaptationProfile p = profile("110", "135",
                ProteinAdaptationStage.ADAPTING, 3,
                ProteinToleranceStatus.MILD_ISSUES, null);
        service.evaluateScheduled(p, Set.of(), DAY_1);
        assertEquals(3, p.getDaysAtCurrentTarget());

        p.setToleranceStatus(ProteinToleranceStatus.GOOD);
        service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(4, p.getDaysAtCurrentTarget());
    }

    // ── 8. NOT_STARTED ─────────────────────────────────────────────────

    @Test
    void notStarted_isNotCounted() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.NOT_STARTED, 0,
                ProteinToleranceStatus.UNKNOWN, null);

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.SKIPPED_NOT_RAMPING, r.outcome());
        assertEquals(0, p.getDaysAtCurrentTarget());
        assertNull(p.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    // ── 9. TARGET_REACHED ──────────────────────────────────────────────

    @Test
    void targetReached_isNotCounted() {
        ProteinAdaptationProfile p = profile("135", "135",
                ProteinAdaptationStage.TARGET_REACHED, 4,
                ProteinToleranceStatus.GOOD, null);

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.SKIPPED_NOT_RAMPING, r.outcome());
        assertEquals(4, p.getDaysAtCurrentTarget());
        assertGrams("135", p.getCurrentTargetGrams());
        assertNull(p.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    // ── 10. Concurrent update / optimistic locking ─────────────────────

    @Test
    void concurrentTick_optimisticLockFailure_propagates_andLeavesNoSecondIncrement() {
        // Two workers load the same row (version N) and both pass the
        // "lastEvaluatedOn < today" check. The first commit wins; the second
        // save is rejected by @Version.
        ProteinAdaptationProfile workerA = ramping(2, DAY_1.minusDays(1));
        ProteinAdaptationProfile workerB = ramping(2, DAY_1.minusDays(1));

        when(repository.save(workerB)).thenThrow(
                new ObjectOptimisticLockingFailureException(
                        ProteinAdaptationProfile.class, "profile-id"));

        ScheduledEvaluationResult a = service.evaluateScheduled(workerA, Set.of(), DAY_1);
        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, a.outcome());

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> service.evaluateScheduled(workerB, Set.of(), DAY_1));

        // The row that committed holds exactly one increment for DAY_1.
        assertEquals(3, workerA.getDaysAtCurrentTarget());
        assertEquals(DAY_1, workerA.getLastEvaluatedOn());
    }

    @Test
    void loserOfTheRace_reloadsAndIsSkipped() {
        // After the winner commits, a reloaded copy carries the new marker,
        // so a retry by the loser is a no-op.
        ProteinAdaptationProfile reloaded = ramping(3, DAY_1);

        ScheduledEvaluationResult r = service.evaluateScheduled(reloaded, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY, r.outcome());
        assertEquals(3, reloaded.getDaysAtCurrentTarget());
        verify(repository, never()).save(any());
    }

    // ── 11. Existing records with lastEvaluatedOn = null ───────────────

    @Test
    void existingRecordWithNullMarker_isEligible_andGetsStamped() {
        ProteinAdaptationProfile p = ramping(5, null);
        assertNull(p.getLastEvaluatedOn());

        ScheduledEvaluationResult r = service.evaluateScheduled(p, Set.of(), DAY_1);

        assertEquals(ScheduledEvaluationOutcome.DAY_COUNTED, r.outcome());
        assertEquals(6, p.getDaysAtCurrentTarget());
        assertEquals(DAY_1, p.getLastEvaluatedOn());
    }

    @Test
    void builderDefault_leavesMarkerNull() {
        ProteinAdaptationProfile fresh = ProteinAdaptationProfile.builder().build();
        assertNull(fresh.getLastEvaluatedOn());
    }

    // ── Manual evaluate() stays independent of the marker ──────────────

    @Test
    void manualEvaluate_doesNotCountDays_orTouchMarker() {
        ProteinAdaptationProfile p = ramping(2, null);

        service.evaluate(p);
        service.evaluate(p);

        assertEquals(2, p.getDaysAtCurrentTarget());
        assertNull(p.getLastEvaluatedOn());
    }

    @Test
    void manualEvaluate_stillAdvancesWhenEligible() {
        ProteinAdaptationProfile p = ramping(MIN_DAYS, null);

        service.evaluate(p);

        assertGrams("120", p.getCurrentTargetGrams());
        assertEquals(0, p.getDaysAtCurrentTarget());
        verify(repository).save(p);
    }
}
