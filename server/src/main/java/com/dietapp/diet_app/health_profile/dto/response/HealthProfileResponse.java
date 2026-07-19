package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.entity.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileResponse {

    private UUID id;

    private UUID userId;

    private BasicProfile basicProfile;

    private GoalProfile goalProfile;

    private LifestyleProfile lifestyleProfile;

    private DietPreferenceProfile dietPreferenceProfile;

    private CookingProfile cookingProfile;

    private MedicalProfile medicalProfile;

    private SupplementProfile supplementProfile;

    private BehaviourProfile behaviourProfile;

    private List<ActivityProfile> activities;

    private Integer profileCompletion;

    private HealthMetricsResponse metrics;

    private List<HealthRecommendationResponse> recommendations;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
