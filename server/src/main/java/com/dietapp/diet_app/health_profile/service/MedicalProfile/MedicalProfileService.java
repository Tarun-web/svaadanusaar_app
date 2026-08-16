package com.dietapp.diet_app.health_profile.service.MedicalProfile;
import com.dietapp.diet_app.health_profile.dto.request.MedicalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.MedicalProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface MedicalProfileService {

    MedicalProfileResponse saveOrUpdate(MedicalProfileRequest request);

    Optional<MedicalProfileResponse> getMyProfile(
    );

}
