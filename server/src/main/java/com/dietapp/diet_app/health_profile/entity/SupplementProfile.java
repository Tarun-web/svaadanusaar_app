package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.SupplementType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "supplement_profiles",
        indexes = {
                @Index(
                        name = "idx_supplement_profile_health_profile",
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
public class SupplementProfile extends BaseEntity {

    /**
     * One Health Profile owns exactly one Supplement Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * Whether the user is willing to consume supplements.
     *
     * If false, the recommendation engine should avoid suggesting
     * Whey Protein, Creatine, Fish Oil, etc.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean openToSupplements = false;

    /**
     * Supplements currently consumed.
     *
     * Examples:
     * WHEY_PROTEIN
     * CREATINE
     * FISH_OIL
     * MULTIVITAMIN
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "supplement_profile_current_supplements",
            joinColumns = @JoinColumn(name = "supplement_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "supplement")
    private Set<SupplementType> currentSupplements = new HashSet<>();

    // ============================================================
    // TODO (Future Enhancements)
    // ============================================================

    /*
     * Replace with SupplementUsage entity.
     *
     * SupplementUsage
     * -------------------
     * Supplement
     * Dosage
     * Frequency
     * Timing
     * StartDate
     */
}