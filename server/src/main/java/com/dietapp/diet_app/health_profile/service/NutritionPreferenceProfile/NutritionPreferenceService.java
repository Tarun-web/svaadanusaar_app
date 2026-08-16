package com.dietapp.diet_app.health_profile.service.NutritionPreferenceProfile;

import com.dietapp.diet_app.health_profile.dto.request.NutritionPreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.NutritionPreferenceResponse;

import java.util.Optional;
import java.util.UUID;

public interface NutritionPreferenceService {

    /**
     * Creates or updates Nutrition Preferences.
     */
    NutritionPreferenceResponse saveOrUpdate(
            NutritionPreferenceRequest request
    );

    /**
     * Returns Nutrition Preferences by Health Profile Id.
     */
    Optional<NutritionPreferenceResponse> getMyProfile(
    );

}
