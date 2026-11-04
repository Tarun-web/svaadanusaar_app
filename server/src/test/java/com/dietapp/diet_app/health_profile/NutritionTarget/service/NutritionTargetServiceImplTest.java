package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.*;
import com.dietapp.diet_app.health_profile.entity.*;
import com.dietapp.diet_app.health_profile.enums.*;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NutritionTargetServiceImpl}.
 *
 * <p>The full pipeline is tested by mocking every collaborator.
 * These tests verify orchestration logic: correct ordering of calls,
 * correct wiring of inputs/outputs between pipeline stages, correct
 * aggregation of warnings, correct field population in the response,
 * and edge cases such as missing profiles, no workout data, and
 * existing adaptation profiles.</p>
 *
 * <p>{@code LENIENT} strictness is used so that the {@code @BeforeEach}
 * repository stub is tolerated by tests that throw before reaching
 * the adaptation-profile lookup.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(
        strictness = org.mockito.quality.Strictness.LENIENT)
class NutritionTargetServiceImplTest {

    // ── Mocks ──────────────────────────────────────────────────────────
    @Mock HealthProfileContextService        healthProfileContextService;
    @Mock GoalCalorieCalculator              goalCalorieCalculator;
    @Mock CalorieSafetyEngine                calorieSafetyEngine;
    @Mock MacroCalculator                    macroCalculator;
    @Mock FibreCalculator                    fibreCalculator;
    @Mock WaterCalculator                    waterCalculator;
    @Mock ProteinAdaptationProfileRepository proteinAdaptationProfileRepository;

    @InjectMocks
    NutritionTargetServiceImpl service;

    // ── Shared fixtures ────────────────────────────────────────────────

    private static final BigDecimal WEIGHT      = new BigDecimal("75");
    private static final BigDecimal TDEE        = new BigDecimal("2200");
    private static final BigDecimal PROPOSED    = new BigDecimal("1700");
    private static final BigDecimal FINAL_CAL   = new BigDecimal("1700");
    private static final BigDecimal PROTEIN     = new BigDecimal("110");
    private static final BigDecimal LONG_PROTEIN= new BigDecimal("135");
    private static final BigDecimal FAT         = new BigDecimal("60");
    private static final BigDecimal CARBS       = new BigDecimal("150");
    private static final BigDecimal FIBRE       = new BigDecimal("23.80");
    private static final BigDecimal WATER       = new BigDecimal("2630");

    /**
     * Builds a HealthProfile with personal + fitness-goal profiles attached.
     *
     * <p>Sets both directions of the relationship: {@code pp/fg.healthProfile = hp}
     * AND {@code hp.personalProfile = pp}, {@code hp.fitnessGoalProfile = fg}.
     * This mirrors what JPA would do at runtime when the entity graph is loaded.</p>
     */
    private HealthProfile healthProfileWith(PersonalProfile pp, FitnessGoalProfile fg) {
        HealthProfile hp = HealthProfile.builder().build();
        pp.setHealthProfile(hp);
        fg.setHealthProfile(hp);
        hp.setPersonalProfile(pp);
        hp.setFitnessGoalProfile(fg);
        return hp;
    }

    /** Stub for CalorieTargetResult. */
    private CalorieTargetResult calorieResult() {
        return CalorieTargetResult.builder()
                .tdee(TDEE)
                .proposedCalories(PROPOSED)
                .goal(Goal.FAT_LOSS)
                .build();
    }

    /** Stub for CalorieSafetyResult — SAFE, no warnings. */
    private CalorieSafetyResult safetyResult() {
        return new CalorieSafetyResult(
                PROPOSED,
                FINAL_CAL,
                CalorieSafetyStatus.SAFE,
                false,
                false,
                Collections.emptyList()
        );
    }

    /** Stub for ProteinAdaptationResult. */
    private ProteinAdaptationResult proteinResult() {
        return new ProteinAdaptationResult(
                PROTEIN,
                LONG_PROTEIN,
                new BigDecimal("70"),
                ProteinAdaptationStage.STARTING,
                true,
                null,
                ProteinToleranceStatus.UNKNOWN,
                Collections.emptyList()
        );
    }

    /** Stub for FatCalculationResult. */
    private FatCalculationResult fatResult() {
        return new FatCalculationResult(
                FAT,
                FAT.multiply(new BigDecimal("9")),
                new BigDecimal("600"),
                new BigDecimal("0.8"),
                "FAT_LOSS",
                true,
                CalorieSafetyStatus.SAFE,
                false,
                Collections.emptyList()
        );
    }

    /** Stub for CarbohydrateCalculationResult. */
    private CarbohydrateCalculationResult carbResult() {
        return new CarbohydrateCalculationResult(
                CARBS,
                new BigDecimal("600"),
                new BigDecimal("600"),
                true,
                CalorieSafetyStatus.SAFE,
                false,
                Collections.emptyList()
        );
    }

    /** Stub for WaterCalculationResult — no warnings. */
    private WaterCalculationResult waterResult() {
        return new WaterCalculationResult(
                WATER,
                new BigDecimal("2625"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                Collections.emptyList()
        );
    }

    @BeforeEach
    void stubProteinAdaptationRepository() {
        // Default: no existing adaptation profile
        when(proteinAdaptationProfileRepository.findByHealthProfile(any()))
                .thenReturn(Optional.empty());
    }

    // ================================================================
    // Helpers to wire a "happy path" mock chain
    // ================================================================

    private void stubHappyPath(HealthProfile hp) {
        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(safetyResult());

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any()))
                .thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr))
                .thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any()))
                .thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr))
                .thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any()))
                .thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr))
                .thenReturn(CARBS);

        when(fibreCalculator.calculate(FINAL_CAL))
                .thenReturn(FIBRE);
        when(waterCalculator.calculate(any()))
                .thenReturn(waterResult());
    }

    // ================================================================
    // Tests
    // ================================================================

    // ── 1. Null health profile ────────────────────────────────────────

    @Test
    void nullHealthProfile_throwsIllegalState() {
        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateForCurrentUser()
        );
    }

    // ── 2. Missing personal profile ───────────────────────────────────

    @Test
    void missingPersonalProfile_throwsIllegalState() {
        HealthProfile hp = HealthProfile.builder().build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        fg.setHealthProfile(hp);

        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(hp);

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateForCurrentUser()
        );
    }

    // ── 3. Missing fitness goal profile ──────────────────────────────

    @Test
    void missingFitnessGoalProfile_throwsIllegalState() {
        HealthProfile hp = HealthProfile.builder().build();
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        pp.setHealthProfile(hp);

        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(hp);

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateForCurrentUser()
        );
    }

    // ── 4. Missing weight ─────────────────────────────────────────────

    @Test
    void missingWeight_throwsIllegalState() {
        HealthProfile hp = HealthProfile.builder().build();
        PersonalProfile pp = PersonalProfile.builder()
                .heightCm(172.0).gender(Gender.MALE).build(); // no weight
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        pp.setHealthProfile(hp);
        fg.setHealthProfile(hp);
        hp.setPersonalProfile(pp);
        hp.setFitnessGoalProfile(fg);

        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(hp);

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateForCurrentUser()
        );
    }

    // ── 5. Happy path: minimal valid profile (no workout, no medical) ─

    @Test
    void happyPath_minimalProfile_populatesAllMacroFields() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);

        NutritionTargetResponse response = service.calculateForCurrentUser();

        assertNotNull(response);
        assertEquals(TDEE,        response.tdee());
        assertEquals(PROPOSED,    response.proposedCalories());
        assertEquals(FINAL_CAL,   response.finalCalories());
        assertEquals(PROTEIN,     response.proteinGrams());
        assertEquals(LONG_PROTEIN,response.longTermProteinGrams());
        assertEquals(FAT,         response.fatGrams());
        assertEquals(CARBS,       response.carbohydrateGrams());
        assertEquals(FIBRE,       response.fibreGrams());
        assertEquals(WATER,       response.waterMl());
    }

    // ── 6. Macro fields never null on happy path ──────────────────────

    @Test
    void happyPath_noNullMacroFields() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.MUSCLE_GAIN).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertNotNull(r.proteinGrams(),        "proteinGrams must not be null");
        assertNotNull(r.longTermProteinGrams(), "longTermProteinGrams must not be null");
        assertNotNull(r.fatGrams(),            "fatGrams must not be null");
        assertNotNull(r.carbohydrateGrams(),   "carbohydrateGrams must not be null");
        assertNotNull(r.fibreGrams(),          "fibreGrams must not be null");
        assertNotNull(r.waterMl(),             "waterMl must not be null");
    }

    // ── 7. Adaptation fields correct ─────────────────────────────────

    @Test
    void happyPath_adaptationFieldsPopulated() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertEquals(ProteinAdaptationStage.STARTING, r.adaptationStage());
        assertTrue(r.rampUpRequired());
        assertNull(r.nextEligibleProteinIncreaseDate());
    }

    // ── 8. proteinGrams = currentTarget, NOT longTermTarget ──────────

    @Test
    void proteinGrams_isCurrentAdaptationTarget_notLongTermTarget() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);

        NutritionTargetResponse r = service.calculateForCurrentUser();

        // Current target < long-term target during adaptation
        assertTrue(
                r.proteinGrams().compareTo(r.longTermProteinGrams()) < 0,
                "During adaptation, proteinGrams must be < longTermProteinGrams"
        );
    }

    // ── 9. FatCalculator receives current protein, not long-term ──────

    @Test
    void fatCalculator_receivesCurrentProteinTarget() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        // Verify that the FatCalculationInput given to macroCalculator.calculateFat()
        // contains the current protein target (PROTEIN = 110), not long-term (135)
        verify(macroCalculator).calculateFat(argThat(input ->
                input.proteinGrams().compareTo(PROTEIN) == 0
        ));
    }

    // ── 10. FatCalculator receives finalCalories not proposedCalories ─

    @Test
    void fatCalculator_receivesFinalCalories_notProposedCalories() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        // Make finalCalories different from proposedCalories to verify wiring
        CalorieSafetyResult adjusted = new CalorieSafetyResult(
                new BigDecimal("1700"),
                new BigDecimal("1500"),   // safety adjusted final
                CalorieSafetyStatus.ADJUSTED,
                true,
                false,
                Collections.emptyList()
        );
        when(healthProfileContextService.getCurrentUserHealthProfile())
                .thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(adjusted);

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(new BigDecimal("1500"))).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        // finalCalories in response should be the safety-adjusted value
        assertEquals(new BigDecimal("1500"), r.finalCalories());
        assertTrue(r.safetyAdjusted());

        // FatCalculator was called with targetCalories = 1500 (finalCalories)
        verify(macroCalculator).calculateFat(argThat(input ->
                input.targetCalories().compareTo(new BigDecimal("1500")) == 0
        ));
    }

    // ── 11. Resistance training detected from GYM activity ───────────

    @Test
    void resistanceTraining_detectedFromGymActivity() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.MUSCLE_GAIN).build();

        ActivityProfile gymActivity = ActivityProfile.builder()
                .activityType(ActivityType.GYM)
                .intensity(WorkoutIntensity.HIGH)
                .daysPerWeek(5)
                .durationMinutes(60)
                .experienceLevel(ExperienceLevel.TWO_TO_FIVE_YEARS)
                .build();

        WorkoutProfile wp = WorkoutProfile.builder()
                .activities(new HashSet<>(Set.of(gymActivity)))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        wp.setHealthProfile(hp);

        // Wire the workout profile into hp
        hp.setWorkoutProfile(wp);
        gymActivity.setWorkoutProfile(wp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        // ProteinCalculationInput must have resistanceTraining = true
        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input -> input.resistanceTraining()),
                any()
        );
    }

    // ── 12. No GYM activity → resistanceTraining = false ─────────────

    @Test
    void noGymActivity_resistanceTrainingFalse() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();

        ActivityProfile runActivity = ActivityProfile.builder()
                .activityType(ActivityType.RUNNING)
                .intensity(WorkoutIntensity.MODERATE)
                .daysPerWeek(4)
                .durationMinutes(45)
                .experienceLevel(ExperienceLevel.LESS_THAN_A_YEAR)
                .build();

        WorkoutProfile wp = WorkoutProfile.builder()
                .activities(new HashSet<>(Set.of(runActivity)))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        hp.setWorkoutProfile(wp);
        wp.setHealthProfile(hp);
        runActivity.setWorkoutProfile(wp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input -> !input.resistanceTraining()),
                any()
        );
    }

    // ── 13. No workout profile → no activities passed to water ───────

    @Test
    void noWorkoutProfile_waterCalculatorReceivesEmptyActivities() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.MAINTENANCE).build();
        HealthProfile hp = healthProfileWith(pp, fg);
        // no workoutProfile set

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(waterCalculator).calculate(argThat(input ->
                input.activities() == null || input.activities().isEmpty()
        ));
    }

    // ── 14. Sport type resolved from SPORTS activity ──────────────────

    @Test
    void sportType_resolvedFromSportsActivity() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.SPORTS_PERFORMANCE).build();

        ActivityProfile sportsActivity = ActivityProfile.builder()
                .activityType(ActivityType.SPORTS)
                .sportType(SportType.CRICKET)
                .intensity(WorkoutIntensity.HIGH)
                .daysPerWeek(5)
                .durationMinutes(120)
                .experienceLevel(ExperienceLevel.FIVE_PLUS_YEARS)
                .build();

        WorkoutProfile wp = WorkoutProfile.builder()
                .activities(new HashSet<>(Set.of(sportsActivity)))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        hp.setWorkoutProfile(wp);
        wp.setHealthProfile(hp);
        sportsActivity.setWorkoutProfile(wp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateFat(argThat(input ->
                input.sportType() == SportType.CRICKET
        ));
    }

    // ── 15. currentProteinGrams pulled from CurrentNutritionProfile ──

    @Test
    void currentProteinGrams_pulledFromCurrentNutritionProfile() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();

        CurrentNutritionProfile cnp = CurrentNutritionProfile.builder()
                .currentProteinGrams(new BigDecimal("70"))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        hp.setCurrentNutritionProfile(cnp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input ->
                        input.currentProteinGrams() != null
                        && input.currentProteinGrams().compareTo(new BigDecimal("70")) == 0
                ),
                any()
        );
    }

    // ── 16. null CurrentNutritionProfile → currentProteinGrams = null ─

    @Test
    void nullCurrentNutritionProfile_currentProteinGramsIsNull() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);
        // no currentNutritionProfile

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input -> input.currentProteinGrams() == null),
                any()
        );
    }

    // ── 17. Existing adaptation profile is passed to MacroCalculator ─

    @Test
    void existingAdaptationProfile_passedToMacroCalculator() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        ProteinAdaptationProfile existing = ProteinAdaptationProfile.builder()
                .healthProfile(hp)
                .currentTargetGrams(new BigDecimal("120"))
                .longTermTargetGrams(LONG_PROTEIN)
                .adaptationStage(ProteinAdaptationStage.ADAPTING)
                .toleranceStatus(ProteinToleranceStatus.GOOD)
                .build();

        when(proteinAdaptationProfileRepository.findByHealthProfile(hp))
                .thenReturn(Optional.of(existing));

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(eq(hp), any(), eq(existing));
    }

    // ── 18. No existing adaptation profile → null passed to MacroCalculator

    @Test
    void noExistingAdaptationProfile_nullPassedToMacroCalculator() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        // default mock: returns empty Optional → null is passed
        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(eq(hp), any(), isNull());
    }

    // ── 19. Medical conditions propagated to protein and fat inputs ───

    @Test
    void medicalConditions_propagatedToProteinAndFatInputs() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();

        MedicalProfile mp = MedicalProfile.builder()
                .medicalConditions(new HashSet<>(Set.of(MedicalCondition.KIDNEY_DISEASE)))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        hp.setMedicalProfile(mp);
        mp.setHealthProfile(hp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input ->
                        input.medicalConditions() != null
                        && input.medicalConditions().contains(MedicalCondition.KIDNEY_DISEASE)
                ),
                any()
        );

        verify(macroCalculator).calculateFat(argThat(input ->
                input.medicalConditions() != null
                && input.medicalConditions().contains(MedicalCondition.KIDNEY_DISEASE)
        ));
    }

    // ── 20. Null medical profile → empty set passed (no NPE) ─────────

    @Test
    void nullMedicalProfile_noNpe_emptyConditions() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);
        // no medical profile

        stubHappyPath(hp);

        assertDoesNotThrow(() -> service.calculateForCurrentUser());

        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input ->
                        input.medicalConditions() != null
                        && input.medicalConditions().isEmpty()
                ),
                any()
        );
    }

    // ── 21. requiresClinicalReview = true when fat layer requires it ─

    @Test
    void requiresClinicalReview_whenFatLayerRequiresIt() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(safetyResult());  // SAFE at calorie layer

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        // Fat layer requires clinical review
        FatCalculationResult fatWithReview = new FatCalculationResult(
                FAT, FAT.multiply(new BigDecimal("9")),
                new BigDecimal("600"), new BigDecimal("0.8"),
                "KIDNEY_DISEASE_REVIEW", false,
                CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED, true,
                List.of(new FatWarning(FatWarningCode.CLINICAL_REVIEW_REQUIRED,
                        "Kidney disease detected — clinical review required."))
        );
        when(macroCalculator.calculateFat(any())).thenReturn(fatWithReview);
        when(macroCalculator.resolveNutritionTargetFat(fatWithReview)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(FINAL_CAL)).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertTrue(r.requiresClinicalReview(),
                "requiresClinicalReview must be true when any pipeline layer requires it");
    }

    // ── 22. requiresClinicalReview = true when safety layer requires it

    @Test
    void requiresClinicalReview_whenSafetyLayerRequiresIt() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        CalorieSafetyResult clinicalSafety = new CalorieSafetyResult(
                PROPOSED, FINAL_CAL,
                CalorieSafetyStatus.CLINICAL_REVIEW_REQUIRED,
                false, true,
                List.of(new CalorieWarning(
                        CalorieWarningCode.CLINICAL_REVIEW_REQUIRED,
                        "Clinical review required."
                ))
        );

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(clinicalSafety);

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(any())).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertTrue(r.requiresClinicalReview());
    }

    // ── 23. Warning aggregation: calorie warnings included ───────────

    @Test
    void warningAggregation_calorieWarningsIncluded() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        CalorieSafetyResult withWarning = new CalorieSafetyResult(
                PROPOSED, FINAL_CAL,
                CalorieSafetyStatus.WARNING, false, false,
                List.of(new CalorieWarning(
                        CalorieWarningCode.TARGET_RATE_TOO_AGGRESSIVE,
                        "Rate too aggressive."
                ))
        );

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(withWarning);

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(FINAL_CAL)).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertFalse(r.warnings().isEmpty(), "Response must include calorie warnings");
        assertTrue(r.warnings().stream()
                .anyMatch(w -> w.code() == CalorieWarningCode.TARGET_RATE_TOO_AGGRESSIVE));
    }

    // ── 24. Warning aggregation: protein warnings wrapped ─────────────

    @Test
    void warningAggregation_proteinWarningsWrapped() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        ProteinAdaptationResult prWithWarning = new ProteinAdaptationResult(
                PROTEIN, LONG_PROTEIN, null,
                ProteinAdaptationStage.NOT_STARTED, false, null,
                ProteinToleranceStatus.UNKNOWN,
                List.of(new ProteinWarning(
                        ProteinWarningCode.CURRENT_INTAKE_UNKNOWN,
                        "Current intake unknown."
                ))
        );

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(safetyResult());
        when(macroCalculator.calculateProtein(any(), any(), any()))
                .thenReturn(prWithWarning);
        when(macroCalculator.resolveNutritionTargetProtein(prWithWarning))
                .thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(FINAL_CAL)).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        boolean hasProteinWarning = r.warnings().stream()
                .anyMatch(w -> w.message().contains("[Protein]")
                            && w.message().contains("Current intake unknown"));
        assertTrue(hasProteinWarning,
                "Protein warnings must be wrapped and included in response warnings");
    }

    // ── 25. Warning de-duplication: same code appears only once ──────

    @Test
    void warningDeduplication_sameCodeAppearsOnce() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        // Two warnings with the SAME code
        CalorieSafetyResult withDupeWarnings = new CalorieSafetyResult(
                PROPOSED, FINAL_CAL,
                CalorieSafetyStatus.WARNING, false, false,
                List.of(
                        new CalorieWarning(CalorieWarningCode.MEDICAL_CONDITION_PRESENT, "msg1"),
                        new CalorieWarning(CalorieWarningCode.MEDICAL_CONDITION_PRESENT, "msg2")
                )
        );

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(withDupeWarnings);

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(FINAL_CAL)).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        long count = r.warnings().stream()
                .filter(w -> w.code() == CalorieWarningCode.MEDICAL_CONDITION_PRESENT)
                .count();

        assertEquals(1, count,
                "Duplicate warning codes must appear only once in the aggregated list");
    }

    // ── 26. Water calculator receives correct weight ──────────────────

    @Test
    void waterCalculator_receivesWeightFromPersonalProfile() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(new BigDecimal("90")).heightCm(180.0).gender(Gender.FEMALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.MAINTENANCE).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);
        // override weight stub
        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(fibreCalculator.calculate(any())).thenReturn(FIBRE);

        service.calculateForCurrentUser();

        verify(waterCalculator).calculate(argThat(input ->
                input.weightKg().compareTo(new BigDecimal("90")) == 0
        ));
    }

    // ── 27. Full pipeline call order (smoke test) ─────────────────────

    @Test
    void fullPipeline_allCalculatorsInvoked() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.BODY_RECOMPOSITION).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        verify(goalCalorieCalculator, times(1)).calculate(any(), any(), any());
        verify(calorieSafetyEngine,   times(1)).evaluate(any(), any(), any(), any());
        verify(macroCalculator,       times(1)).calculateProtein(any(), any(), any());
        verify(macroCalculator,       times(1)).calculateFat(any());
        verify(macroCalculator,       times(1)).calculateCarbohydrate(any());
        verify(fibreCalculator,       times(1)).calculate(any());
        verify(waterCalculator,       times(1)).calculate(any());
    }

    // ── 28. safetyStatus in response reflects safety layer result ─────

    @Test
    void safetyStatus_reflectsCalorieSafetyEngineResult() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        CalorieSafetyResult adjustedResult = new CalorieSafetyResult(
                PROPOSED, new BigDecimal("1600"),
                CalorieSafetyStatus.ADJUSTED, true, false,
                Collections.emptyList()
        );

        when(healthProfileContextService.getCurrentUserHealthProfile()).thenReturn(hp);
        when(goalCalorieCalculator.calculate(any(), any(), any()))
                .thenReturn(calorieResult());
        when(calorieSafetyEngine.evaluate(any(), any(), any(), any()))
                .thenReturn(adjustedResult);

        ProteinAdaptationResult pr = proteinResult();
        when(macroCalculator.calculateProtein(any(), any(), any())).thenReturn(pr);
        when(macroCalculator.resolveNutritionTargetProtein(pr)).thenReturn(PROTEIN);

        FatCalculationResult fr = fatResult();
        when(macroCalculator.calculateFat(any())).thenReturn(fr);
        when(macroCalculator.resolveNutritionTargetFat(fr)).thenReturn(FAT);

        CarbohydrateCalculationResult cr = carbResult();
        when(macroCalculator.calculateCarbohydrate(any())).thenReturn(cr);
        when(macroCalculator.resolveNutritionTargetCarbohydrate(cr)).thenReturn(CARBS);

        when(fibreCalculator.calculate(new BigDecimal("1600"))).thenReturn(FIBRE);
        when(waterCalculator.calculate(any())).thenReturn(waterResult());

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertEquals(CalorieSafetyStatus.ADJUSTED, r.status());
        assertTrue(r.safetyAdjusted());
    }

    // ── 29. Warnings list is never null ───────────────────────────────

    @Test
    void warningsList_neverNull() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.GENERAL_HEALTH).build();
        HealthProfile hp = healthProfileWith(pp, fg);

        stubHappyPath(hp);

        NutritionTargetResponse r = service.calculateForCurrentUser();

        assertNotNull(r.warnings(), "warnings list must never be null");
    }

    // ── 30. Primary intensity resolved from highest-intensity activity ─

    @Test
    void primaryIntensity_resolvedFromHighestIntensityActivity() {
        PersonalProfile pp = PersonalProfile.builder()
                .weightKg(WEIGHT).heightCm(172.0).gender(Gender.MALE).build();
        FitnessGoalProfile fg = FitnessGoalProfile.builder()
                .primaryGoal(Goal.SPORTS_PERFORMANCE).build();

        ActivityProfile lowActivity = ActivityProfile.builder()
                .activityType(ActivityType.YOGA)
                .intensity(WorkoutIntensity.LOW)
                .daysPerWeek(3)
                .durationMinutes(30)
                .experienceLevel(ExperienceLevel.LESS_THAN_A_YEAR)
                .build();

        ActivityProfile highActivity = ActivityProfile.builder()
                .activityType(ActivityType.RUNNING)
                .intensity(WorkoutIntensity.HIGH)
                .daysPerWeek(4)
                .durationMinutes(60)
                .experienceLevel(ExperienceLevel.FIVE_PLUS_YEARS)
                .build();

        WorkoutProfile wp = WorkoutProfile.builder()
                .activities(new HashSet<>(Set.of(lowActivity, highActivity)))
                .build();

        HealthProfile hp = healthProfileWith(pp, fg);
        hp.setWorkoutProfile(wp);
        wp.setHealthProfile(hp);
        lowActivity.setWorkoutProfile(wp);
        highActivity.setWorkoutProfile(wp);

        stubHappyPath(hp);
        service.calculateForCurrentUser();

        // Protein input should have the highest intensity (HIGH from running)
        verify(macroCalculator).calculateProtein(
                eq(hp),
                argThat(input -> input.workoutIntensity() == WorkoutIntensity.HIGH),
                any()
        );
    }
}
