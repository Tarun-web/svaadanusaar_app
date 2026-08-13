package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.FoodAllergy;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.PregnancyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "medical_profiles",
        indexes = {
                @Index(
                        name = "idx_medical_profile_health_profile",
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
public class MedicalProfile extends BaseEntity {

    /**
     * One Health Profile owns exactly one Medical Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * Medical conditions diagnosed in the user.
     *
     * Examples:
     * DIABETES
     * PCOS
     * HYPOTHYROIDISM
     * OBESITY
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "medical_profile_conditions",
            joinColumns = @JoinColumn(name = "medical_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "medical_condition")
    private Set<MedicalCondition> medicalConditions = new HashSet<>();

    /**
     * Food allergies.
     *
     * Examples:
     * DAIRY
     * GLUTEN
     * PEANUT
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "medical_profile_food_allergies",
            joinColumns = @JoinColumn(name = "medical_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "food_allergy")
    private Set<FoodAllergy> foodAllergies = new HashSet<>();

    /**
     * Current medications.
     *
     * TODO:
     * Replace with Medication entity in future.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "medical_profile_medications",
            joinColumns = @JoinColumn(name = "medical_profile_id")
    )
    @Column(name = "medication")
    private Set<String> medications = new HashSet<>();

    /**
     * Pregnancy / breastfeeding status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PregnancyStatus pregnancyStatus =
            PregnancyStatus.NOT_APPLICABLE;

    // ============================================================
    // TODO (Future Medical Module)
    // ============================================================

    /*
     * Blood Reports
     *
     * HbA1c
     * Fasting Blood Sugar
     * PP Blood Sugar
     * LDL
     * HDL
     * Triglycerides
     * Total Cholesterol
     */

    /*
     * Kidney Function
     *
     * Creatinine
     * eGFR
     * Uric Acid
     */

    /*
     * Liver Function
     *
     * ALT
     * AST
     * Bilirubin
     */

    /*
     * Vitamin Deficiencies
     *
     * Vitamin D
     * Vitamin B12
     * Iron
     * Calcium
     * Magnesium
     */

    /*
     * Hormonal Profile
     *
     * Testosterone
     * Estrogen
     * Cortisol
     * TSH
     * T3
     * T4
     */

}