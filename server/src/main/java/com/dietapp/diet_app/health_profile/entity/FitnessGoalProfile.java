package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.DietFlexibility;
import com.dietapp.diet_app.health_profile.enums.DietStrictness;
import com.dietapp.diet_app.health_profile.enums.Goal;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "fitness_goal_profiles",
        indexes = {
                @Index(
                        name = "idx_fitness_goal_health_profile",
                        columnList = "health_profile_id",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessGoalProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal primaryGoal;

    @Enumerated(EnumType.STRING)
    private Goal secondaryGoal;

    /**
     * Desired body weight.
     */
    private Double targetWeightKg;

    /**
     * User's desired completion date.
     */
    private LocalDate targetDate;

    /**
     * Preferred weekly change.
     * Examples:
     * 0.25
     * 0.50
     * 0.75
     * 1.00
     */
    @DecimalMin("0.10")
    @DecimalMax("2.00")
    private Double weeklyWeightChangeKg;

    @Builder.Default
    @Column(nullable = false)
    private Boolean includeCheatMeals = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer cheatMealsPerWeek = 0;

    @Enumerated(EnumType.STRING)
    private DietStrictness dietStrictness;

    @Enumerated(EnumType.STRING)
    private DietFlexibility dietFlexibility;

}