package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "workout_profiles",
        indexes = {
                @Index(
                        name = "idx_workout_profile_health_profile",
                        columnList = "health_profile_id",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutProfile extends BaseEntity {

    /**
     * One Health Profile owns exactly one Workout Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * User's workout activities.
     *
     * Examples:
     * - Weight Training
     * - Running
     * - Cricket
     * - Cycling
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "workoutProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<ActivityProfile> activities = new HashSet<>();

    /**
     * Preferred workout time.
     *
     * Example:
     * 06:00
     * 18:30
     */
    private LocalTime preferredWorkoutTime;

    /**
     * Whether AI should include
     * a pre-workout meal.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean includePreWorkoutMeal = true;

    /**
     * Whether AI should include
     * a post-workout meal.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean includePostWorkoutMeal = true;


    // ============================================================
    // TODO (Future Enhancements)
    // ============================================================

    /*
     * Workout Split
     *
     * PUSH_PULL_LEGS
     * UPPER_LOWER
     * BRO_SPLIT
     * FULL_BODY
     */

    /*
     * Wearable Integrations
     *
     * Garmin
     * Apple Health
     * Google Fit
     * Fitbit
     */

    /*
     * Preferred Gym
     *
     * Home
     * Commercial Gym
     * Outdoor
     */

}