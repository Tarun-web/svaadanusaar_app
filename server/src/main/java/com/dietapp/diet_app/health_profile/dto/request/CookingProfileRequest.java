package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.BudgetCategory;
import com.dietapp.diet_app.health_profile.enums.CookingEquipment;
import com.dietapp.diet_app.health_profile.enums.CookingSkill;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CookingProfileRequest {

    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    @Builder.Default
    private CookingSkill cookingSkill = CookingSkill.BEGINNER;

    @Builder.Default
    @Min(5)
    @Max(180)
    private Integer cookingTimeMinutes = 30;

    @Builder.Default
    private Set<CookingEquipment> equipments = new HashSet<>();

    @Builder.Default
    private Boolean mealPrep = false;

    @Builder.Default
    private BudgetCategory budget = BudgetCategory.MEDIUM;

}