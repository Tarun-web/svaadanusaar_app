package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.StressLevel;
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
public class BehaviourProfile {

    private Double waterIntakeLitres;

    private Double sleepHours;

    @Enumerated(EnumType.STRING)
    private StressLevel stressLevel;

    private Boolean skipBreakfast;

    private Boolean lateNightEating;

    private Boolean alcohol;

    private Boolean smoking;

}
