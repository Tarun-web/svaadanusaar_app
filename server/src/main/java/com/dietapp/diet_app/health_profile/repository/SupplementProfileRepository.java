package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.SupplementProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplementProfileRepository
        extends JpaRepository<SupplementProfile, UUID> {

    Optional<SupplementProfile> findByHealthProfileId(UUID healthProfileId);
    boolean existsByHealthProfileId(UUID healthProfileId);

}