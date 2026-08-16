package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.LifestylePreferenceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LifestylePreferenceProfileRepository
        extends JpaRepository<LifestylePreferenceProfile, UUID> {

    Optional<LifestylePreferenceProfile> findByHealthProfileId(UUID healthProfileId);
    boolean existsByHealthProfileId(UUID healthProfileId);

}