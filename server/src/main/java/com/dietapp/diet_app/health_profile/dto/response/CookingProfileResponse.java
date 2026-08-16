package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.BudgetCategory;
import com.dietapp.diet_app.health_profile.enums.CookingEquipment;
import com.dietapp.diet_app.health_profile.enums.CookingSkill;
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
public class CookingProfileResponse {

    private UUID id;

    private UUID healthProfileId;

    private CookingSkill cookingSkill;

    private Integer cookingTimeMinutes;

    private Set<CookingEquipment> equipments = new HashSet<>();

    private Boolean mealPrep;

    private BudgetCategory budget;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}