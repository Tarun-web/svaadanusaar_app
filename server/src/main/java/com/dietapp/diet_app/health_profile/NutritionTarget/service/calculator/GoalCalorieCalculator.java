package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieTargetResult;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.TdeeResult;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Goal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class GoalCalorieCalculator {

    /**
     * Approximate energy equivalent of 1 kg of body weight.
     *
     * This is a planning approximation, NOT a physiological constant.
     */
    private static final BigDecimal KCAL_PER_KG =
            BigDecimal.valueOf(7700);

    /**
     * Default weekly change when the user does not specify one.
     *
     * This is deliberately conservative because this class only
     * creates a calorie proposal. Final safety validation happens
     * inside CalorieSafetyEngine.
     */
    private static final BigDecimal DEFAULT_WEEKLY_CHANGE =
            BigDecimal.valueOf(0.50);

    /**
     * Minimum meaningful weekly change accepted by the calculator.
     */
    private static final BigDecimal MIN_WEEKLY_CHANGE =
            BigDecimal.valueOf(0.05);

    /**
     * Maximum surplus used specifically for muscle-gain proposals.
     *
     * This is a calculation-policy limit, not a medical safety limit.
     */
    private static final BigDecimal MAX_MUSCLE_GAIN_SURPLUS =
            BigDecimal.valueOf(400);

    private final TdeeCalculator tdeeCalculator;


    /**
     * Calculates a proposed calorie target.
     *
     * IMPORTANT:
     *
     * This class does NOT decide whether the final calorie target
     * is medically or physiologically safe.
     *
     * It calculates the mathematical calorie proposal.
     *
     * CalorieSafetyEngine is responsible for evaluating that proposal.
     */
    public CalorieTargetResult calculate(
            PersonalProfile personalProfile,
            FitnessGoalProfile fitnessGoalProfile,
            MedicalProfile medicalProfile,
            Collection<ActivityProfile> activities
    ) {

        validateInput(
                personalProfile,
                fitnessGoalProfile
        );

        BigDecimal currentWeight =
                personalProfile.getWeightKg();

        Goal goal =
                fitnessGoalProfile.getPrimaryGoal();

        /*
         * ---------------------------------------------------------
         * TDEE
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
         * Optional target values
         * ---------------------------------------------------------
         *
         * Do not blindly convert null to BigDecimal.
         */

        BigDecimal targetWeight =
                toBigDecimal(
                        fitnessGoalProfile.getTargetWeightKg()
                );

        BigDecimal requestedWeeklyChange =
                toBigDecimal(
                        fitnessGoalProfile.getWeeklyWeightChangeKg()
                );

        LocalDate targetDate =
                fitnessGoalProfile.getTargetDate();

        /*
         * ---------------------------------------------------------
         * GOAL-SPECIFIC CALCULATION
         * ---------------------------------------------------------
         */

        return switch (goal) {

            case FAT_LOSS ->
                    calculateFatLoss(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            requestedWeeklyChange,
                            tdee
                    );

            case WEIGHT_GAIN ->
                    calculateWeightGain(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            requestedWeeklyChange,
                            tdee
                    );

            case MUSCLE_GAIN ->
                    calculateMuscleGain(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            requestedWeeklyChange,
                            tdee
                    );

            case BODY_RECOMPOSITION ->
                    calculateBodyRecomposition(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            tdee
                    );

            case MAINTENANCE ->
                    calculateMaintenance(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            tdee
                    );

            case GENERAL_HEALTH ->
                    calculateGeneralHealth(
                            currentWeight,
                            targetWeight,
                            targetDate,
                            tdee
                    );

            case SPORTS_PERFORMANCE ->
                    calculateSportsPerformance(
                            currentWeight,
                            targetWeight,
                            targetDate,
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
                targetWeight,
                targetDate
        );

        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange
                );

        boolean timelineAchievable =
                isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        BigDecimal dailyDeficit =
                weeklyChangeToDailyCalories(
                        effectiveWeeklyChange
                );

        BigDecimal proposedCalories =
                tdee.subtract(dailyDeficit);

        return buildResult(
                Goal.FAT_LOSS,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailyDeficit.negate(),
                proposedCalories,
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
                targetWeight,
                targetDate
        );

        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange
                );

        boolean timelineAchievable =
                isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        BigDecimal dailySurplus =
                weeklyChangeToDailyCalories(
                        effectiveWeeklyChange
                );

        BigDecimal proposedCalories =
                tdee.add(dailySurplus);

        return buildResult(
                Goal.WEIGHT_GAIN,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailySurplus,
                proposedCalories,
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

        BigDecimal effectiveWeeklyChange =
                resolveWeeklyChange(
                        requestedWeeklyChange
                );

        boolean timelineAchievable =
                targetWeight == null
                        || targetDate == null
                        || isTimelineAchievable(
                        currentWeight,
                        targetWeight,
                        targetDate,
                        effectiveWeeklyChange
                );

        BigDecimal dailySurplus =
                weeklyChangeToDailyCalories(
                        effectiveWeeklyChange
                );

        /*
         * Muscle gain should normally use a modest surplus.
         *
         * This is not the safety engine. It simply prevents
         * the muscle-gain proposal from becoming excessively
         * large due to a user-entered weekly rate.
         */

        dailySurplus =
                dailySurplus.min(
                        MAX_MUSCLE_GAIN_SURPLUS
                );

        BigDecimal proposedCalories =
                tdee.add(dailySurplus);

        return buildResult(
                Goal.MUSCLE_GAIN,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                requestedWeeklyChange,
                effectiveWeeklyChange,
                dailySurplus,
                proposedCalories,
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
         * Start at maintenance.
         *
         * Macro distribution, especially protein,
         * will become important in the macro engine.
         */

        return buildResult(
                Goal.BODY_RECOMPOSITION,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                tdee,
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
                tdee,
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
         * Medical considerations are handled by the
         * CalorieSafetyEngine.
         */

        return buildResult(
                Goal.GENERAL_HEALTH,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                tdee,
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
         * Later this should become training-day/rest-day
         * periodization.
         */

        BigDecimal dailySurplus =
                BigDecimal.valueOf(200);

        BigDecimal proposedCalories =
                tdee.add(dailySurplus);

        return buildResult(
                Goal.SPORTS_PERFORMANCE,
                currentWeight,
                targetWeight,
                targetDate,
                tdee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                dailySurplus,
                proposedCalories,
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

        if (personalProfile.getWeightKg() == null) {
            throw new IllegalArgumentException(
                    "Current weight is required."
            );
        }

        validatePositive(
                personalProfile.getWeightKg(),
                "Current weight"
        );

        /*
         * Validate target date globally if supplied.
         */

        LocalDate targetDate =
                fitnessGoalProfile.getTargetDate();

        if (targetDate != null
                && targetDate.isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Target date cannot be in the past."
            );
        }

        /*
         * Weekly change, when supplied, must be positive.
         */

        BigDecimal weeklyChange =
                toBigDecimal(
                        fitnessGoalProfile
                                .getWeeklyWeightChangeKg()
                );

        if (weeklyChange != null) {

            validatePositive(
                    weeklyChange,
                    "Weekly weight change"
            );
        }
    }


    private void validateFatLossTarget(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate
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

        validateTargetDateForWeightGoal(
                targetDate,
                "Fat-loss"
        );
    }


    private void validateWeightGainTarget(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            LocalDate targetDate
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

        validateTargetDateForWeightGoal(
                targetDate,
                "Weight-gain"
        );
    }


    private void validateTargetDateForWeightGoal(
            LocalDate targetDate,
            String goalName
    ) {

        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "Target date is required for "
                            + goalName.toLowerCase()
                            + "."
            );
        }

        if (!targetDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    goalName
                            + " target date must be in the future."
            );
        }
    }


    // ============================================================
    // WEEKLY CHANGE
    // ============================================================

    private BigDecimal resolveWeeklyChange(
            BigDecimal requested
    ) {

        /*
         * If the user didn't provide a rate,
         * use a conservative default.
         */

        if (requested == null) {
            return DEFAULT_WEEKLY_CHANGE;
        }

        if (requested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Weekly weight change must be greater than zero."
            );
        }

        /*
         * Do not silently turn a tiny user-entered value
         * into a different target.
         *
         * The only reason for a minimum here is to prevent
         * numerical noise.
         */

        if (requested.compareTo(MIN_WEEKLY_CHANGE) < 0) {
            return MIN_WEEKLY_CHANGE;
        }

        return requested;
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

        if (targetDate == null || targetWeight == null) {
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
                weeklyChange.multiply(weeks);

        BigDecimal requiredChange =
                currentWeight
                        .subtract(targetWeight)
                        .abs();

        return achievableChange.compareTo(
                requiredChange
        ) >= 0;
    }


    // ============================================================
    // CALORIE CONVERSION
    // ============================================================

    private BigDecimal weeklyChangeToDailyCalories(
            BigDecimal weeklyChange
    ) {

        return weeklyChange
                .multiply(KCAL_PER_KG)
                .divide(
                        BigDecimal.valueOf(7),
                        2,
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
            BigDecimal proposedCalories,
            boolean timelineAchievable
    ) {

        return CalorieTargetResult.builder()
                .goal(goal)
                .currentWeightKg(currentWeight)
                .targetWeightKg(targetWeight)
                .targetDate(targetDate)
                .tdee(tdee)
                .requestedWeeklyWeightChangeKg(
                        requestedWeeklyChange
                )
                .effectiveWeeklyWeightChangeKg(
                        effectiveWeeklyChange
                )
                .dailyCalorieAdjustment(
                        dailyAdjustment
                )
                .proposedCalories(
                        proposedCalories
                                .setScale(
                                        0,
                                        RoundingMode.HALF_UP
                                )
                )
                .timelineAchievable(
                        timelineAchievable
                )
                .build();
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private BigDecimal toBigDecimal(
            Double value
    ) {

        return value == null
                ? null
                : BigDecimal.valueOf(value);
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
                    field
                            + " must be greater than zero."
            );
        }
    }
}