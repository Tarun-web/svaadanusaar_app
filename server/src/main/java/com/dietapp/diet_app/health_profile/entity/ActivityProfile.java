package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
        name = "activity_profiles",
        indexes = {
                @Index(
                        name = "idx_activity_profile_workout_profile",
                        columnList = "workout_profile_id"
                ),
                @Index(
                        name = "idx_activity_profile_activity_type",
                        columnList = "activity_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityProfile extends BaseEntity {

    /**
     * Parent Workout Profile.
     *
     * One Workout Profile can have multiple activities.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workout_profile_id",
            nullable = false
    )
    private WorkoutProfile workoutProfile;

    /**
     * Primary activity.
     *
     * Examples:
     * WEIGHT_TRAINING
     * RUNNING
     * CYCLING
     * SPORTS
     * YOGA
     * SWIMMING
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_type",
            nullable = false
    )
    private ActivityType activityType;

    /**
     * Required only when activityType = SPORTS.
     *
     * Examples:
     * FOOTBALL
     * CRICKET
     * BADMINTON
     * TENNIS
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type")
    private SportType sportType;

    /**
     * User's experience level.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "experience_level",
            nullable = false
    )
    private ExperienceLevel experienceLevel;

    /**
     * Average workout intensity.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "workout_intensity",
            nullable = false
    )
    private WorkoutIntensity intensity;

    /**
     * Number of days per week.
     *
     * Valid range: 1–7
     */
    @NotNull
    @Min(1)
    @Max(7)
    @Column(
            name = "days_per_week",
            nullable = false
    )
    private Integer daysPerWeek;

    /**
     * Average duration of one session.
     *
     * In minutes.
     *
     * Valid range:
     * 5–600 minutes
     */
    @NotNull
    @Min(5)
    @Max(600)
    @Column(
            name = "duration_minutes",
            nullable = false
    )
    private Integer durationMinutes;

    // ============================================================
    // TODO (Future Enhancements)
    // ============================================================

    /*
     * Planned additions:
     *
     * private WorkoutGoal workoutGoal;
     *
     * private Integer averageHeartRate;
     *
     * private Integer averageCaloriesBurned;
     *
     * private Double averageDistanceKm;
     *
     * private Integer averageSteps;
     *
     * private Integer metValue;
     *
     * private String notes;
     */

    /*
     * Wearable Integrations
     *
     * Garmin
     * Apple Health
     * Google Fit
     * Fitbit
     */

}