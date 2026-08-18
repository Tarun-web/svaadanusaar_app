package com.dietapp.diet_app.health_profile.dto.response;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}