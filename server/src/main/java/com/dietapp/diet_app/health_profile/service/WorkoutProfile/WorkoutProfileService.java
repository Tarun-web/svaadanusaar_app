package com.dietapp.diet_app.health_profile.service.WorkoutProfile;


import com.dietapp.diet_app.health_profile.dto.request.WorkoutProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.WorkoutProfileResponse;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutProfileService {

    WorkoutProfileResponse saveOrUpdate(WorkoutProfileRequest request);

    Optional<WorkoutProfileResponse> getMyProfile(
    );

}
