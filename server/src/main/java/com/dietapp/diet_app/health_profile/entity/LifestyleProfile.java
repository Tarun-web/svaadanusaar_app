package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.health_profile.enums.LivingArrangement;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.Region;
import com.dietapp.diet_app.health_profile.enums.State;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalTime;


@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifestyleProfile {

    @Enumerated(EnumType.STRING)
    private Occupation occupation;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    private State state;

    @Enumerated(EnumType.STRING)
    private LivingArrangement livingArrangement;

    private Integer dailySteps;

    private LocalTime wakeUpTime;

    private LocalTime sleepTime;

    private LocalTime breakfastTime;

    private LocalTime lunchTime;

    private LocalTime dinnerTime;

}
