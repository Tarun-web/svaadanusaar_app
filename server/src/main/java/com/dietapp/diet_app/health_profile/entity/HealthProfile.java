package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "health_profiles",
        indexes = {
                @Index(
                        name = "idx_health_profile_user",
                        columnList = "user_id",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthProfile extends BaseEntity {

    /**
     * One user has exactly one health profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    /**
     * Whether onboarding is completed.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean onboardingCompleted = false;

    /**
     * 0-100
     * Calculated whenever profile changes.
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer profileCompletionPercentage = 0;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private PersonalProfile personalProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private FitnessGoalProfile fitnessGoalProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private NutritionPreferenceProfile nutritionPreferenceProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private WorkoutProfile workoutProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private MedicalProfile medicalProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private CookingProfile cookingProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private LifestylePreferenceProfile lifestylePreferenceProfile;

    @OneToOne(
            mappedBy = "healthProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private SupplementProfile supplementProfile;


}
