package com.dietapp.diet_app.health_profile.NutritionTarget.scheduler;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.NutritionTargetServiceImpl;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinClinicalReviewResolver;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.*;
import com.dietapp.diet_app.health_profile.entity.*;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The ten end-to-end Nutrition Engine v1 scenarios.
 *
 * <p>What runs for real: {@link NutritionTargetServiceImpl},
 * {@link MacroCalculator}, {@link ProteinCalculator},
 * {@link ProteinAdaptationService} and {@link ProteinAdaptationScheduler}.
 * What is faked: the persistence boundary only - an in-memory table that hands
 * out <em>detached copies</em> and emulates {@code @Version} checking on save
 * (so lost updates are detected as in Hibernate). The calorie/fat/carb/fibre/
 * water calculators are stubbed with fixed values because they are outside the
 * protein-adaptation behaviour under test.</p>
 *
 * <p><strong>This is not PostgreSQL verification.</strong> At 75 kg and
 * FAT_LOSS the long-term protein target is 120 g; with 70 g reported the gap is
 * 50 g so the ramp starts at 100 g.</p>
 */
class ProteinEndToEndScenariosTest {

    private static final LocalDate BASE = LocalDate.now();

    /**
     * PostgreSQL orders uuid values as unsigned bytes; java.util.UUID.compareTo
     * compares the two halves as SIGNED longs. The fake must follow the
     * database, otherwise the scheduler's all-zero starting key is not the minimum.
     */
    private static final Comparator<UUID> UNSIGNED_UUID = (x, y) -> {
        int hi = Long.compareUnsigned(x.getMostSignificantBits(), y.getMostSignificantBits());
        return hi != 0 ? hi
                : Long.compareUnsigned(x.getLeastSignificantBits(), y.getLeastSignificantBits());
    };

    private final Map<UUID, ProteinAdaptationProfile> rows = new LinkedHashMap<>();
    private volatile CyclicBarrier raceBarrier;
    private final ThreadLocal<Boolean> alreadyRaced = ThreadLocal.withInitial(() -> false);

    private HealthProfile healthProfile;
    private PersonalProfile personalProfile;
    private NutritionTargetServiceImpl service;
    private ProteinAdaptationService adaptationService;
    private ProteinAdaptationProfileRepository repository;
    private ProteinAdaptationScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);

        when(repository.findByHealthProfile(any())).thenAnswer(inv -> {
            synchronized (rows) {
                return rows.values().stream().findFirst().map(this::copy);
            }
        });
        when(repository.save(any(ProteinAdaptationProfile.class))).thenAnswer(inv -> save(inv.getArgument(0)));
        when(repository.findEligibleForScheduledEvaluation(any(), any(), any(), any())).thenAnswer(inv -> {
            Collection<ProteinAdaptationStage> stages = inv.getArgument(0);
            LocalDate today = inv.getArgument(1);
            UUID afterId = inv.getArgument(2);
            org.springframework.data.domain.Pageable page = inv.getArgument(3);
            List<EligibleProteinProfile> out;
            synchronized (rows) {
                out = rows.values().stream()
                        .filter(p -> stages.contains(p.getAdaptationStage()))
                        .filter(p -> p.getLastEvaluatedOn() == null || p.getLastEvaluatedOn().isBefore(today))
                        .filter(p -> UNSIGNED_UUID.compare(p.getId(), afterId) > 0)
                        .sorted(Comparator.comparing(ProteinAdaptationProfile::getId, UNSIGNED_UUID))
                        .limit(page.getPageSize())
                        .map(p -> new EligibleProteinProfile(copy(p), healthProfile.getId(), latestIntake()))
                        .toList();
            }
            awaitRaceOnce(out);
            return out;
        });

        ProteinClinicalReviewResolver resolver = mock(ProteinClinicalReviewResolver.class);
        when(resolver.findRequiringClinicalReview(anyCollection())).thenAnswer(inv -> {
            Set<MedicalCondition> c = healthProfile.getMedicalProfile() == null
                    ? Set.of() : healthProfile.getMedicalProfile().getMedicalConditions();
            return ProteinCalculator.anyRequireClinicalReview(c)
                    ? Set.of(healthProfile.getId()) : Set.<UUID>of();
        });

        adaptationService = new ProteinAdaptationService(repository);
        MacroCalculator macro = new MacroCalculator(
                new ProteinCalculator(), adaptationService, stubFat(), stubCarbs());

        personalProfile = PersonalProfile.builder()
                .weightKg(new BigDecimal("75")).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile goal = FitnessGoalProfile.builder().primaryGoal(Goal.FAT_LOSS).build();
        healthProfile = HealthProfile.builder().build();
        healthProfile.setId(UUID.randomUUID());
        personalProfile.setHealthProfile(healthProfile);
        goal.setHealthProfile(healthProfile);
        healthProfile.setPersonalProfile(personalProfile);
        healthProfile.setFitnessGoalProfile(goal);

        HealthProfileContextService context = mock(HealthProfileContextService.class);
        when(context.getCurrentUserHealthProfile()).thenReturn(healthProfile);
        GoalCalorieCalculator goalCalc = mock(GoalCalorieCalculator.class);
        when(goalCalc.calculate(any(), any(), any())).thenReturn(CalorieTargetResult.builder()
                .tdee(new BigDecimal("2200")).proposedCalories(new BigDecimal("1700"))
                .goal(Goal.FAT_LOSS).build());
        CalorieSafetyEngine safety = mock(CalorieSafetyEngine.class);
        when(safety.evaluate(any(), any(), any(), any())).thenReturn(new CalorieSafetyResult(
                new BigDecimal("1700"), new BigDecimal("1700"), CalorieSafetyStatus.SAFE,
                false, false, Collections.emptyList()));
        FibreCalculator fibre = mock(FibreCalculator.class);
        when(fibre.calculate(any())).thenReturn(new BigDecimal("23.80"));
        WaterCalculator water = mock(WaterCalculator.class);
        when(water.calculate(any())).thenReturn(new WaterCalculationResult(
                new BigDecimal("2630"), new BigDecimal("2625"), BigDecimal.ZERO, BigDecimal.ZERO,
                true, Collections.emptyList()));

        service = new NutritionTargetServiceImpl(
                context, goalCalc, safety, macro, fibre, water, repository);
        scheduler = newScheduler();
    }

    // ── fake persistence ───────────────────────────────────────────────

    private ProteinAdaptationScheduler newScheduler() {
        return new ProteinAdaptationScheduler(repository, adaptationService, resolverMock(),
                Clock.system(ZoneId.of("UTC")), 50);
    }

    private ProteinClinicalReviewResolver resolverMock() {
        ProteinClinicalReviewResolver r = mock(ProteinClinicalReviewResolver.class);
        when(r.findRequiringClinicalReview(anyCollection())).thenAnswer(inv -> {
            Set<MedicalCondition> c = healthProfile.getMedicalProfile() == null
                    ? Set.of() : healthProfile.getMedicalProfile().getMedicalConditions();
            return ProteinCalculator.anyRequireClinicalReview(c)
                    ? Set.of(healthProfile.getId()) : Set.<UUID>of();
        });
        return r;
    }

    private ProteinAdaptationProfile copy(ProteinAdaptationProfile s) {
        ProteinAdaptationProfile c = ProteinAdaptationProfile.builder()
                .healthProfile(s.getHealthProfile())
                .currentProteinGrams(s.getCurrentProteinGrams())
                .longTermTargetGrams(s.getLongTermTargetGrams())
                .currentTargetGrams(s.getCurrentTargetGrams())
                .adaptationStage(s.getAdaptationStage())
                .daysAtCurrentTarget(s.getDaysAtCurrentTarget())
                .toleranceStatus(s.getToleranceStatus())
                .lastTargetChangeAt(s.getLastTargetChangeAt())
                .lastEvaluatedOn(s.getLastEvaluatedOn())
                .build();
        c.setId(s.getId());
        c.setVersion(s.getVersion());
        return c;
    }

    private ProteinAdaptationProfile save(ProteinAdaptationProfile p) {
        synchronized (rows) {
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                p.setVersion(0L);
                rows.put(p.getId(), copy(p));
                return p;
            }
            ProteinAdaptationProfile row = rows.get(p.getId());
            if (!Objects.equals(row.getVersion(), p.getVersion())) {
                throw new ObjectOptimisticLockingFailureException(ProteinAdaptationProfile.class, p.getId());
            }
            ProteinAdaptationProfile updated = copy(p);
            updated.setVersion(row.getVersion() + 1);
            rows.put(p.getId(), updated);
            return copy(updated);
        }
    }

    private void awaitRaceOnce(List<?> out) {
        CyclicBarrier barrier = raceBarrier;
        if (barrier != null && !out.isEmpty() && !alreadyRaced.get()) {
            alreadyRaced.set(true);
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static FatCalculator stubFat() {
        FatCalculator f = mock(FatCalculator.class);
        when(f.calculate(any())).thenReturn(new FatCalculationResult(
                new BigDecimal("60"), new BigDecimal("540"), new BigDecimal("600"),
                new BigDecimal("0.8"), "FAT_LOSS", true, CalorieSafetyStatus.SAFE,
                false, Collections.emptyList()));
        return f;
    }

    private static CarbohydrateCalculator stubCarbs() {
        CarbohydrateCalculator c = mock(CarbohydrateCalculator.class);
        when(c.calculate(any())).thenReturn(new CarbohydrateCalculationResult(
                new BigDecimal("150"), new BigDecimal("600"), new BigDecimal("600"),
                true, CalorieSafetyStatus.SAFE, false, Collections.emptyList()));
        return c;
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static LocalDate day(int n) {
        return BASE.plusDays(n);
    }

    private BigDecimal latestIntake() {
        return healthProfile.getCurrentNutritionProfile() == null
                ? null : healthProfile.getCurrentNutritionProfile().getCurrentProteinGrams();
    }

    private void reportIntake(String grams) {
        healthProfile.setCurrentNutritionProfile(CurrentNutritionProfile.builder()
                .currentProteinGrams(grams == null ? null : new BigDecimal(grams)).build());
    }

    private void setConditions(MedicalCondition... conditions) {
        MedicalProfile mp = MedicalProfile.builder()
                .medicalConditions(new HashSet<>(Arrays.asList(conditions))).build();
        mp.setHealthProfile(healthProfile);
        healthProfile.setMedicalProfile(mp);
    }

    /** The single persisted row (live reference, read-only use). */
    private ProteinAdaptationProfile row() {
        synchronized (rows) {
            assertThat(rows).hasSize(1);
            return rows.values().iterator().next();
        }
    }

    private void forceRow(Consumer<ProteinAdaptationProfile> change) {
        synchronized (rows) {
            change.accept(rows.values().iterator().next());
        }
    }

    private ProteinAdaptationRunSummary runOn(int dayNumber) {
        return scheduler.run(day(dayNumber));
    }

    private void runDays(int from, int toInclusive) {
        for (int d = from; d <= toInclusive; d++) {
            runOn(d);
        }
    }

    private static boolean unknownIntakeWarning(NutritionTargetResponse r) {
        return r.warnings().stream().anyMatch(w ->
                w.message().contains("[Protein]")
                        && w.message().contains("Current protein intake is unknown"));
    }

    /** latest intake, baseline, target, stage and warnings all agree with persisted state. */
    private void assertConsistent(NutritionTargetResponse r) {
        ProteinAdaptationProfile p = row();
        assertThat(r.currentProteinIntakeGrams()).isEqualTo(latestIntake());
        assertThat(r.adaptationBaselineProteinGrams()).isEqualTo(p.getCurrentProteinGrams());
        assertThat(r.proteinGrams()).isEqualByComparingTo(p.getCurrentTargetGrams());
        assertThat(r.longTermProteinGrams()).isEqualByComparingTo(p.getLongTermTargetGrams());
        assertThat(r.adaptationStage()).isEqualTo(p.getAdaptationStage());
        assertThat(unknownIntakeWarning(r)).isEqualTo(latestIntake() == null);
        boolean differ = !Objects.equals(
                r.currentProteinIntakeGrams() == null ? null : r.currentProteinIntakeGrams().stripTrailingZeros(),
                r.adaptationBaselineProteinGrams() == null ? null : r.adaptationBaselineProteinGrams().stripTrailingZeros());
        assertThat(r.adaptationBaselineOutOfDate()).isEqualTo(differ);
    }

    // ═══ 1. New user, no reported intake ═══════════════════════════════

    @Test
    void scenario01_newUserWithNoIntake() {
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.currentProteinIntakeGrams()).isNull();
        assertThat(r.adaptationBaselineProteinGrams()).isNull();
        assertThat(r.adaptationBaselineOutOfDate()).isFalse();
        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.NOT_STARTED);
        assertThat(r.rampUpRequired()).isFalse();
        assertThat(r.proteinGrams()).isEqualByComparingTo("120");      // no ramp possible
        assertThat(unknownIntakeWarning(r)).isTrue();
        assertConsistent(r);

        // the scheduler has nothing to count for a NOT_STARTED profile
        ProteinAdaptationRunSummary s = runOn(1);
        assertThat(s.examined()).isZero();
        assertThat(row().getDaysAtCurrentTarget()).isZero();
        assertThat(row().getLastEvaluatedOn()).isNull();
    }

    @Test
    void scenario01b_firstEverIntakeReport_capturesTheBaselineOnce_thenNeverRebaselines() {
        service.calculateForCurrentUser();                       // NOT_STARTED, no baseline
        reportIntake("70");

        NutritionTargetResponse started = service.calculateForCurrentUser();
        assertThat(started.adaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        assertThat(started.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");

        reportIntake("95");
        NutritionTargetResponse later = service.calculateForCurrentUser();
        assertThat(later.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertThat(later.adaptationBaselineOutOfDate()).isTrue();
        assertConsistent(later);
    }

    // ═══ 2. New user with known intake ═════════════════════════════════

    @Test
    void scenario02_newUserWithKnownIntake() {
        reportIntake("70");

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.currentProteinIntakeGrams()).isEqualByComparingTo("70");
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertThat(r.adaptationBaselineOutOfDate()).isFalse();
        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        assertThat(r.rampUpRequired()).isTrue();
        assertThat(r.proteinGrams()).isEqualByComparingTo("100");      // gap 50 -> 120 - 20
        assertThat(r.longTermProteinGrams()).isEqualByComparingTo("120");
        assertThat(unknownIntakeWarning(r)).isFalse();
        assertConsistent(r);
        assertThat(row().getVersion()).isZero();
    }

    // ═══ 3. Intake updated after adaptation starts ═════════════════════

    @Test
    void scenario03_intakeUpdatedMidAdaptation_doesNotResetOrRebaseline() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 3);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);

        reportIntake("95");
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.currentProteinIntakeGrams()).isEqualByComparingTo("95");
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertThat(r.adaptationBaselineOutOfDate()).isTrue();
        assertThat(r.proteinGrams()).isEqualByComparingTo("100");
        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);        // progress kept
        assertThat(row().getLastTargetChangeAt()).isEqualTo(BASE);      // not restarted
        assertConsistent(r);

        // the ramp then proceeds on the original schedule
        runDays(4, 7);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row().getAdaptationStage()).isEqualTo(ProteinAdaptationStage.ADAPTING);
        assertThat(row().getCurrentProteinGrams()).isEqualByComparingTo("70");
        assertConsistent(service.calculateForCurrentUser());
    }

    // ═══ 4. Intake cleared ═════════════════════════════════════════════

    @Test
    void scenario04_intakeCleared_keepsBaselineAndProgress_andWarns() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 4);

        reportIntake(null);
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.currentProteinIntakeGrams()).isNull();
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertThat(r.adaptationBaselineOutOfDate()).isTrue();
        assertThat(unknownIntakeWarning(r)).isTrue();
        assertThat(r.proteinGrams()).isEqualByComparingTo("100");
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(4);
        assertConsistent(r);

        // the scheduler still counts days for a user whose intake is now unknown
        runDays(5, 7);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row().getCurrentProteinGrams()).isEqualByComparingTo("70");
        assertConsistent(service.calculateForCurrentUser());
    }

    // ═══ 5. Long-term target reached ═══════════════════════════════════

    @Test
    void scenario05_reachesLongTermTarget_clampsAndStops() {
        reportIntake("70");
        service.calculateForCurrentUser();

        runDays(1, 7);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
        runDays(8, 14);

        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("120");   // clamped, not 120+
        assertThat(row().getAdaptationStage()).isEqualTo(ProteinAdaptationStage.TARGET_REACHED);

        // further days: no change at all, and the profile is no longer selected
        int version = row().getVersion().intValue();
        ProteinAdaptationRunSummary after = runOn(15);
        assertThat(after.examined()).isZero();
        assertThat(row().getVersion().intValue()).isEqualTo(version);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("120");

        reportIntake("125");
        NutritionTargetResponse r = service.calculateForCurrentUser();
        assertThat(r.rampUpRequired()).isFalse();
        assertThat(r.nextEligibleProteinIncreaseDate()).isNull();
        assertThat(r.proteinGrams()).isEqualByComparingTo("120");
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertConsistent(r);
    }

    // ═══ 6. Kidney / liver clinical review ═════════════════════════════

    @ParameterizedTest
    @EnumSource(value = MedicalCondition.class, names = {"KIDNEY_DISEASE", "LIVER_DISEASE"})
    void scenario06_clinicalReview_blocksTheScheduler_andIsNotSticky(MedicalCondition condition) {
        reportIntake("70");
        setConditions(condition);

        NutritionTargetResponse r = service.calculateForCurrentUser();
        assertThat(r.requiresClinicalReview()).isTrue();
        assertConsistent(r);
        BigDecimal target = row().getCurrentTargetGrams();
        ProteinAdaptationStage stage = row().getAdaptationStage();

        ProteinAdaptationRunSummary blocked = null;
        for (int d = 1; d <= 10; d++) {
            blocked = runOn(d);
        }
        assertThat(blocked.outcomes()).containsEntry(ScheduledEvaluationOutcome.SKIPPED_CLINICAL_REVIEW, 1);
        assertThat(row().getDaysAtCurrentTarget()).isZero();
        assertThat(row().getLastEvaluatedOn()).isNull();
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo(target);
        assertThat(row().getAdaptationStage()).isEqualTo(stage);

        // condition removed -> the very next run progresses (nothing was stored)
        healthProfile.getMedicalProfile().getMedicalConditions().clear();
        if (stage == ProteinAdaptationStage.STARTING || stage == ProteinAdaptationStage.ADAPTING) {
            runOn(11);
            assertThat(row().getDaysAtCurrentTarget()).isEqualTo(1);
        }
    }

    @Test
    void scenario06b_nonReviewConditions_doNotBlock() {
        reportIntake("70");
        service.calculateForCurrentUser();
        setConditions(MedicalCondition.DIABETES, MedicalCondition.HYPERTENSION);

        runOn(1);

        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(1);
    }

    // ═══ 7. Tolerance issues ═══════════════════════════════════════════

    @Test
    void scenario07_toleranceIssues_holdProgression_thenResume() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 3);

        adaptationService.recordTolerance(copy(row()), ProteinToleranceStatus.MILD_ISSUES);
        ProteinAdaptationRunSummary held = null;
        for (int d = 4; d <= 6; d++) {
            held = runOn(d);
        }
        assertThat(held.outcomes()).containsEntry(ScheduledEvaluationOutcome.HELD_TOLERANCE, 1);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);

        adaptationService.recordTolerance(copy(row()), ProteinToleranceStatus.SIGNIFICANT_ISSUES);
        runDays(7, 9);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("100");

        // intake changes while held do not release the hold
        reportIntake("110");
        runOn(10);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);
        assertThat(row().getToleranceStatus()).isEqualTo(ProteinToleranceStatus.SIGNIFICANT_ISSUES);

        adaptationService.recordTolerance(copy(row()), ProteinToleranceStatus.GOOD);
        runDays(11, 14);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row().getCurrentProteinGrams()).isEqualByComparingTo("70");
        assertConsistent(service.calculateForCurrentUser());
    }

    // ═══ 8. Scheduler twice on the same day ════════════════════════════

    @Test
    void scenario08_schedulerTwiceOnSameDay_countsOneDay() {
        reportIntake("70");
        service.calculateForCurrentUser();

        ProteinAdaptationRunSummary first = runOn(1);
        long version = row().getVersion();
        ProteinAdaptationRunSummary second = runOn(1);

        assertThat(first.examined()).isEqualTo(1);
        assertThat(second.examined()).isZero();
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(1);
        assertThat(row().getVersion()).isEqualTo(version);                // second run wrote nothing

        // and the service-level guard holds even if a stale copy slips through the query
        ScheduledEvaluationResult direct =
                adaptationService.evaluateScheduled(copy(row()), Set.of(), day(1));
        assertThat(direct.outcome()).isEqualTo(ScheduledEvaluationOutcome.ALREADY_EVALUATED_TODAY);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(1);
    }

    @Test
    void scenario08b_twiceOnTheAdvanceDay_advancesOnlyOnce() {
        reportIntake("70");
        service.calculateForCurrentUser();
        forceRow(p -> p.setDaysAtCurrentTarget(6));

        runOn(1);
        runOn(1);

        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row().getDaysAtCurrentTarget()).isZero();
    }

    // ═══ 9. Two concurrent scheduler runs ══════════════════════════════

    @Test
    void scenario09_twoConcurrentRuns_oneWins_oneConflicts_noDoubleProgression() throws Exception {
        reportIntake("70");
        service.calculateForCurrentUser();
        forceRow(p -> p.setDaysAtCurrentTarget(6));        // today's tick would advance 100 -> 110
        raceBarrier = new CyclicBarrier(2);                // both read before either writes

        ProteinAdaptationScheduler a = newScheduler();
        ProteinAdaptationScheduler b = newScheduler();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ProteinAdaptationRunSummary> fa = pool.submit(() -> a.run(day(1)));
            Future<ProteinAdaptationRunSummary> fb = pool.submit(() -> b.run(day(1)));
            ProteinAdaptationRunSummary sa = fa.get(20, TimeUnit.SECONDS);
            ProteinAdaptationRunSummary sb = fb.get(20, TimeUnit.SECONDS);

            assertThat(sa.conflicts() + sb.conflicts()).isEqualTo(1);
            assertThat(sa.failures() + sb.failures()).isZero();
            int advanced = sa.outcomes().getOrDefault(ScheduledEvaluationOutcome.TARGET_ADVANCED, 0)
                    + sb.outcomes().getOrDefault(ScheduledEvaluationOutcome.TARGET_ADVANCED, 0);
            assertThat(advanced).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            raceBarrier = null;
        }

        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");   // not 120
        assertThat(row().getDaysAtCurrentTarget()).isZero();
        assertThat(row().getLastEvaluatedOn()).isEqualTo(day(1));
        assertThat(row().getVersion()).isEqualTo(1L);                            // exactly one write

        // a late third run finds nothing to do
        assertThat(runOn(1).examined()).isZero();
        assertConsistent(service.calculateForCurrentUser());
    }

    @Test
    void scenario09b_staleWriterIsRejectedByVersionCheck() {
        reportIntake("70");
        service.calculateForCurrentUser();
        ProteinAdaptationProfile staleA = copy(row());
        ProteinAdaptationProfile staleB = copy(row());

        adaptationService.evaluateScheduled(staleA, Set.of(), day(1));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> adaptationService.evaluateScheduled(staleB, Set.of(), day(1)))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);

        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(1);
    }

    // ═══ 10. Nutrition target changes after initialisation ═════════════

    @Test
    void scenario10a_longTermTargetRises_progressIsKept() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 3);

        personalProfile.setWeightKg(new BigDecimal("90"));          // 1.6 g/kg -> 144 g
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.longTermProteinGrams()).isEqualByComparingTo("144");
        assertThat(r.proteinGrams()).isEqualByComparingTo("100");   // current target untouched
        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        assertThat(row().getDaysAtCurrentTarget()).isEqualTo(3);
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertThat(r.currentProteinIntakeGrams()).isEqualByComparingTo("70");
        assertThat(r.adaptationBaselineOutOfDate()).isFalse();
        assertConsistent(r);

        runDays(4, 7);                                              // ramp continues, same schedule
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("110");
    }

    @Test
    void scenario10b_longTermTargetFalls_clampsCurrentTarget_andFinishes() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 3);

        personalProfile.setWeightKg(new BigDecimal("60"));          // 1.6 g/kg -> 96 g < current 100
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.longTermProteinGrams()).isEqualByComparingTo("96");
        assertThat(r.proteinGrams()).isEqualByComparingTo("96");
        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.TARGET_REACHED);
        assertThat(r.rampUpRequired()).isFalse();
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");   // baseline untouched
        assertConsistent(r);

        assertThat(runOn(4).examined()).isZero();                   // scheduler no longer selects it
    }

    @Test
    void scenario10c_targetChangeAfterTargetReached_resumesRamp() {
        reportIntake("70");
        service.calculateForCurrentUser();
        runDays(1, 14);
        assertThat(row().getAdaptationStage()).isEqualTo(ProteinAdaptationStage.TARGET_REACHED);

        personalProfile.setWeightKg(new BigDecimal("90"));          // long-term 144 > current 120
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.adaptationStage()).isEqualTo(ProteinAdaptationStage.ADAPTING);
        assertThat(r.proteinGrams()).isEqualByComparingTo("120");
        assertThat(r.rampUpRequired()).isTrue();
        assertThat(row().getDaysAtCurrentTarget()).isZero();
        assertThat(r.adaptationBaselineProteinGrams()).isEqualByComparingTo("70");
        assertConsistent(r);

        runDays(15, 21);
        assertThat(row().getCurrentTargetGrams()).isEqualByComparingTo("130");
    }

    @Test
    void scenario10d_unchangedInputs_writeNothing() {
        reportIntake("70");
        service.calculateForCurrentUser();
        long version = row().getVersion();

        service.calculateForCurrentUser();
        service.calculateForCurrentUser();

        assertThat(row().getVersion()).isEqualTo(version);
    }
}
