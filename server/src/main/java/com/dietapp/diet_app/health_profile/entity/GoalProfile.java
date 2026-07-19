package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.health_profile.enums.DietFlexibility;
import com.dietapp.diet_app.health_profile.enums.DietStrictness;
import com.dietapp.diet_app.health_profile.enums.Goal;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalProfile {

    @Enumerated(EnumType.STRING)
    private Goal primaryGoal;

    @Enumerated(EnumType.STRING)
    private Goal secondaryGoal;

    private LocalDate targetDate;

    private Boolean includeCheatMeals;

    private Integer cheatMealsPerWeek;

    @Enumerated(EnumType.STRING)
    private DietStrictness dietStrictness;

    @Enumerated(EnumType.STRING)
    private DietFlexibility dietFlexibility;
}
