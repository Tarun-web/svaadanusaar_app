package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.TdeeResult;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Goal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

//Its job is:
//
//        "Given the user's estimated TDEE and their goal, how many calories should they eat?"

@Component
@RequiredArgsConstructor
public class GoalCalorieCalculator {

    private static final BigDecimal KCAL_PER_KG =
            BigDecimal.valueOf(7700);

    /*
     * Safety boundaries.
     *
     * These are deliberately conservative V1 boundaries.
     * They are not medical prescriptions.
     */
    private static final BigDecimal MIN_CALORIE_FLOOR =
            BigDecimal.valueOf(1200);

    private static final BigDecimal MAX_CALORIE_CEILING =
            BigDecimal.valueOf(5000);

    /*
     * Maximum recommended rate as a fraction of
     * current body weight per week.
     *
     * Example:
     *
     * 78 kg × 1% = 0.78 kg/week
     */
    private static final BigDecimal MAX_WEEKLY_CHANGE_PERCENT =
            BigDecimal.valueOf(0.01);

    /*
     * Minimum practical change.
     */
    private static final BigDecimal MIN_WEEKLY_CHANGE =
            BigDecimal.valueOf(0.10);

    /*
     * Conservative V1 calorie adjustment limits.
     */
    private static final BigDecimal MAX_DAILY_DEFICIT =
            BigDecimal.valueOf(1000);

    private static final BigDecimal MAX_DAILY_SURPLUS =
            BigDecimal.valueOf(500);

    private final TdeeCalculator tdeeCalculator;


    /**
     * Calculates the user's calorie target.
     */
    public CalorieTargetResult calculate(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile,
            Collection<com.dietapp.diet_app.health_profile.entity.ActivityProfile> activities
    ) {

        validateInput(
                personalProfile,
                fitnessGoalProfile
        );

        /*
         * ---------------------------------------------------------
         * 1. CURRENT WEIGHT
         * ---------------------------------------------------------
         */
        BigDecimal currentWeight =
                personalProfile.getWeightKg();


        /*
         * ---------------------------------------------------------
         * 2. GOAL
         * ---------------------------------------------------------
         */
        Goal goal =
                fitnessGoalProfile.getPrimaryGoal();


        /*
         * ---------------------------------------------------------
         * 3. TDEE
         * ---------------------------------------------------------
         */
        TdeeResult tdeeResult =
                tdeeCalculator.calculate(
                        personalProfile,
                        activities
                );

        BigDecimal tdee =
                tdeeResult.tdee();


        /*
         * ---------------------------------------------------------
         * 4. TARGET WEIGHT
         * ---------------------------------------------------------
         */
        BigDecimal targetWeight =
                BigDecimal.valueOf(fitnessGoalProfile.getTargetWeightKg());


        /*
         * ---------------------------------------------------------
         * 5. REQUESTED WEEKLY CHANGE
         * ---------------------------------------------------------
         */
        BigDecimal requestedWeeklyChange =
                BigDecimal.valueOf(fitnessGoalProfile
                        .getWeeklyWeightChangeKg());


        /*
         * ---------------------------------------------------------
         * 6. GOAL-SPECIFIC CALCULATION
         * ---------------------------------------------------------
         */
        return switch (goal) {

            case FAT_LOSS ->
                    calculateFatLoss(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            requestedWeeklyChange,
                            tdee
                    );

            case WEIGHT_GAIN ->
                    calculateWeightGain(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            requestedWeeklyChange,
                            tdee
                    );

            case MUSCLE_GAIN ->
                    calculateMuscleGain(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            requestedWeeklyChange,
                            tdee
                    );

            case BODY_RECOMPOSITION ->
                    calculateBodyRecomposition(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            tdee
                    );

            case MAINTENANCE ->
                    calculateMaintenance(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            tdee
                    );

            case GENERAL_HEALTH ->
                    calculateGeneralHealth(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            tdee
                    );

            case SPORTS_PERFORMANCE ->
                    calculateSportsPerformance(
                            currentWeight,
                            targetWeight,
                            fitnessGoalProfile.getTargetDate(),
                            tdee
                    );
        };
    }


    // ============================================================
    // FAT LOSS
    // ============================================================

    private CalorieTargetResult calculateFatLoss(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal requestedWeeklyChange,
            BigDecimal tdee
    ) {

        validateFatLossTarget(
                currentWeight,
                targetWeight
        );

        BigDecimal maximumSafeWeeklyChange =
                currentWeight.multiply(
                        MAX_WEEKLY_CHANGE_PERCENT
                );

        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange,
                        maximumSafeWeeklyChange
                );

        boolean timelineAchievable =
                isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        /*
         * 7700 kcal ≈ 1 kg body-weight change.
         *
         * Daily deficit:
         *
         * weekly kg × 7700 / 7
         */
        BigDecimal dailyDeficit =
                effectiveWeeklyChange
                        .multiply(KCAL_PER_KG)
                        .divide(
                                BigDecimal.valueOf(7),
                                2,
                                RoundingMode.HALF_UP
                        );

        dailyDeficit =
                dailyDeficit.min(
                        MAX_DAILY_DEFICIT
                );

        BigDecimal targetCalories =
                tdee.subtract(
                        dailyDeficit
                );

        targetCalories =
                clampCalories(
                        targetCalories
                );

        return buildResult(
                Goal.FAT_LOSS,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailyDeficit.negate(),
                targetCalories,
                timelineAchievable
        );
    }


    // ============================================================
    // WEIGHT GAIN
    // ============================================================

    private CalorieTargetResult calculateWeightGain(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal requestedWeeklyChange,
            BigDecimal tdee
    ) {

        validateWeightGainTarget(
                currentWeight,
                targetWeight
        );

        BigDecimal maximumWeeklyGain =
                BigDecimal.valueOf(0.50);

        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange,
                        maximumWeeklyGain
                );

        boolean timelineAchievable =
                isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        BigDecimal dailySurplus =
                effectiveWeeklyChange
                        .multiply(KCAL_PER_KG)
                        .divide(
                                BigDecimal.valueOf(7),
                                2,
                                RoundingMode.HALF_UP
                        );

        dailySurplus =
                dailySurplus.min(
                        MAX_DAILY_SURPLUS
                );

        BigDecimal targetCalories =
                tdee.add(
                        dailySurplus
                );

        targetCalories =
                clampCalories(
                        targetCalories
                );

        return buildResult(
                Goal.WEIGHT_GAIN,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailySurplus,
                targetCalories,
                timelineAchievable
        );
    }


    // ============================================================
    // MUSCLE GAIN
    // ============================================================

    private CalorieTargetResult calculateMuscleGain(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal requestedWeeklyChange,
            BigDecimal tdee
    ) {

        /*
         * Muscle gain should use a smaller surplus than
         * unrestricted weight gain.
         */
        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange,
                        BigDecimal.valueOf(0.25)
                );

        boolean timelineAchievable =
                targetWeight == null
                        || isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        BigDecimal dailySurplus =
                effectiveWeeklyChange
                        .multiply(KCAL_PER_KG)
                        .divide(
                                BigDecimal.valueOf(7),
                                2,
                                RoundingMode.HALF_UP
                        );

        dailySurplus =
                dailySurplus.min(
                        BigDecimal.valueOf(400)
                );

        BigDecimal targetCalories =
                clampCalories(
                        tdee.add(
                                dailySurplus
                        )
                );

        return buildResult(
                Goal.MUSCLE_GAIN,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailySurplus,
                targetCalories,
                timelineAchievable
        );
    }


    // ============================================================
    // BODY RECOMPOSITION
    // ============================================================

    private CalorieTargetResult calculateBodyRecomposition(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal tdee
    ) {

        /*
         * Start close to maintenance.
         *
         * Macro distribution and protein become particularly
         * important for recomposition.
         */
        BigDecimal targetCalories =
                clampCalories(
                        tdee
                );

        return buildResult(
                Goal.BODY_RECOMPOSITION,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                targetCalories,
                true
        );
    }


    // ============================================================
    // MAINTENANCE
    // ============================================================

    private CalorieTargetResult calculateMaintenance(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal tdee
    ) {

        return buildResult(
                Goal.MAINTENANCE,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                clampCalories(tdee),
                true
        );
    }


    // ============================================================
    // GENERAL HEALTH
    // ============================================================

    private CalorieTargetResult calculateGeneralHealth(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal tdee
    ) {

        /*
         * General health starts around maintenance.
         *
         * Future versions can adjust this based on:
         * medical profile, body composition, etc.
         */
        BigDecimal targetCalories =
                clampCalories(tdee);

        return buildResult(
                Goal.GENERAL_HEALTH,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                targetCalories,
                true
        );
    }


    // ============================================================
    // SPORTS PERFORMANCE
    // ============================================================

    private CalorieTargetResult calculateSportsPerformance(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal tdee
    ) {

        /*
         * Start with a modest performance surplus.
         *
         * Later this should become training-day /
         * rest-day periodized nutrition.
         */
        BigDecimal dailySurplus =
                BigDecimal.valueOf(200);

        BigDecimal targetCalories =
                clampCalories(
                        tdee.add(
                                dailySurplus
                        )
                );

        return buildResult(
                Goal.SPORTS_PERFORMANCE,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                dailySurplus,
                targetCalories,
                true
        );
    }


    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateInput(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile
    ) {

        if (personalProfile == null) {
            throw new IllegalArgumentException(
                    "Personal profile is required."
            );
        }

        if (personalProfile.getWeightKg() == null) {
            throw new IllegalArgumentException(
                    "Current weight is required."
            );
        }

        if (fitnessGoalProfile == null) {
            throw new IllegalArgumentException(
                    "Fitness goal profile is required."
            );
        }

        if (fitnessGoalProfile.getPrimaryGoal() == null) {
            throw new IllegalArgumentException(
                    "Primary fitness goal is required."
            );
        }

        validatePositive(
                personalProfile.getWeightKg(),
                "Current weight"
        );

        if (fitnessGoalProfile.getTargetDate() != null
                && fitnessGoalProfile.getTargetDate()
                .isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Target date cannot be in the past."
            );
        }

        if (fitnessGoalProfile
                .getWeeklyWeightChangeKg() != null) {

            validatePositive(
                    BigDecimal.valueOf(fitnessGoalProfile
                            .getWeeklyWeightChangeKg()),
                    "Weekly weight change"
            );
        }
    }


    private void validateFatLossTarget(
            BigDecimal currentWeight,
            BigDecimal targetWeight
    ) {

        if (targetWeight == null) {
            throw new IllegalArgumentException(
                    "Target weight is required for fat loss."
            );
        }

        if (targetWeight.compareTo(currentWeight) >= 0) {
            throw new IllegalArgumentException(
                    "Fat-loss target weight must be below current weight."
            );
        }
    }


    private void validateWeightGainTarget(
            BigDecimal currentWeight,
            BigDecimal targetWeight
    ) {

        if (targetWeight == null) {
            throw new IllegalArgumentException(
                    "Target weight is required for weight gain."
            );
        }

        if (targetWeight.compareTo(currentWeight) <= 0) {
            throw new IllegalArgumentException(
                    "Weight-gain target weight must be above current weight."
            );
        }
    }


    // ============================================================
    // WEEKLY CHANGE
    // ============================================================

    private BigDecimal resolveWeeklyChange(
            BigDecimal requested,
            BigDecimal maximumAllowed
    ) {

        if (requested == null) {
            return maximumAllowed.min(
                    BigDecimal.valueOf(0.50)
            );
        }

        if (requested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Weekly weight change must be greater than zero."
            );
        }

        if (requested.compareTo(MIN_WEEKLY_CHANGE) < 0) {
            return MIN_WEEKLY_CHANGE;
        }

        return requested.min(
                maximumAllowed
        );
    }


    // ============================================================
    // TIMELINE
    // ============================================================

    private boolean isTimelineAchievable(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal weeklyChange
    ) {

        if (targetDate == null) {
            return true;
        }

        if (targetWeight == null) {
            return true;
        }

        long days =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        targetDate
                );

        if (days <= 0) {
            return false;
        }

        BigDecimal weeks =
                BigDecimal.valueOf(days)
                        .divide(
                                BigDecimal.valueOf(7),
                                4,
                                RoundingMode.HALF_UP
                        );

        BigDecimal achievableChange =
                weeklyChange.multiply(
                        weeks
                );

        BigDecimal requiredChange =
                currentWeight
                        .subtract(targetWeight)
                        .abs();

        return achievableChange.compareTo(
                requiredChange
        ) >= 0;
    }


    // ============================================================
    // CALORIE SAFETY
    // ============================================================

    private BigDecimal clampCalories(
            BigDecimal calories
    ) {

        if (calories.compareTo(
                MIN_CALORIE_FLOOR
        ) < 0) {

            return MIN_CALORIE_FLOOR;
        }

        if (calories.compareTo(
                MAX_CALORIE_CEILING
        ) > 0) {

            return MAX_CALORIE_CEILING;
        }

        return calories.setScale(
                0,
                RoundingMode.HALF_UP
        );
    }


    // ============================================================
    // RESULT
    // ============================================================

    private CalorieTargetResult buildResult(
            Goal goal,
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate,
            BigDecimal tdee,
            BigDecimal requestedWeeklyChange,
            BigDecimal effectiveWeeklyChange,
            BigDecimal dailyAdjustment,
            BigDecimal targetCalories,
            boolean timelineAchievable
    ) {

        return new CalorieTargetResult(
                goal,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailyAdjustment,
                targetCalories,
                MIN_CALORIE_FLOOR,
                MAX_CALORIE_CEILING,
                timelineAchievable
        );
    }


    private void validatePositive(
            BigDecimal value,
            String field
    ) {

        if (value == null
                || value.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    field + " must be greater than zero."
            );
        }
    }
}