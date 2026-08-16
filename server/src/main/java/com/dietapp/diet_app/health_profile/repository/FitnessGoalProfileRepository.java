package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FitnessGoalProfileRepository
        extends JpaRepository<FitnessGoalProfile, UUID> {

    Optional<FitnessGoalProfile> findByHealthProfileId(UUID healthProfileId);

    boolean existsByHealthProfileId(UUID healthProfileId);

}
