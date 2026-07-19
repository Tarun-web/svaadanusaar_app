package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.FoodAllergy;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.List;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalProfile {

    @Column(columnDefinition = "jsonb")
    private List<MedicalCondition> medicalConditions;  // stored as JSONB (enum names)

    @Column(columnDefinition = "jsonb")
    private List<FoodAllergy> allergies;  // stored as JSONB (enum names)

    @Column(columnDefinition = "jsonb")
    private List<String> medications;  // stored as JSONB

    private Boolean kidneyDisease;

    private Boolean liverDisease;

    private Boolean pregnant;

    private Boolean breastfeeding;

}