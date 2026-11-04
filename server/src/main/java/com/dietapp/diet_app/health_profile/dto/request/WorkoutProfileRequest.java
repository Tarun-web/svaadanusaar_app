package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.TrainingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutProfileRequest {

    /**
     * Preferred workout time.
     */
    private LocalTime preferredWorkoutTime;

    /**
     * Include pre-workout meal.
     */
    @Builder.Default
    private Boolean includePreWorkoutMeal = true;

    /**
     * Include post-workout meal.
     */
    @Builder.Default
    private Boolean includePostWorkoutMeal = true;

    /**
     * Weekly rest day.
     */
    private DayOfWeek preferredRestDay;

    /**
     * The user's resistance-training experience level.
     *
     * <p>Optional — send {@code null} (or omit the field) when unknown.
     * The server will store {@code NULL} and no default will be applied.</p>
     */
    private TrainingStatus trainingStatus;

}