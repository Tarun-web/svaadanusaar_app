package com.dietapp.diet_app.health_profile.service.ActivityProfile;

import com.dietapp.diet_app.health_profile.dto.request.ActivityRequest;
import com.dietapp.diet_app.health_profile.dto.response.ActivityResponse;

import java.util.List;
import java.util.UUID;

public interface ActivityProfileService {

    ActivityResponse addActivity(
            ActivityRequest request
    );

    ActivityResponse updateActivity(
            UUID activityId,
            ActivityRequest request
    );

    void deleteActivity(
            UUID activityId
    );

    List<ActivityResponse> getMyActivities();

}