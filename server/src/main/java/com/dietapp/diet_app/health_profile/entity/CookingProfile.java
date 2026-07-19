package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.BudgetCategory;
import com.dietapp.diet_app.health_profile.enums.CookingEquipment;
import com.dietapp.diet_app.health_profile.enums.CookingSkill;
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
public class CookingProfile {

    @Enumerated(EnumType.STRING)
    private CookingSkill cookingSkill;

    private Integer cookingTimeMinutesPerDay;

    @Column(columnDefinition = "jsonb")
    private List<CookingEquipment> equipments;  // stored as JSONB (enum names)

    private Boolean mealPrep;

    @Enumerated(EnumType.STRING)
    private BudgetCategory budget;
//    ECONOMY
//    ₹2000–3500/month
//
//            STANDARD
//    ₹3500–7000
//
//    PREMIUM
//    7000+
}
