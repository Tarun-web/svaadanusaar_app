package com.dietapp.diet_app.health_profile.dto.request;


import com.dietapp.diet_app.health_profile.entity.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHealthProfileRequest {

    @NotNull
    @Valid
    private BasicProfile basicProfile;

    @NotNull
    @Valid
    private GoalProfile goalProfile;

    @NotNull
    @Valid
    private LifestyleProfile lifestyleProfile;

    @NotNull
    @Valid
    private DietPreferenceProfile dietPreferenceProfile;

    @NotNull
    @Valid
    private CookingProfile cookingProfile;

    @NotNull
    @Valid
    private MedicalProfile medicalProfile;

    @NotNull
    @Valid
    private SupplementProfile supplementProfile;

    @NotNull
    @Valid
    private BehaviourProfile behaviourProfile;
}
