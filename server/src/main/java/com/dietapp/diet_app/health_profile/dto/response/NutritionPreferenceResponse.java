package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPreferenceResponse {

    /**
     * Nutrition Preference Profile Id.
     */
    private UUID id;

    /**
     * Parent Health Profile Id.
     */
    private UUID healthProfileId;

    /**
     * User's diet preference.
     */
    private DietType dietType;

    /**
     * Preferred meals per day.
     */
    private Integer mealsPerDay;

    /**
     * Preferred meal size.
     */
    private MealSize preferredMealSize;

    /**
     * Spice preference.
     */
    private SpicePreference spicePreference;

    /**
     * Dairy preference.
     */
    private DairyPreference dairyPreference;

    /**
     * Egg preference.
     */
    private Boolean consumesEggs;

    /**
     * Food variety preference.
     */
    private FoodVarietyPreference foodVarietyPreference;

    /**
     * Preferred cuisines.
     */
    @Builder.Default
    private Set<Cuisine> preferredCuisines = new HashSet<>();

    /**
     * Preferred taste profiles.
     */
    @Builder.Default
    private Set<TastePreference> tastePreferences = new HashSet<>();

    /**
     * Preferred protein sources.
     */
    @Builder.Default
    private Set<ProteinSource> preferredProteinSources = new HashSet<>();

    /**
     * Religious restrictions.
     */
    @Builder.Default
    private Set<ReligiousRestriction> religiousRestrictions = new HashSet<>();

    /**
     * Audit Fields.
     */
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ============================================================
    // TODO (Food Module)
    // ============================================================

    /*
     * Replace with:
     *
     * private Set<FoodResponse> favouriteFoods;
     *
     * private Set<FoodResponse> dislikedFoods;
     *
     */
}