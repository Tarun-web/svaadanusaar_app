package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityProfileRepository
        extends JpaRepository<ActivityProfile, UUID> {

    List<ActivityProfile> findByWorkoutProfileId(UUID workoutProfileId);
    boolean existsByHealthProfileId(UUID healthProfileId);

}