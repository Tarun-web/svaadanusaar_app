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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * First-insert race on {@link ProteinAdaptationProfile}: two concurrent first
 * calculations for the same user both find no row and both insert.
 *
 * <p>The real {@link NutritionTargetServiceImpl}, {@link MacroCalculator},
 * {@link ProteinCalculator} and {@link ProteinAdaptationService} run; only the
 * persistence boundary is an in-memory fake that defers the INSERT until
 * {@code flush()} and enforces the unique {@code health_profile_id}
 * constraint (SQLState 23505) like the database would. A {@link CyclicBarrier}
 * forces both threads to read before either writes.
 * <strong>This is not PostgreSQL verification.</strong></p>
 */
class ProteinFirstInsertRaceTest {

    private final Map<UUID, ProteinAdaptationProfile> rows = new LinkedHashMap<>();
    private final ThreadLocal<ProteinAdaptationProfile> pending = new ThreadLocal<>();
    private volatile CyclicBarrier readBarrier;
    private final ThreadLocal<Boolean> alreadyWaited = ThreadLocal.withInitial(() -> false);
    private volatile SQLException flushFailure;

    private HealthProfile healthProfile;
    private ProteinAdaptationProfileRepository repository;
    private NutritionTargetServiceImpl service;
    private ProteinAdaptationService adaptation;

    @BeforeEach
    void setUp() {
        repository = mock(ProteinAdaptationProfileRepository.class);

        when(repository.findByHealthProfile(any())).thenAnswer(inv -> {
            Optional<ProteinAdaptationProfile> found;
            synchronized (rows) {
                found = rows.values().stream().findFirst().map(this::copy);
            }
            awaitBothHaveRead();
            return found;
        });
        when(repository.save(any(ProteinAdaptationProfile.class))).thenAnswer(inv -> {
            ProteinAdaptationProfile p = inv.getArgument(0);
            if (p.getId() == null) {
                pending.set(p);                       // INSERT is deferred to flush
                return p;
            }
            synchronized (rows) {
                rows.put(p.getId(), copy(p));
            }
            return p;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            ProteinAdaptationProfile p = pending.get();
            pending.remove();
            if (p == null) {
                return null;
            }
            synchronized (rows) {
                if (flushFailure != null) {
                    throw new DataIntegrityViolationException("could not execute statement", flushFailure);
                }
                if (!rows.isEmpty()) {                // uq_protein_adaptation_health_profile
                    throw new DataIntegrityViolationException("could not execute statement",
                            new SQLException("duplicate key value violates unique constraint", "23505"));
                }
                p.setId(UUID.randomUUID());
                p.setVersion(0L);
                rows.put(p.getId(), copy(p));
            }
            return null;
        }).when(repository).flush();

        adaptation = new ProteinAdaptationService(repository);
        MacroCalculator macro = new MacroCalculator(
                new ProteinCalculator(), adaptation, stubFat(), stubCarbs());

        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(new BigDecimal("75")).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile goal = FitnessGoalProfile.builder().primaryGoal(Goal.FAT_LOSS).build();
        healthProfile = HealthProfile.builder().build();
        healthProfile.setId(UUID.randomUUID());
        pp.setHealthProfile(healthProfile);
        goal.setHealthProfile(healthProfile);
        healthProfile.setPersonalProfile(pp);
        healthProfile.setFitnessGoalProfile(goal);
        healthProfile.setCurrentNutritionProfile(CurrentNutritionProfile.builder()
                .currentProteinGrams(new BigDecimal("70")).build());

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
    }

    // ── fake persistence helpers ───────────────────────────────────────

    private void awaitBothHaveRead() {
        CyclicBarrier barrier = readBarrier;
        if (barrier != null && !alreadyWaited.get()) {
            alreadyWaited.set(true);
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
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

    private ProteinAdaptationProfile onlyRow() {
        synchronized (rows) {
            assertThat(rows).hasSize(1);
            return rows.values().iterator().next();
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

    // ── tests ──────────────────────────────────────────────────────────

    @Test
    void singleFirstCalculation_insertsExactlyOneRow() {
        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertThat(r.proteinGrams()).isEqualByComparingTo("100");
        assertThat(onlyRow().getAdaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        verify(repository, times(1)).flush();
    }

    @Test
    void concurrentFirstCalculations_oneWins_loserGetsConflict_noDuplicateRow() throws Exception {
        readBarrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        return service.calculateForCurrentUser();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> f : futures) {
                outcomes.add(f.get(20, TimeUnit.SECONDS));
            }

            long winners = outcomes.stream().filter(o -> o instanceof NutritionTargetResponse).count();
            List<Object> losers = outcomes.stream()
                    .filter(o -> !(o instanceof NutritionTargetResponse)).toList();
            assertThat(winners).isEqualTo(1);
            assertThat(losers).hasSize(1);
            // never an unexplained raw integrity error: it is the mapped conflict type
            assertThat(losers.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        } finally {
            pool.shutdownNow();
            readBarrier = null;
        }

        ProteinAdaptationProfile row = onlyRow();
        assertThat(row.getVersion()).isZero();
        assertThat(row.getAdaptationStage()).isEqualTo(ProteinAdaptationStage.STARTING);
        assertThat(row.getCurrentTargetGrams()).isEqualByComparingTo("100");
        assertThat(row.getCurrentProteinGrams()).isEqualByComparingTo("70");
    }

    @Test
    void retryAfterLosingTheRace_findsTheWinnersRow_andDoesNotResetProgress() throws Exception {
        readBarrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> a = pool.submit(() -> attempt());
            Future<Object> b = pool.submit(() -> attempt());
            a.get(20, TimeUnit.SECONDS);
            b.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            readBarrier = null;
        }
        // the winner's row has made progress since
        synchronized (rows) {
            ProteinAdaptationProfile live = rows.values().iterator().next();
            live.setDaysAtCurrentTarget(4);
            live.setLastEvaluatedOn(LocalDate.now());
        }

        NutritionTargetResponse retry = service.calculateForCurrentUser();   // the loser's retry

        assertThat(retry.proteinGrams()).isEqualByComparingTo("100");
        ProteinAdaptationProfile row = onlyRow();
        assertThat(row.getDaysAtCurrentTarget()).isEqualTo(4);               // not reset to 0
        assertThat(row.getLastEvaluatedOn()).isEqualTo(LocalDate.now());
        assertThat(row.getVersion()).isZero();
    }

    private Object attempt() {
        try {
            return service.calculateForCurrentUser();
        } catch (RuntimeException e) {
            return e;
        }
    }

    @Test
    void existingProgress_isNeverOverwritten_andNoInsertIsAttempted() {
        service.calculateForCurrentUser();
        synchronized (rows) {
            ProteinAdaptationProfile live = rows.values().iterator().next();
            live.setAdaptationStage(ProteinAdaptationStage.ADAPTING);
            live.setCurrentTargetGrams(new BigDecimal("110"));
            live.setDaysAtCurrentTarget(3);
        }

        service.calculateForCurrentUser();

        ProteinAdaptationProfile row = onlyRow();
        assertThat(row.getAdaptationStage()).isEqualTo(ProteinAdaptationStage.ADAPTING);
        assertThat(row.getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row.getDaysAtCurrentTarget()).isEqualTo(3);
        verify(repository, times(1)).flush();            // only the very first insert flushed
    }

    @Test
    void initialise_findingARowAlreadyPastNotStarted_isAConflict_andDoesNotResetIt() {
        // A row created by a concurrent request between the caller's read and
        // initialise()'s own re-read, which has already made progress.
        service.calculateForCurrentUser();
        synchronized (rows) {
            ProteinAdaptationProfile live = rows.values().iterator().next();
            live.setAdaptationStage(ProteinAdaptationStage.ADAPTING);
            live.setCurrentTargetGrams(new BigDecimal("110"));
            live.setDaysAtCurrentTarget(5);
        }
        ProteinTargetResult longTerm = mock(ProteinTargetResult.class);
        when(longTerm.longTermTargetGrams()).thenReturn(new BigDecimal("120"));

        assertThatThrownBy(() -> adaptation.initialise(healthProfile, longTerm, new BigDecimal("70")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        ProteinAdaptationProfile row = onlyRow();
        assertThat(row.getAdaptationStage()).isEqualTo(ProteinAdaptationStage.ADAPTING);
        assertThat(row.getCurrentTargetGrams()).isEqualByComparingTo("110");
        assertThat(row.getDaysAtCurrentTarget()).isEqualTo(5);
    }

    @Test
    void nonUniqueIntegrityViolation_isNotDisguisedAsAConflict() {
        flushFailure = new SQLException("null value in column violates not-null constraint", "23502");

        assertThatThrownBy(() -> service.calculateForCurrentUser())
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void noRowIsFlushedWhenAnExistingRowIsUpdated() {
        service.calculateForCurrentUser();
        org.mockito.Mockito.clearInvocations(repository);

        service.calculateForCurrentUser();

        verify(repository, never()).flush();
    }
}
