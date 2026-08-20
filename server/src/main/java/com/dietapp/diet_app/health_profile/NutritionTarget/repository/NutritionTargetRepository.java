package com.dietapp.diet_app.health_profile.NutritionTarget.repository;

import com.dietapp.diet_app.health_profile.NutritionTarget.entity.NutritionTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NutritionTargetRepository
        extends JpaRepository<NutritionTarget, UUID> {

    Optional<NutritionTarget> findByHealthProfileId(UUID healthProfileId);
}
