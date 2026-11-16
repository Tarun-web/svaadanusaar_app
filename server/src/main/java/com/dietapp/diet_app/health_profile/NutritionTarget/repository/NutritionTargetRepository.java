package com.dietapp.diet_app.health_profile.NutritionTarget.repository;

import com.dietapp.diet_app.health_profile.NutritionTarget.entity.NutritionTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Currently unused: targets are calculated on demand and not persisted. See
 * {@link NutritionTarget} for the decision and the schema implications.
 */
public interface NutritionTargetRepository
        extends JpaRepository<NutritionTarget, UUID> {

    Optional<NutritionTarget> findByHealthProfileId(UUID healthProfileId);
}
