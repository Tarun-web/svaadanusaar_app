package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequest {

    /**
     * Primary activity.
     */
    @NotNull
    private ActivityType activityType;

    /**
     * Required only if activityType = SPORTS.
     */
    private SportType sportType;

    /**
     * Experience level.
     */
    @NotNull
    private ExperienceLevel experienceLevel;

    /**
     * Workout intensity.
     */
    @NotNull
    private WorkoutIntensity intensity;

    /**
     * Days per week.
     */
    @NotNull
    @Min(1)
    @Max(7)
    private Integer daysPerWeek;

    /**
     * Duration of one workout session.
     *
     * Minutes.
     */
    @NotNull
    @Min(5)
    @Max(600)
    private Integer durationMinutes;

}