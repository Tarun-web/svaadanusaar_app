package com.dietapp.diet_app.health_profile.service.LifestylePreferenceProfile;

import com.dietapp.diet_app.health_profile.dto.request.LifestylePreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.LifestylePreferenceResponse;

import java.util.Optional;
import java.util.UUID;

public interface LifestylePreferenceService {

    LifestylePreferenceResponse saveOrUpdate(
            LifestylePreferenceRequest request
    );

    Optional<LifestylePreferenceResponse> getMyProfile(
    );

}