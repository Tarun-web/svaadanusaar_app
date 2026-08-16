package com.dietapp.diet_app.health_profile.service.ProfileCompletion;

import java.util.UUID;

public interface ProfileCompletionService {
    public Integer calculateProfileCompletion(UUID healthProfileId);
    void refreshProfileCompletion(
            UUID healthProfileId
    );
}
