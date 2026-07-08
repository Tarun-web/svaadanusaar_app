package com.dietapp.diet_app.HealthProfile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileResponse {

    private UUID userId;
    private Integer age;
    private String gender;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String goal;
    private String trainingLevel;
    private String activityLevel;
    private String dietType;
    private Integer mealsPerDay;
    private String origin;
    private String[] allergies;
    private String[] diseases;
}
