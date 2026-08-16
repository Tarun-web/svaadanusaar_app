package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.Cuisine;
import com.dietapp.diet_app.health_profile.enums.DietType;
import com.dietapp.diet_app.health_profile.enums.ReligiousRestriction;
import com.dietapp.diet_app.health_profile.enums.TastePreference;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.util.List;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietPreferenceProfile {

    @Enumerated(EnumType.STRING)
    private DietType dietType;

    @Column(columnDefinition = "jsonb")
    private List<Cuisine> preferredCuisines;  // stored as JSONB (enum names)

    @Column(columnDefinition = "jsonb")
    private List<TastePreference> tastePreferences;  // stored as JSONB (enum names)

    @Column(columnDefinition = "jsonb")
    private List<String> favouriteFoods;  // stored as JSONB

    @Column(columnDefinition = "jsonb")
    private List<String> dislikedFoods;  // stored as JSONB

    @Column(columnDefinition = "jsonb")
    private List<ReligiousRestriction> religiousRestrictions;  // stored as JSONB (enum names)

    private Integer mealsPerDay;

}
