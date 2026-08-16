package com.dietapp.diet_app.health_profile.service.SupplementProfile;

import com.dietapp.diet_app.health_profile.dto.request.SupplementProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.SupplementProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface SupplementProfileService {

    SupplementProfileResponse saveOrUpdate(
            SupplementProfileRequest request
    );

    Optional<SupplementProfileResponse> getMyProfile(
    );

}
