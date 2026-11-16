package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.common.exception.IncompleteProfileException;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.*;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.*;
import com.dietapp.diet_app.health_profile.entity.*;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Orchestrates the full nutrition target pipeline for the current user.
 *
 * <h3>Pipeline order</h3>
 * <pre>
 *   HealthProfile
 *       ↓
 *   GoalCalorieCalculator   →  CalorieTargetResult
 *       ↓
 *   CalorieSafetyEngine     →  CalorieSafetyResult  (finalCalories = macro budget)
 *       ↓
 *   MacroCalculator
 *       ├── ProteinCalculator + ProteinAdaptationService  →  proteinGrams (current)
 *       ├── FatCalculator                                 →  fatGrams
 *       └── CarbohydrateCalculator                        →  carbohydrateGrams
 *       ↓
 *   FibreCalculator         →  fibreGrams
 *       ↓
 *   WaterCalculator         →  waterMl
 *       ↓
 *   NutritionTargetResponse (all macros + warnings)
 * </pre>
 *
 * <h3>Key rules</h3>
 * <ul>
 *   <li>Macros are always budgeted against {@code safetyResult.finalCalories()},
 *       never the original proposed calories.</li>
 *   <li>{@code NutritionTargetResponse.proteinGrams} = current adaptation target,
 *       not the long-term target.</li>
 *   <li>Fat feasibility is always checked against the current protein target.</li>
 *   <li>All warnings from all pipeline stages are aggregated into a single list
 *       with no duplicates (by code).</li>
 *   <li>{@code ProteinAdaptationService.initialise()} writes to the database,
 *       so the class-level {@code @Transactional(readOnly=true)} is overridden
 *       on {@link #calculateForCurrentUser()} with a writable transaction.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionTargetServiceImpl
        implements NutritionTargetService {

    private final HealthProfileContextService healthProfileContextService;

    private final GoalCalorieCalculator              goalCalorieCalculator;
    private final CalorieSafetyEngine                calorieSafetyEngine;
    private final MacroCalculator                    macroCalculator;
    private final FibreCalculator                    fibreCalculator;
    private final WaterCalculator                    waterCalculator;
    private final ProteinAdaptationProfileRepository proteinAdaptationProfileRepository;


    // ================================================================
    // calculateForCurrentUser()
    // ================================================================

    /**
     * Calculates the full nutrition target for the currently authenticated user.
     *
     * <p>Annotated with {@code @Transactional} (no {@code readOnly}) to allow
     * {@code ProteinAdaptationService.initialise()} to write the
     * {@link ProteinAdaptationProfile} when one does not yet exist.</p>
     */
    @Override
    @Transactional
    public NutritionTargetResponse calculateForCurrentUser() {

        /*
         * ---------------------------------------------------------
         * 1. LOAD HEALTH PROFILE
         * ---------------------------------------------------------
         */
        HealthProfile healthProfile =
                healthProfileContextService.getCurrentUserHealthProfile();

        if (healthProfile == null) {
            throw new IllegalStateException(
                    "Health profile is required to calculate nutrition targets."
            );
        }

        /*
         * ---------------------------------------------------------
         * 2. EXTRACT PROFILE SECTIONS
         * ---------------------------------------------------------
         */
        PersonalProfile     personalProfile     = healthProfile.getPersonalProfile();
        FitnessGoalProfile  fitnessGoalProfile  = healthProfile.getFitnessGoalProfile();
        MedicalProfile      medicalProfile      = healthProfile.getMedicalProfile();

        validateProfile(personalProfile, fitnessGoalProfile);

        // Activities (null-safe)
        Set<ActivityProfile> activities = resolveActivities(healthProfile);

        /*
         * ---------------------------------------------------------
         * 3. CALORIE PIPELINE
         * ---------------------------------------------------------
         */
        CalorieTargetResult calorieProposal =
                goalCalorieCalculator.calculate(
                        personalProfile,
                        fitnessGoalProfile,
                        activities
                );

        CalorieSafetyResult safetyResult =
                calorieSafetyEngine.evaluate(
                        personalProfile,
                        fitnessGoalProfile,
                        medicalProfile,
                        calorieProposal
                );

        // Final macro budget — always use finalCalories, never originalCalories
        BigDecimal finalCalories = safetyResult.finalCalories();

        /*
         * ---------------------------------------------------------
         * 4. RESOLVE TRAINING CONTEXT
         * ---------------------------------------------------------
         */
        boolean          resistanceTraining = resolveResistanceTraining(activities);
        WorkoutIntensity primaryIntensity   = resolvePrimaryIntensity(activities);
        SportType        sportType          = resolveSportType(activities);
        Goal             goal               = fitnessGoalProfile.getPrimaryGoal();
        BigDecimal       weightKg           = personalProfile.getWeightKg();

        Set<com.dietapp.diet_app.health_profile.enums.MedicalCondition> medicalConditions =
                (medicalProfile != null && medicalProfile.getMedicalConditions() != null)
                        ? medicalProfile.getMedicalConditions()
                        : Collections.emptySet();

        BigDecimal currentProteinGrams = resolveCurrentProteinGrams(healthProfile);

        /*
         * ---------------------------------------------------------
         * 5. PROTEIN CALCULATION
         * ---------------------------------------------------------
         */
        ProteinCalculationInput proteinInput = new ProteinCalculationInput(
                weightKg,
                null,           // no bodyFatPercentage in PersonalProfile
                goal,
                resistanceTraining,
                primaryIntensity,
                null,           // no trainingStatus in any profile
                currentProteinGrams,
                medicalConditions
        );

        // Load the existing adaptation profile (null if none yet)
        ProteinAdaptationProfile existingAdaptation =
                proteinAdaptationProfileRepository
                        .findByHealthProfile(healthProfile)
                        .orElse(null);

        ProteinAdaptationResult proteinResult =
                macroCalculator.calculateProtein(
                        healthProfile,
                        proteinInput,
                        existingAdaptation
                );

        BigDecimal proteinGrams = macroCalculator
                .resolveNutritionTargetProtein(proteinResult);

        /*
         * ---------------------------------------------------------
         * 6. FAT CALCULATION
         * ---------------------------------------------------------
         */
        FatCalculationInput fatInput = new FatCalculationInput(
                weightKg,
                goal,
                finalCalories,
                proteinGrams,       // current adaptation target — NOT long-term
                resistanceTraining,
                primaryIntensity,
                sportType,
                medicalConditions
        );

        FatCalculationResult fatResult =
                macroCalculator.calculateFat(fatInput);

        BigDecimal fatGrams = macroCalculator
                .resolveNutritionTargetFat(fatResult);

        /*
         * ---------------------------------------------------------
         * 7. CARBOHYDRATE CALCULATION
         * ---------------------------------------------------------
         */
        CarbohydrateCalculationResult carbResult =
                macroCalculator.calculateCarbohydrate(fatResult);

        BigDecimal carbohydrateGrams = macroCalculator
                .resolveNutritionTargetCarbohydrate(carbResult);

        /*
         * ---------------------------------------------------------
         * 8. FIBRE CALCULATION
         * ---------------------------------------------------------
         */
        BigDecimal fibreGrams = fibreCalculator.calculate(finalCalories);

        /*
         * ---------------------------------------------------------
         * 9. WATER CALCULATION
         * ---------------------------------------------------------
         */
        WaterCalculationInput waterInput = new WaterCalculationInput(
                weightKg,
                activities
        );

        WaterCalculationResult waterResult = waterCalculator.calculate(waterInput);

        BigDecimal waterMl = waterResult.waterMl();

        /*
         * ---------------------------------------------------------
         * 10. AGGREGATE WARNINGS
         * ---------------------------------------------------------
         * Calorie-layer warnings come as List<CalorieWarning>.
         * Protein, fat, carb, and water warnings have their own types
         * (ProteinWarning, FatWarning, etc.). We aggregate all into
         * the response as CalorieWarning for a uniform contract.
         * The warnings list uses message de-duplication by warning code.
         * ---------------------------------------------------------
         */
        List<CalorieWarning> allWarnings = aggregateWarnings(
                safetyResult,
                proteinResult,
                fatResult,
                carbResult,
                waterResult
        );

        /*
         * ---------------------------------------------------------
         * 11. BUILD RESPONSE
         * ---------------------------------------------------------
         */
        return NutritionTargetResponse.builder()

                // Calorie pipeline
                .tdee(calorieProposal.getTdee())
                .proposedCalories(safetyResult.originalCalories())
                .finalCalories(finalCalories)
                .safetyAdjusted(safetyResult.safetyAdjusted())
                .status(safetyResult.status())
                .requiresClinicalReview(
                        safetyResult.requiresClinicalReview()
                        || proteinResult.requiresClinicalReview()
                        || fatResult.requiresClinicalReview()
                        || carbResult.requiresClinicalReview()
                )

                // Macro targets
                .proteinGrams(proteinGrams)
                .longTermProteinGrams(proteinResult.longTermTargetGrams())
                .fatGrams(fatGrams)
                .carbohydrateGrams(carbohydrateGrams)
                .fibreGrams(fibreGrams)
                .waterMl(waterMl)

                // Protein adaptation
                .adaptationStage(proteinResult.adaptationStage())
                .rampUpRequired(proteinResult.rampUpRequired())
                .nextEligibleProteinIncreaseDate(
                        proteinResult.nextEligibleIncreaseDate()
                )
                .currentProteinIntakeGrams(currentProteinGrams)
                .adaptationBaselineProteinGrams(
                        proteinResult.adaptationBaselineProteinGrams())
                .adaptationBaselineOutOfDate(isBaselineOutOfDate(
                        currentProteinGrams,
                        proteinResult.adaptationBaselineProteinGrams()))

                // Aggregated warnings
                .warnings(allWarnings)

                .build();
    }


    // ================================================================
    // Private helpers
    // ================================================================

    /**
     * Resolves the {@link ActivityProfile} set, returning an empty set
     * when no workout profile or activities exist.
     */
    private Set<ActivityProfile> resolveActivities(HealthProfile healthProfile) {
        WorkoutProfile workoutProfile = healthProfile.getWorkoutProfile();
        if (workoutProfile == null || workoutProfile.getActivities() == null) {
            return Collections.emptySet();
        }
        return workoutProfile.getActivities();
    }

    /**
     * Returns {@code true} when any activity has
     * {@code activityType == GYM} (resistance / weight training).
     */
    private boolean resolveResistanceTraining(Set<ActivityProfile> activities) {
        if (activities == null) return false;
        return activities.stream()
                .anyMatch(a -> a.getActivityType() == ActivityType.GYM);
    }

    /**
     * Returns the intensity of the "primary" activity.
     *
     * <p>Selection rule: the activity with the highest intensity
     * (ATHLETE &gt; HIGH &gt; MODERATE &gt; LOW).  When two activities share
     * the same intensity, the one with more days per week wins.
     * Returns {@code null} when there are no valid activities.</p>
     */
    private WorkoutIntensity resolvePrimaryIntensity(Set<ActivityProfile> activities) {
        if (activities == null || activities.isEmpty()) return null;

        return activities.stream()
                .filter(a -> a.getIntensity() != null && a.getDaysPerWeek() != null)
                .max(Comparator
                        .comparingInt((ActivityProfile a) -> intensityRank(a.getIntensity()))
                        .thenComparingInt(a -> Optional.ofNullable(a.getDaysPerWeek()).orElse(0))
                )
                .map(ActivityProfile::getIntensity)
                .orElse(null);
    }

    /**
     * Returns the sport type from the first SPORTS activity, or {@code null}
     * when no SPORTS activity exists.
     */
    private SportType resolveSportType(Set<ActivityProfile> activities) {
        if (activities == null) return null;
        return activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.SPORTS
                          && a.getSportType() != null)
                .map(ActivityProfile::getSportType)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns {@code true} when exactly one value is {@code null} or both
     * are non-null and numerically different; {@code false} when both are
     * {@code null} or numerically equal.
     */
    static boolean isBaselineOutOfDate(BigDecimal latestReported, BigDecimal baseline) {
        if (latestReported == null || baseline == null) {
            return latestReported != baseline;
        }
        return latestReported.compareTo(baseline) != 0;
    }

    /**
     * Extracts the user's current protein intake from
     * {@code HealthProfile.currentNutritionProfile}.
     *
     * <p>Returns {@code null} when the profile or the field is absent —
     * the adaptation engine must NOT invent a value.</p>
     */
    private BigDecimal resolveCurrentProteinGrams(HealthProfile healthProfile) {
        CurrentNutritionProfile cnp = healthProfile.getCurrentNutritionProfile();
        if (cnp == null) return null;
        return cnp.getCurrentProteinGrams();
    }

    /**
     * Maps a {@link WorkoutIntensity} to a numeric rank for comparison.
     */
    private int intensityRank(WorkoutIntensity intensity) {
        if (intensity == null) return 0;
        return switch (intensity) {
            case LOW      -> 1;
            case MODERATE -> 2;
            case HIGH     -> 3;
            case ATHLETE  -> 4;
        };
    }

    /**
     * Aggregates all pipeline warnings into a single de-duplicated
     * {@code List<CalorieWarning>}.
     *
     * <p>Each warning from the protein/fat/carb/water layers is wrapped
     * as a {@link CalorieWarning} using the warning's code name and message.
     * Duplicate codes (same string representation) are suppressed so
     * the same message is never repeated.</p>
     */
    private List<CalorieWarning> aggregateWarnings(
            CalorieSafetyResult   safetyResult,
            ProteinAdaptationResult proteinResult,
            FatCalculationResult  fatResult,
            CarbohydrateCalculationResult carbResult,
            WaterCalculationResult waterResult) {

        List<CalorieWarning> all = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // Calorie-safety layer warnings (already CalorieWarning)
        if (safetyResult.warnings() != null) {
            for (CalorieWarning w : safetyResult.warnings()) {
                String key = w.code().name();
                if (seen.add(key)) {
                    all.add(w);
                }
            }
        }

        // Protein adaptation warnings → wrapped as CalorieWarning
        if (proteinResult.warnings() != null) {
            for (var w : proteinResult.warnings()) {
                String key = "PROTEIN_" + w.code().name();
                if (seen.add(key)) {
                    all.add(new CalorieWarning(
                            com.dietapp.diet_app.health_profile.NutritionTarget.enums
                                    .CalorieWarningCode.GENERAL_ADVISORY,
                            "[Protein] " + w.message()
                    ));
                }
            }
        }

        // Fat warnings → wrapped as CalorieWarning
        if (fatResult.warnings() != null) {
            for (var w : fatResult.warnings()) {
                String key = "FAT_" + w.code().name();
                if (seen.add(key)) {
                    all.add(new CalorieWarning(
                            com.dietapp.diet_app.health_profile.NutritionTarget.enums
                                    .CalorieWarningCode.GENERAL_ADVISORY,
                            "[Fat] " + w.message()
                    ));
                }
            }
        }

        // Carbohydrate warnings → wrapped as CalorieWarning
        if (carbResult.warnings() != null) {
            for (var w : carbResult.warnings()) {
                String key = "CARB_" + w.code().name();
                if (seen.add(key)) {
                    all.add(new CalorieWarning(
                            com.dietapp.diet_app.health_profile.NutritionTarget.enums
                                    .CalorieWarningCode.GENERAL_ADVISORY,
                            "[Carbohydrate] " + w.message()
                    ));
                }
            }
        }

        // Water warnings → wrapped as CalorieWarning
        if (waterResult.warnings() != null) {
            for (var w : waterResult.warnings()) {
                String key = "WATER_" + w.code().name();
                if (seen.add(key)) {
                    all.add(new CalorieWarning(
                            com.dietapp.diet_app.health_profile.NutritionTarget.enums
                                    .CalorieWarningCode.GENERAL_ADVISORY,
                            "[Water] " + w.message()
                    ));
                }
            }
        }

        return Collections.unmodifiableList(all);
    }

    /**
     * Validates the minimum required profile data for a nutrition calculation.
     */
    private void validateProfile(
            PersonalProfile    personalProfile,
            FitnessGoalProfile fitnessGoalProfile) {

        // IncompleteProfileException extends IllegalStateException and is
        // mapped to a 4xx; the field names are fixed, client-safe identifiers.
        if (personalProfile == null) {
            throw new IncompleteProfileException(
                    "Personal profile is incomplete.", List.of("personalProfile"));
        }
        if (fitnessGoalProfile == null) {
            throw new IncompleteProfileException(
                    "Fitness goal profile is incomplete.", List.of("fitnessGoalProfile"));
        }
        if (personalProfile.getWeightKg() == null) {
            throw new IncompleteProfileException(
                    "Current weight is required.", List.of("weightKg"));
        }
        if (personalProfile.getHeightCm() == null) {
            throw new IncompleteProfileException(
                    "Height is required.", List.of("heightCm"));
        }
        if (personalProfile.getGender() == null) {
            throw new IncompleteProfileException(
                    "Gender is required.", List.of("gender"));
        }
    }
}
