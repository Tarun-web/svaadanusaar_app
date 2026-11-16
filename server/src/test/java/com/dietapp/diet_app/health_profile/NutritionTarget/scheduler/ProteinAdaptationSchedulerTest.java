package com.dietapp.diet_app.health_profile.NutritionTarget.scheduler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ScheduledEvaluationOutcome;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinClinicalReviewResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProteinAdaptationScheduler}.
 *
 * <p>The repository is an in-memory fake that applies the same filters as the
 * JPQL query (stage, {@code lastEvaluatedOn}, keyset on id, page size) and the
 * real {@link ProteinAdaptationService} is used, so the ramp rules are the
 * production rules. <strong>No database is involved</strong>: the JPQL itself is
 * not exercised here.</p>
 */
class ProteinAdaptationSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final BigDecimal SECRET_INTAKE = new BigDecimal("123.45");
    private static final BigDecimal SECRET_TARGET = new BigDecimal("111.11");

    private final List<ProteinAdaptationProfile> store = new ArrayList<>();
    private final Map<UUID, BigDecimal> latestIntakeByHealthProfile = new java.util.HashMap<>();
    private final Set<UUID> clinicalReview = new HashSet<>();
    private final List<Integer> requestedPageSizes = new ArrayList<>();
    private final Set<UUID> failOnSaveWith = new HashSet<>();
    private final Set<UUID> conflictOnSave = new HashSet<>();

    private ProteinAdaptationProfileRepository repository;
    private ProteinClinicalReviewResolver resolver;
    private ProteinAdaptationService service;
    private ListAppender<ILoggingEvent> logs;
    private long idSeq = 0;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);
        resolver = mock(ProteinClinicalReviewResolver.class);

        when(repository.findEligibleForScheduledEvaluation(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    Collection<ProteinAdaptationStage> stages = inv.getArgument(0);
                    LocalDate today = inv.getArgument(1);
                    UUID afterId = inv.getArgument(2);
                    Pageable page = inv.getArgument(3);
                    requestedPageSizes.add(page.getPageSize());
                    return store.stream()
                            .filter(p -> stages.contains(p.getAdaptationStage()))
                            .filter(p -> p.getLastEvaluatedOn() == null
                                    || p.getLastEvaluatedOn().isBefore(today))
                            .filter(p -> p.getId().compareTo(afterId) > 0)
                            .sorted(Comparator.comparing(ProteinAdaptationProfile::getId))
                            .limit(page.getPageSize())
                            .map(p -> new EligibleProteinProfile(
                                    p, healthIdOf(p),
                                    latestIntakeByHealthProfile.get(healthIdOf(p))))
                            .toList();
                });

        when(repository.save(any(ProteinAdaptationProfile.class))).thenAnswer(inv -> {
            ProteinAdaptationProfile p = inv.getArgument(0);
            if (conflictOnSave.contains(p.getId())) {
                throw new ObjectOptimisticLockingFailureException(
                        ProteinAdaptationProfile.class, p.getId());
            }
            if (failOnSaveWith.contains(p.getId())) {
                throw new IllegalStateException("simulated failure for " + SECRET_INTAKE);
            }
            return p;
        });

        when(resolver.findRequiringClinicalReview(anyCollection())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            Set<UUID> hit = new HashSet<>(ids);
            hit.retainAll(clinicalReview);
            return hit;
        });

        service = new ProteinAdaptationService(repository);

        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(ProteinAdaptationScheduler.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(ProteinAdaptationScheduler.class)).detachAppender(logs);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private final Map<UUID, UUID> healthIds = new java.util.HashMap<>();

    private UUID healthIdOf(ProteinAdaptationProfile p) {
        return healthIds.get(p.getId());
    }

    private ProteinAdaptationScheduler scheduler(int batchSize) {
        return new ProteinAdaptationScheduler(repository, service, resolver,
                Clock.fixed(Instant.parse("2026-03-10T05:00:00Z"), ZoneId.of("UTC")),
                batchSize);
    }

    private ProteinAdaptationProfile add(
            ProteinAdaptationStage stage,
            int days,
            String current,
            String longTerm,
            ProteinToleranceStatus tolerance,
            LocalDate lastEvaluatedOn) {

        ProteinAdaptationProfile p = ProteinAdaptationProfile.builder()
                .currentProteinGrams(SECRET_INTAKE)
                .currentTargetGrams(new BigDecimal(current))
                .longTermTargetGrams(new BigDecimal(longTerm))
                .adaptationStage(stage)
                .daysAtCurrentTarget(days)
                .toleranceStatus(tolerance)
                .lastTargetChangeAt(TODAY.minusDays(20))
                .lastEvaluatedOn(lastEvaluatedOn)
                .build();
        p.setId(new UUID(0L, ++idSeq));
        healthIds.put(p.getId(), new UUID(1L, idSeq));
        store.add(p);
        return p;
    }

    private ProteinAdaptationProfile ramping(int days) {
        return add(ProteinAdaptationStage.ADAPTING, days, "110", "135",
                ProteinToleranceStatus.GOOD, null);
    }

    private void assertGrams(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    // ── eligibility ────────────────────────────────────────────────────

    @Test
    void startingAndAdaptingProfiles_areProcessed() {
        ProteinAdaptationProfile a = add(ProteinAdaptationStage.STARTING, 0, "100", "135",
                ProteinToleranceStatus.UNKNOWN, null);
        ProteinAdaptationProfile b = ramping(1);

        ProteinAdaptationRunSummary s = scheduler(50).run(TODAY);

        assertEquals(2, s.examined());
        assertEquals(1, a.getDaysAtCurrentTarget());
        assertEquals(2, b.getDaysAtCurrentTarget());
        assertEquals(TODAY, a.getLastEvaluatedOn());
        assertEquals(TODAY, b.getLastEvaluatedOn());
        assertEquals(0, s.failures());
        assertEquals(0, s.conflicts());
    }

    @Test
    void targetReachedAndNotStarted_areNeverLoadedOrChanged() {
        ProteinAdaptationProfile reached = add(ProteinAdaptationStage.TARGET_REACHED, 3,
                "135", "135", ProteinToleranceStatus.GOOD, null);
        ProteinAdaptationProfile notStarted = add(ProteinAdaptationStage.NOT_STARTED, 0,
                "135", "135", ProteinToleranceStatus.UNKNOWN, null);

        ProteinAdaptationRunSummary s = scheduler(50).run(TODAY);

        assertEquals(0, s.examined());
        assertEquals(3, reached.getDaysAtCurrentTarget());
        assertNull(reached.getLastEvaluatedOn());
        assertEquals(0, notStarted.getDaysAtCurrentTarget());
        assertNull(notStarted.getLastEvaluatedOn());
        verify(repository, never()).save(any());
    }

    @Test
    void eligibleStages_areExactlyStartingAndAdapting() {
        assertEquals(Set.of(ProteinAdaptationStage.STARTING, ProteinAdaptationStage.ADAPTING),
                ProteinAdaptationScheduler.ELIGIBLE_STAGES);
    }

    @Test
    void emptyDatabase_isANoOp() {
        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(0, s.examined());
        assertTrue(s.outcomes().isEmpty());
        verifyNoInteractions(resolver);
    }

    // ── ramp rules (delegated to the existing service) ─────────────────

    @Test
    void seventhEligibleDay_advancesByTenGrams() {
        ProteinAdaptationProfile p = ramping(ProteinAdaptationService.MIN_DAYS_BEFORE_INCREMENT - 1);

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(1, s.outcomes().get(ScheduledEvaluationOutcome.TARGET_ADVANCED));
        assertGrams("120", p.getCurrentTargetGrams());
        assertEquals(0, p.getDaysAtCurrentTarget());
        assertEquals(TODAY, p.getLastEvaluatedOn());
    }

    @Test
    void sixthDay_doesNotAdvance() {
        ProteinAdaptationProfile p = ramping(4);

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(1, s.outcomes().get(ScheduledEvaluationOutcome.DAY_COUNTED));
        assertEquals(5, p.getDaysAtCurrentTarget());
        assertGrams("110", p.getCurrentTargetGrams());
    }

    @Test
    void increase_isClampedToLongTermTarget_andReachesTargetReached() {
        ProteinAdaptationProfile p = add(ProteinAdaptationStage.ADAPTING, 6, "130", "135",
                ProteinToleranceStatus.GOOD, null);

        scheduler(10).run(TODAY);

        assertGrams("135", p.getCurrentTargetGrams());
        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
    }

    @Test
    void afterTargetReached_nextRunsDoNotChangeAnything() {
        ProteinAdaptationProfile p = add(ProteinAdaptationStage.ADAPTING, 6, "130", "135",
                ProteinToleranceStatus.GOOD, null);
        ProteinAdaptationScheduler s = scheduler(10);
        s.run(TODAY);

        ProteinAdaptationRunSummary next = s.run(TODAY.plusDays(1));

        assertEquals(0, next.examined());
        assertGrams("135", p.getCurrentTargetGrams());
        assertEquals(ProteinAdaptationStage.TARGET_REACHED, p.getAdaptationStage());
    }

    @Test
    void mildAndSignificantTolerance_holdProgression_butProfilesAreStillSelected() {
        ProteinAdaptationProfile mild = add(ProteinAdaptationStage.ADAPTING, 6, "110", "135",
                ProteinToleranceStatus.MILD_ISSUES, null);
        ProteinAdaptationProfile significant = add(ProteinAdaptationStage.ADAPTING, 6, "110",
                "135", ProteinToleranceStatus.SIGNIFICANT_ISSUES, null);

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(2, s.outcomes().get(ScheduledEvaluationOutcome.HELD_TOLERANCE));
        for (ProteinAdaptationProfile p : List.of(mild, significant)) {
            assertEquals(6, p.getDaysAtCurrentTarget());
            assertGrams("110", p.getCurrentTargetGrams());
            assertNull(p.getLastEvaluatedOn());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void clinicalReviewProfiles_areSkippedAndUnchanged_othersStillProcessed() {
        ProteinAdaptationProfile review = ramping(6);
        ProteinAdaptationProfile normal = ramping(6);
        clinicalReview.add(healthIdOf(review));

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(1, s.outcomes().get(ScheduledEvaluationOutcome.SKIPPED_CLINICAL_REVIEW));
        assertEquals(1, s.outcomes().get(ScheduledEvaluationOutcome.TARGET_ADVANCED));
        assertEquals(6, review.getDaysAtCurrentTarget());
        assertGrams("110", review.getCurrentTargetGrams());
        assertNull(review.getLastEvaluatedOn());
        assertGrams("120", normal.getCurrentTargetGrams());
    }

    // ── idempotency ────────────────────────────────────────────────────

    @Test
    void secondRunOnSameDay_doesNotCountAnotherDay() {
        ProteinAdaptationProfile p = ramping(2);
        ProteinAdaptationScheduler s = scheduler(10);

        ProteinAdaptationRunSummary first = s.run(TODAY);
        ProteinAdaptationRunSummary second = s.run(TODAY);

        assertEquals(1, first.examined());
        assertEquals(0, second.examined());
        assertEquals(3, p.getDaysAtCurrentTarget());
        verify(repository, times(1)).save(p);
    }

    @Test
    void secondRunOnSameDay_doesNotAdvanceTargetTwice() {
        ProteinAdaptationProfile p = ramping(6);
        ProteinAdaptationScheduler s = scheduler(10);

        s.run(TODAY);
        s.run(TODAY);

        assertGrams("120", p.getCurrentTargetGrams());
        assertEquals(0, p.getDaysAtCurrentTarget());
    }

    @Test
    void nextCalendarDay_countsAgain() {
        ProteinAdaptationProfile p = ramping(0);
        ProteinAdaptationScheduler s = scheduler(10);

        s.run(TODAY);
        s.run(TODAY.plusDays(1));

        assertEquals(2, p.getDaysAtCurrentTarget());
    }

    @Test
    void markerFromTheFuture_isNeverCountedAgain() {
        ProteinAdaptationProfile p = add(ProteinAdaptationStage.ADAPTING, 2, "110", "135",
                ProteinToleranceStatus.GOOD, TODAY.plusDays(1));

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(0, s.examined());
        assertEquals(2, p.getDaysAtCurrentTarget());
    }

    // ── concurrency / failure isolation ────────────────────────────────

    @Test
    void optimisticLockConflict_isCountedAsConflict_andOthersContinue() {
        ProteinAdaptationProfile conflicted = ramping(2);
        ProteinAdaptationProfile ok = ramping(2);
        conflictOnSave.add(conflicted.getId());

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(1, s.conflicts());
        assertEquals(0, s.failures());
        assertEquals(1, s.outcomes().get(ScheduledEvaluationOutcome.DAY_COUNTED));
        assertEquals(3, ok.getDaysAtCurrentTarget());
    }

    @Test
    void conflictedProfile_isNotRetriedWithinTheSameRun() {
        ProteinAdaptationProfile conflicted = ramping(2);
        conflictOnSave.add(conflicted.getId());

        scheduler(10).run(TODAY);

        verify(repository, times(1)).save(conflicted);
    }

    @Test
    void secondInstanceRunningAfterFirst_findsNothingToDo() {
        // Models two scheduler instances: the loser either loses the version
        // race (conflict, covered above) or arrives after the winner committed
        // and then sees nothing eligible.
        ProteinAdaptationProfile p = ramping(2);
        ProteinAdaptationScheduler instanceA = scheduler(10);
        ProteinAdaptationScheduler instanceB = scheduler(10);

        instanceA.run(TODAY);
        ProteinAdaptationRunSummary b = instanceB.run(TODAY);

        assertEquals(0, b.examined());
        assertEquals(3, p.getDaysAtCurrentTarget());
    }

    @Test
    void oneFailure_doesNotAffectOtherProfiles_andIsCounted() {
        ProteinAdaptationProfile bad = ramping(2);
        ProteinAdaptationProfile good1 = ramping(2);
        ProteinAdaptationProfile good2 = ramping(2);
        failOnSaveWith.add(bad.getId());

        ProteinAdaptationRunSummary s = scheduler(10).run(TODAY);

        assertEquals(1, s.failures());
        assertEquals(0, s.conflicts());
        assertEquals(2, s.outcomes().get(ScheduledEvaluationOutcome.DAY_COUNTED));
        assertEquals(3, good1.getDaysAtCurrentTarget());
        assertEquals(3, good2.getDaysAtCurrentTarget());
    }

    @Test
    void runDaily_swallowsSelectionFailure() {
        doThrow(new IllegalStateException("db down"))
                .when(repository)
                .findEligibleForScheduledEvaluation(any(), any(), any(), any());

        assertDoesNotThrow(() -> scheduler(10).runDaily());
    }

    // ── batching ───────────────────────────────────────────────────────

    @Test
    void largeSet_isProcessedInKeysetBatches_withoutSkippingOrRepeating() {
        List<ProteinAdaptationProfile> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            all.add(ramping(0));
        }

        ProteinAdaptationRunSummary s = scheduler(3).run(TODAY);

        assertEquals(7, s.examined());
        all.forEach(p -> assertEquals(1, p.getDaysAtCurrentTarget()));
        verify(repository, times(7)).save(any());
        // 3 + 3 + 1: the short last page ends the loop without an extra query.
        assertEquals(List.of(3, 3, 3), requestedPageSizes);
        verify(resolver, times(3)).findRequiringClinicalReview(anyCollection());
    }

    @Test
    void exactMultipleOfBatchSize_endsWithOneEmptyQuery() {
        for (int i = 0; i < 6; i++) {
            ramping(0);
        }

        ProteinAdaptationRunSummary s = scheduler(3).run(TODAY);

        assertEquals(6, s.examined());
        assertEquals(3, requestedPageSizes.size());
    }

    @Test
    void clinicalReview_isResolvedOncePerBatch_notPerProfile() {
        for (int i = 0; i < 5; i++) {
            ramping(0);
        }

        scheduler(5).run(TODAY);

        verify(resolver, times(1)).findRequiringClinicalReview(anyCollection());
    }

    // ── latest intake handling ─────────────────────────────────────────

    @Test
    void latestIntake_neverChangesStoredBaselineOrProgress() {
        ProteinAdaptationProfile p = ramping(2);
        latestIntakeByHealthProfile.put(healthIdOf(p), new BigDecimal("40"));

        scheduler(10).run(TODAY);

        assertGrams("123.45", p.getCurrentProteinGrams());
        assertEquals(3, p.getDaysAtCurrentTarget());
        assertGrams("110", p.getCurrentTargetGrams());
    }

    // ── time zone ──────────────────────────────────────────────────────

    @Test
    void runDaily_usesTheConfiguredZone_notTheJvmDefault() {
        // 19:00 UTC on 1 March is already 2 March in Asia/Kolkata (+05:30).
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T19:00:00Z"), ZoneId.of("Asia/Kolkata"));
        ProteinAdaptationScheduler s = new ProteinAdaptationScheduler(
                repository, service, resolver, clock, 10);
        ProteinAdaptationProfile p = ramping(0);

        s.runDaily();

        assertEquals(LocalDate.of(2026, 3, 2), p.getLastEvaluatedOn());
    }

    @Test
    void publicConstructor_buildsClockFromTimezoneId() {
        ProteinAdaptationScheduler s = new ProteinAdaptationScheduler(
                repository, service, resolver, "Asia/Kolkata", 10);

        assertNotNull(s);
    }

    @Test
    void publicConstructor_rejectsUnknownTimezone() {
        assertThrows(RuntimeException.class, () -> new ProteinAdaptationScheduler(
                repository, service, resolver, "Not/AZone", 10));
    }

    @Test
    void constructor_rejectsBatchSizeBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> scheduler(0));
        assertThrows(IllegalArgumentException.class, () -> scheduler(-5));
    }

    @Test
    void runDaily_isScheduledWithExplicitZoneAndConfigurableCron() throws Exception {
        Scheduled ann = ProteinAdaptationScheduler.class
                .getMethod("runDaily").getAnnotation(Scheduled.class);

        assertNotNull(ann);
        assertEquals("${app.timezone}", ann.zone());
        assertTrue(ann.cron().contains("app.scheduler.protein-adaptation.cron"));
    }

    // ── logging / privacy ──────────────────────────────────────────────

    @Test
    void logs_doNotContainHealthData() {
        ProteinAdaptationProfile ok = add(ProteinAdaptationStage.ADAPTING, 2,
                SECRET_TARGET.toPlainString(), "135", ProteinToleranceStatus.GOOD, null);
        ProteinAdaptationProfile conflicted = add(ProteinAdaptationStage.ADAPTING, 2,
                SECRET_TARGET.toPlainString(), "135", ProteinToleranceStatus.GOOD, null);
        ProteinAdaptationProfile failing = add(ProteinAdaptationStage.ADAPTING, 2,
                SECRET_TARGET.toPlainString(), "135", ProteinToleranceStatus.GOOD, null);
        conflictOnSave.add(conflicted.getId());
        failOnSaveWith.add(failing.getId());
        latestIntakeByHealthProfile.put(healthIdOf(ok), SECRET_INTAKE);

        scheduler(10).run(TODAY);

        assertFalse(logs.list.isEmpty());
        for (ILoggingEvent e : logs.list) {
            String text = e.getFormattedMessage();
            assertFalse(text.contains("123.45"), text);
            assertFalse(text.contains("111.11"), text);
            assertNull(e.getThrowableProxy(), "no stack traces (exception messages may hold data)");
        }
    }
}
