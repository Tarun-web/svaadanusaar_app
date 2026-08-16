package com.dietapp.diet_app.health_profile.service.FitnessGoalProfile;

import com.dietapp.diet_app.health_profile.dto.request.FitnessGoalRequest;
import com.dietapp.diet_app.health_profile.dto.response.FitnessGoalResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface FitnessGoalService {

    /**
     * Creates or updates the user's fitness goal.
     */
    FitnessGoalResponse saveOrUpdate(FitnessGoalRequest request);

    /**
     * Returns fitness goal by Health Profile Id.
     */
    Optional<FitnessGoalResponse> getMyProfile(
    );

}