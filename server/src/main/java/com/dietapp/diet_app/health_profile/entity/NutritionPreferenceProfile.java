package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.DairyPreference;
import com.dietapp.diet_app.health_profile.enums.DietType;
import com.dietapp.diet_app.health_profile.enums.FoodVarietyPreference;
import com.dietapp.diet_app.health_profile.enums.MealSize;
import com.dietapp.diet_app.health_profile.enums.SpicePreference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Set;

import com.dietapp.diet_app.health_profile.enums.Cuisine;
import com.dietapp.diet_app.health_profile.enums.ProteinSource;
import com.dietapp.diet_app.health_profile.enums.ReligiousRestriction;
import com.dietapp.diet_app.health_profile.enums.TastePreference;

@Entity
@Table(
        name = "nutrition_preference_profiles",
        indexes = {
                @Index(
                        name = "idx_nutrition_preference_health_profile",
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
public class NutritionPreferenceProfile extends BaseEntity {
    /**
     * One Health Profile owns exactly one Nutrition Preference Profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    /**
     * Primary dietary preference.
     *
     * Examples:
     * - VEG
     * - VEGAN
     * - EGGETARIAN
     * - NON_VEG
     * - JAIN
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private DietType dietType;

    /**
     * Number of meals user prefers daily.
     *
     * Example:
     * 3
     * 4
     * 5
     * 6
     */
    @Min(2)
    @Max(8)
    @Builder.Default
    @Column(nullable = false)
    private Integer mealsPerDay = 3;

    /**
     * User's preferred meal size.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private MealSize preferredMealSize = MealSize.MEDIUM;

    /**
     * User's spice tolerance.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private SpicePreference spicePreference = SpicePreference.MEDIUM;

    /**
     * Dairy consumption preference.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DairyPreference dairyPreference = DairyPreference.YES;

    /**
     * Whether the user consumes eggs.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean consumesEggs = false;

    /**
     * Controls meal rotation.
     *
     * LOW
     * -> Same meals frequently.
     *
     * HIGH
     * -> AI should maximize variety.
     */
    @Builder.Default
    @Column(nullable = false)
    private FoodVarietyPreference foodVarietyPreference =
            FoodVarietyPreference.MEDIUM;

// ============================================================
// Preferred Cuisines
// ============================================================

    /**
     * User's preferred cuisines.
     *
     * Used by:
     * - Diet Recommendation Engine
     * - Recipe Recommendation
     * - Food Ranking
     *
     * Example:
     * NORTH_INDIAN
     * SOUTH_INDIAN
     * ITALIAN
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "nutrition_preferred_cuisines",
            joinColumns = @JoinColumn(name = "nutrition_preference_profile_id")
    )
    @Column(name = "cuisine", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Cuisine> preferredCuisines = new HashSet<>();

// ============================================================
// Taste Preferences
// ============================================================

    /**
     * User's preferred taste profiles.
     *
     * Example:
     * SPICY
     * SWEET
     * TANGY
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "nutrition_taste_preferences",
            joinColumns = @JoinColumn(name = "nutrition_preference_profile_id")
    )
    @Column(name = "taste_preference", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<TastePreference> tastePreferences = new HashSet<>();

// ============================================================
// Preferred Protein Sources
// ============================================================

    /**
     * Used by AI while recommending foods.
     *
     * Example:
     * CHICKEN
     * EGGS
     * PANEER
     * SOY
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "nutrition_preferred_protein_sources",
            joinColumns = @JoinColumn(name = "nutrition_preference_profile_id")
    )
    @Column(name = "protein_source", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ProteinSource> preferredProteinSources = new HashSet<>();

// ============================================================
// Religious Restrictions
// ============================================================

    /**
     * Example:
     *
     * JAIN
     * HALAL
     * SATVIK
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "nutrition_religious_restrictions",
            joinColumns = @JoinColumn(name = "nutrition_preference_profile_id")
    )
    @Column(name = "religious_restriction", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ReligiousRestriction> religiousRestrictions = new HashSet<>();

// ============================================================
// TODO (Food Module)
// ============================================================

    /*
     * TODO:
     *
     * Replace these placeholders once Food module is introduced.
     *
     * @ManyToMany(fetch = FetchType.LAZY)
     * @JoinTable(
     *      name = "nutrition_favourite_foods",
     *      joinColumns = @JoinColumn(name = "nutrition_preference_profile_id"),
     *      inverseJoinColumns = @JoinColumn(name = "food_id")
     * )
     * private Set<Food> favouriteFoods = new HashSet<>();
     *
     *
     * @ManyToMany(fetch = FetchType.LAZY)
     * @JoinTable(
     *      name = "nutrition_disliked_foods",
     *      joinColumns = @JoinColumn(name = "nutrition_preference_profile_id"),
     *      inverseJoinColumns = @JoinColumn(name = "food_id")
     * )
     * private Set<Food> dislikedFoods = new HashSet<>();
     *
     */

// ============================================================
// Future Enhancements
// ============================================================

    /*
     * TODO:
     *
     * Add once recommendation engine becomes smarter.
     *
     * private SpiceTolerance spiceTolerance;
     *
     * private CuisineAffinity cuisineAffinity;
     *
     * private SeasonalPreference seasonalPreference;
     *
     * private FestivalFoodPreference festivalPreference;
     *
     * private Boolean openToFoodExperimentation;
     *
     * private Integer weeklyEatingOutFrequency;
     *
     */
}
