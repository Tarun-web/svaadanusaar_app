package com.dietapp.diet_app.health_profile.NutritionTarget.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ActivityLevel;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.CalculationMethod;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Snapshot row for a user's calculated nutrition targets.
 *
 * <p><strong>Design decision (Nutrition Engine v1): targets are calculated on
 * demand and are NOT persisted.</strong> {@code GET /api/v1/nutrition-targets}
 * recomputes everything from the current profile on every call; the only
 * persisted nutrition state is {@code ProteinAdaptationProfile}. Nothing in
 * production code writes or reads this entity: {@code NutritionTargetRepository}
 * and {@code NutritionTargetMapper} have no callers.</p>
 *
 * <p>The class is kept (not deleted) pending a product decision on historical
 * targets. Schema implications while it stays: no Flyway migration creates
 * {@code nutrition_targets} (V19-V24 only alter it), several columns here are
 * {@code nullable=false} while the migrations leave them nullable, and the
 * unique {@code health_profile_id} allows only ONE row per user, i.e. a latest
 * snapshot, not a history. A history feature would need a baseline migration,
 * a write path on calculation, a one-to-many model and a read endpoint.</p>
 */

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