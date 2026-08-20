package com.dietapp.diet_app.health_profile.NutritionTarget.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalculationMethod;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionTarget extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /*
     * Weight used for this calculation.
     *
     * Important:
     * This is a snapshot of the weight at calculation time.
     * It is NOT the user's permanent/current weight.
     */
    @Column(
            name = "weight_used_kg",
            precision = 6,
            scale = 2,
            nullable = false
    )
    private BigDecimal weightUsedKg;

    /*
     * Basal Metabolic Rate.
     */
    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal bmr;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal tdee;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal targetCalories;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal proteinGrams;


    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal fatGrams;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal carbohydrateGrams;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal fiberGrams;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal waterMl;

    @Column(
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal calorieAdjustment;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false)
    private CalculationMethod calculationMethod;
}