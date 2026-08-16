package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.ActivityType;
import com.dietapp.diet_app.health_profile.enums.ExperienceLevel;
import com.dietapp.diet_app.health_profile.enums.SportType;
import com.dietapp.diet_app.health_profile.enums.WorkoutIntensity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

    private UUID id;

    private ActivityType activityType;

    private SportType sportType;

    private ExperienceLevel experienceLevel;

    private WorkoutIntensity intensity;

    private Integer daysPerWeek;

    private Integer durationMinutes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}