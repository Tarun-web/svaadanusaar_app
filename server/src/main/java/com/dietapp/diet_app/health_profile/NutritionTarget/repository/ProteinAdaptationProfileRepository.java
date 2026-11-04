package com.dietapp.diet_app.health_profile.NutritionTarget.repository;

import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link ProteinAdaptationProfile}.
 *
 * <p>Follows the same pattern as {@link NutritionTargetRepository}.</p>
 */
public interface ProteinAdaptationProfileRepository
        extends JpaRepository<ProteinAdaptationProfile, UUID> {

    /**
     * Finds the adaptation profile for the given health profile entity.
     *
     * <p>Used by {@code ProteinAdaptationService} to load or check for
     * an existing profile before creating or updating.</p>
     */
    Optional<ProteinAdaptationProfile> findByHealthProfile(
            HealthProfile healthProfile
    );

    /**
     * Convenience lookup by health profile ID.
     *
     * <p>Avoids loading the full {@link HealthProfile} entity when only
     * the UUID is available.</p>
     */
    Optional<ProteinAdaptationProfile> findByHealthProfileId(
            UUID healthProfileId
    );
}
