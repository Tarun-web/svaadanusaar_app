package com.dietapp.diet_app.health_profile.dto.response;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricsResponse {

    private Double bmi;

    private Double bmr;

    private Double estimatedTdee;

    private Double recommendedCalories;

    private Double recommendedProtein;

    private Double recommendedCarbs;

    private Double recommendedFat;
}