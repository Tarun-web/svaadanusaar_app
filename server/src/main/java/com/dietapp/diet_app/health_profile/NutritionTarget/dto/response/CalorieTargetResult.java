package com.dietapp.diet_app.health_profile.NutritionTarget.dto.response;
import com.dietapp.diet_app.health_profile.enums.Goal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalorieTargetResult {

    private Goal goal;

    private BigDecimal currentWeightKg;

    private BigDecimal targetWeightKg;

    private LocalDate targetDate;

    private BigDecimal tdee;

    private BigDecimal requestedWeeklyWeightChangeKg;

    private BigDecimal effectiveWeeklyWeightChangeKg;

    private BigDecimal dailyCalorieAdjustment;

    /**
     * Calories proposed by GoalCalorieCalculator
     * BEFORE medical/body-size safety evaluation.
     */
    private BigDecimal proposedCalories;

    private boolean timelineAchievable;
}