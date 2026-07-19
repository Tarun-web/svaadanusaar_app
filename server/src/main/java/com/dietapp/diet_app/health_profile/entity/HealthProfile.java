package com.dietapp.diet_app.health_profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    @Embedded
    private BasicProfile basicProfile;

    @Embedded
    private GoalProfile goalProfile;

    @Embedded
    private LifestyleProfile lifestyleProfile;

    @Embedded
    private WorkoutProfile workoutProfile;

    @Embedded
    private DietPreferenceProfile dietPreferenceProfile;

    @Embedded
    private CookingProfile cookingProfile;

    @Embedded
    private MedicalProfile medicalProfile;

    @Embedded
    private SupplementProfile supplementProfile;

    @Embedded
    private BehaviourProfile behaviourProfile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
