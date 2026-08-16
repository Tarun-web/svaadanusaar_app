package com.dietapp.diet_app.health_profile.service.CookingProfile;

import com.dietapp.diet_app.health_profile.dto.request.CookingProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CookingProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface CookingProfileService {

    CookingProfileResponse saveOrUpdate(
            CookingProfileRequest request
    );

    Optional<CookingProfileResponse> getMyProfile(
    );

}
