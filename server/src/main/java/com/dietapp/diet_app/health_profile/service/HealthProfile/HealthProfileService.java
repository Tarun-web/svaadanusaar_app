package com.dietapp.diet_app.health_profile.service.HealthProfile;

import com.dietapp.diet_app.health_profile.dto.request.HealthProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileDetailsResponse;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileResponse;

import java.util.UUID;

public interface HealthProfileService {

    /**
     * Creates a Health Profile for a user.
     */
    HealthProfileResponse createHealthProfile( );

    /**
     * Returns complete Health Profile.
     */
    HealthProfileDetailsResponse getHealthProfile(
            UUID healthProfileId
    );

    /**
     * Returns complete Health Profile by User Id.
     */
    HealthProfileDetailsResponse getMyHealthProfile(    );

    /**
     * Deletes a Health Profile.
     */
    void deleteMyHealthProfile(
    );

    /**
     * Refreshes profile completion percentage.
     */
    void refreshProfileCompletion(
            UUID healthProfileId
    );

    HealthProfileResponse getMyHealthProfileSummary(
    );

}