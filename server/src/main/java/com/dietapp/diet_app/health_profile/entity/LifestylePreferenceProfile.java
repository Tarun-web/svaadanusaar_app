package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "lifestyle_preference_profiles",
        indexes = {
                @Index(
                        name = "idx_lifestyle_preference_health_profile",
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
public class LifestylePreferenceProfile extends BaseEntity {

    /**
     * One Health Profile owns exactly one Lifestyle Preference Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * Whether the user lives in a hostel.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean hosteller = false;

    /**
     * Whether office/campus lunch is available.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean officeLunchAvailable = false;

    /**
     * Whether user frequently travels.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean travelsFrequently = false;

    /**
     * Whether food delivery apps
     * are available to the user.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean foodDeliveryAvailable = true;

    // ============================================================
    // TODO (Future Enhancements)
    // ============================================================

    /*
     * Future additions:
     *
     * OfficeWorkingDays
     *
     * WorkMode
     * (WFH / Hybrid / Office)
     *
     * GroceryShoppingFrequency
     *
     * PreferredFoodDeliveryApp
     *
     * EatsOutsidePerWeek
     *
     * TravelFrequency
     *
     * SeasonalMigration
     */

}