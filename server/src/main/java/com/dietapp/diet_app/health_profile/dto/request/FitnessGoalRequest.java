package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.DietFlexibility;
import com.dietapp.diet_app.health_profile.enums.DietStrictness;
import com.dietapp.diet_app.health_profile.enums.Goal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessGoalRequest {

    @NotNull(message = "Primary goal is required.")
    private Goal primaryGoal;

    private Goal secondaryGoal;

    @DecimalMin(value = "20.0")
    @DecimalMax(value = "300.0")
    private Double targetWeightKg;

    private LocalDate targetDate;

    @DecimalMin(value = "0.10")
    @DecimalMax(value = "2.00")
    private Double weeklyWeightChangeKg;

    @Builder.Default
    private Boolean includeCheatMeals = false;

    @Builder.Default
    private Integer cheatMealsPerWeek = 0;

    private DietStrictness dietStrictness;

    private DietFlexibility dietFlexibility;
}