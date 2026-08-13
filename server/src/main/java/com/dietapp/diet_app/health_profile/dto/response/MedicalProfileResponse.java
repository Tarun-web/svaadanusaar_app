package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.FoodAllergy;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import com.dietapp.diet_app.health_profile.enums.PregnancyStatus;
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
public class MedicalProfileResponse {

    private UUID id;

    private UUID healthProfileId;

    private Set<MedicalCondition> medicalConditions = new HashSet<>();

    private Set<FoodAllergy> foodAllergies = new HashSet<>();

    private Set<String> medications = new HashSet<>();

    private PregnancyStatus pregnancyStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}