package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.NutritionPreferenceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NutritionPreferenceProfileRepository
        extends JpaRepository<NutritionPreferenceProfile, UUID> {

    /**
     * Fetch nutrition preferences for a health profile.
     */
    Optional<NutritionPreferenceProfile> findByHealthProfileId(UUID healthProfileId);

    /**
     * Check whether nutrition preferences already exist.
     */
    boolean existsByHealthProfileId(UUID healthProfileId);

    /**
     * Delete nutrition preferences when required.
     */
    void deleteByHealthProfileId(UUID healthProfileId);
}
