package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityProfile {
    private ActivityType activityType;

    private SportType sportType; // only if SPORTS

    private ExperienceLevel experienceLevel;

    private WorkoutIntensity intensity;

    private Integer daysPerWeek;

    private Integer durationMinutes;
}
