package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.NutritionTarget.enums.TrainingStatus;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutProfileResponse {

    private UUID id;

    private Set<ActivityResponse> activities = new HashSet<>();

    private LocalTime preferredWorkoutTime;

    private Boolean includePreWorkoutMeal;

    private Boolean includePostWorkoutMeal;

    private DayOfWeek preferredRestDay;

    /** The user's resistance-training experience level. {@code null} when not yet provided. */
    private TrainingStatus trainingStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}