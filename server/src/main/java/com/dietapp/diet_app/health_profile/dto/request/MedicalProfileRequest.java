package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.FoodAllergy;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.PregnancyStatus;
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
public class MedicalProfileRequest {

    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    @Builder.Default
    private Set<MedicalCondition> medicalConditions = new HashSet<>();

    @Builder.Default
    private Set<FoodAllergy> foodAllergies = new HashSet<>();

    @Builder.Default
    private Set<String> medications = new HashSet<>();

    @Builder.Default
    private PregnancyStatus pregnancyStatus = PregnancyStatus.NOT_APPLICABLE;

}