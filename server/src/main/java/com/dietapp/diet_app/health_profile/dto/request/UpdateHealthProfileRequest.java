package com.dietapp.diet_app.health_profile.dto.request;


import com.dietapp.diet_app.health_profile.entity.*;

import jakarta.validation.Valid;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHealthProfileRequest {

    @Valid
    private BasicProfile basicProfile;

    @Valid
    private GoalProfile goalProfile;

    @Valid
    private LifestyleProfile lifestyleProfile;

    @Valid
    private DietPreferenceProfile dietPreferenceProfile;

    @Valid
    private CookingProfile cookingProfile;

    @Valid
    private MedicalProfile medicalProfile;

    @Valid
    private SupplementProfile supplementProfile;

    @Valid
    private BehaviourProfile behaviourProfile;
}
