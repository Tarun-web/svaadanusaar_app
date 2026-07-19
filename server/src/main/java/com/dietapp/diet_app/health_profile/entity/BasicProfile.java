package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.health_profile.enums.Gender;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicProfile {

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double heightCm;

    private Double weightKg;

    private Double bodyFatPercentage;

    private Double waistCm;

    private Double neckCm;

    private Double targetWeightKg;
}