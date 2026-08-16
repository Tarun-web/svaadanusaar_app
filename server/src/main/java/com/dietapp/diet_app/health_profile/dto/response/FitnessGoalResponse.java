package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.DietFlexibility;
import com.dietapp.diet_app.health_profile.enums.DietStrictness;
import com.dietapp.diet_app.health_profile.enums.Goal;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessGoalResponse {

    private UUID id;

    private UUID healthProfileId;

    private Goal primaryGoal;

    private Goal secondaryGoal;

    private Double targetWeightKg;

    private LocalDate targetDate;

    private Double weeklyWeightChangeKg;

    private Boolean includeCheatMeals;

    private Integer cheatMealsPerWeek;

    private DietStrictness dietStrictness;

    private DietFlexibility dietFlexibility;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}