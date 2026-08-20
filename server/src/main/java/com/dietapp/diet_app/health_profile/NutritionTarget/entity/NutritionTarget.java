package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "nutrition_targets",
        indexes = {
                @Index(
                        name = "idx_nutrition_target_health_profile",
                        columnList = "health_profile_id",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionTarget extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    @Column(nullable = false)
    private Double bmr;

    @Column(nullable = false)
    private Double tdee;

    @Column(nullable = false)
    private BigDecimal calorieTarget;

    @Column(nullable = false)
    private BigDecimal proteinGrams;

    @Column(nullable = false)
    private BigDecimal fatGrams;

    @Column(nullable = false)
    private BigDecimal carbohydrateGrams;

    @Column(nullable = false)
    private BigDecimal fiberGrams;

    @Column(nullable = false)
    private BigDecimal waterLiters;

    @Column(nullable = false)
    private Integer mealsPerDay;
}
