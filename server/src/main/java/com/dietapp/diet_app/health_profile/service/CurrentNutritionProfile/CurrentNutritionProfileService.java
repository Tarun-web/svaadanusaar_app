package com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;

public interface CurrentNutritionProfileService {

    /**
     * Updates the authenticated user's self-reported protein intake.
     */
    CurrentNutritionProfileResponse saveOrUpdate(CurrentNutritionProfileRequest request);

    /**
     * Returns the authenticated user's self-reported protein intake.
     */
    CurrentNutritionProfileResponse getMyProfile();
}
