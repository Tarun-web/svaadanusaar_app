package com.dietapp.diet_app.health_profile.dto.request;

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
     * Parent Health Profile.
     */
    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    /**
     * User workout activities.
     */
    @Builder.Default
    @Valid
    private Set<ActivityRequest> activities = new HashSet<>();

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

}