package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPreferenceRequest {

    /**
     * Parent Health Profile.
     */
    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    /**
     * User's diet type.
     */
    @NotNull(message = "Diet type is required.")
    private DietType dietType;

    /**
     * Preferred meals per day.
     */
    @NotNull(message = "Meals per day is required.")
    @Min(value = 2, message = "Meals per day must be at least 2.")
    @Max(value = 8, message = "Meals per day cannot exceed 8.")
    private Integer mealsPerDay;

    /**
     * Preferred meal size.
     */
    @NotNull(message = "Preferred meal size is required.")
    private MealSize preferredMealSize;

    /**
     * Spice preference.
     */
    @NotNull(message = "Spice preference is required.")
    private SpicePreference spicePreference;

    /**
     * Dairy preference.
     */
    @NotNull(message = "Dairy preference is required.")
    private DairyPreference dairyPreference;

    /**
     * Whether user consumes eggs.
     */
    @NotNull(message = "Egg preference is required.")
    private Boolean consumesEggs;

    /**
     * Food variety preference.
     */
    @NotNull(message = "Food variety preference is required.")
    private FoodVarietyPreference foodVarietyPreference;

    /**
     * Preferred cuisines.
     */
    @Builder.Default
    @NotEmpty(message = "Select at least one cuisine.")
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

    // ============================================================
    // TODO (Food Module)
    // ============================================================

    /*
     * Replace with:
     *
     * private Set<UUID> favouriteFoodIds = new HashSet<>();
     *
     * private Set<UUID> dislikedFoodIds = new HashSet<>();
     *
     */
}