package com.dietapp.diet_app.health_profile.NutritionTarget.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinToleranceStatus;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Persists the ongoing state of a user's protein adaptation journey.
 *
 * <h3>Key distinction between fields</h3>
 * <pre>
 *   currentProteinGrams  — what the user currently eats
 *   currentTargetGrams   — what Svaadanusaar currently recommends
 *   longTermTargetGrams  — the eventual physiological target
 * </pre>
 *
 * <h3>Application-managed</h3>
 * <p>This entity is created and updated automatically by
 * {@code ProteinAdaptationService}. Users never fill it manually.</p>
 *
 * <h3>Ramp increment / adaptation period</h3>
 * <p>These are business rules defined as constants in
 * {@code ProteinAdaptationService} and are NOT stored here.
 * Storing them as DB state would make them harder to change and
 * would create drift between running instances.</p>
 */
@Entity
@Table(
        name = "protein_adaptation_profiles",
        indexes = {
                @Index(
                        name = "idx_protein_adaptation_health_profile",
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
public class ProteinAdaptationProfile extends BaseEntity {

    /**
     * The parent health profile.
     *
     * <p>Relationship is 1:1 — one user has exactly one
     * protein adaptation profile.</p>
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * The user's current actual daily protein intake in grams.
     *
     * <p>Nullable — {@code null} means intake is unknown.
     * Must NOT be invented; it comes from user input or food-log analysis.</p>
     */
    @Column(
            name = "current_protein_grams",
            precision = 8,
            scale = 2
    )
    private BigDecimal currentProteinGrams;

    /**
     * The long-term physiological protein target calculated by
     * {@code ProteinCalculator}.
     */
    @Column(
            name = "long_term_target_grams",
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal longTermTargetGrams;

    /**
     * What Svaadanusaar currently recommends for this user.
     *
     * <p>This value is written to {@code NutritionTarget.proteinGrams}
     * so the meal plan reflects the user's current adaptation stage,
     * not the eventual target.</p>
     */
    @Column(
            name = "current_target_grams",
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal currentTargetGrams;

    /**
     * The current phase in the adaptation journey.
     *
     * @see ProteinAdaptationStage
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "adaptation_stage",
            nullable = false
    )
    private ProteinAdaptationStage adaptationStage;

    /**
     * Number of days the user has been at the current target.
     *
     * <p>Used by {@code ProteinAdaptationService} to decide whether
     * the minimum adaptation period has elapsed.</p>
     */
    @Builder.Default
    @Column(
            name = "days_at_current_target",
            nullable = false
    )
    private Integer daysAtCurrentTarget = 0;

    /**
     * GI / digestive tolerance at the current protein target.
     *
     * <p>A {@code MILD_ISSUES} or {@code SIGNIFICANT_ISSUES} status
     * blocks progression regardless of elapsed days.</p>
     *
     * @see ProteinToleranceStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "tolerance_status",
            nullable = false
    )
    private ProteinToleranceStatus toleranceStatus;

    /**
     * The date on which the current target was set or last changed.
     *
     * <p>Used to calculate elapsed days and to determine the earliest
     * eligible date for the next increment.</p>
     */
    @Column(
            name = "last_target_change_at"
    )
    private LocalDate lastTargetChangeAt;
}
