package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.BudgetCategory;
import com.dietapp.diet_app.health_profile.enums.CookingEquipment;
import com.dietapp.diet_app.health_profile.enums.CookingSkill;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "cooking_profiles",
        indexes = {
                @Index(
                        name = "idx_cooking_profile_health_profile",
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
public class CookingProfile extends BaseEntity {

    /**
     * One Health Profile owns exactly one Cooking Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * User's cooking skill.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CookingSkill cookingSkill = CookingSkill.BEGINNER;

    /**
     * Maximum cooking time user prefers.
     *
     * Minutes.
     */
    @Builder.Default
    @Min(5)
    @Max(180)
    @Column(nullable = false)
    private Integer cookingTimeMinutes = 30;

    /**
     * Kitchen appliances available.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "cooking_profile_equipment",
            joinColumns = @JoinColumn(name = "cooking_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment")
    private Set<CookingEquipment> equipments = new HashSet<>();

    /**
     * Whether user is willing to do meal prep.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean mealPrep = false;

    /**
     * Monthly food budget category.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetCategory budget = BudgetCategory.MEDIUM;

    // ============================================================
    // TODO (Future Enhancements)
    // ============================================================

    /*
     * Future additions:
     *
     * CanCookDaily
     *
     * PreferredCookingDays
     *
     * PreferredCuisineToCook
     *
     * RefrigeratorCapacity
     *
     * FreezerCapacity
     *
     * KitchenType
     *
     * GasStoveAvailable
     *
     * InductionAvailable
     *
     * AirFryerAvailable
     */
}