package com.dietapp.diet_app.health_profile.service.PersonalProfile;

import com.dietapp.diet_app.health_profile.dto.request.PersonalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.PersonalProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface PersonalProfileService {

    /**
     * Creates a new Personal Profile or updates the existing one.
     */
    PersonalProfileResponse saveOrUpdate(PersonalProfileRequest request);

    /**
     * Returns Personal Profile by Health Profile Id.
     */
    Optional<PersonalProfileResponse> getMyProfile(
    );

}
