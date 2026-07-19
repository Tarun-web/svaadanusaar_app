package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutProfile {

    @ElementCollection
    List<ActivityProfile> activities;
    private Integer averageDailySteps;

    private LocalTime workoutTime;

    private Boolean includePreWorkoutMeal;

    private Boolean includePostWorkoutMeal;

    private Integer currentProteinIntake;

}
