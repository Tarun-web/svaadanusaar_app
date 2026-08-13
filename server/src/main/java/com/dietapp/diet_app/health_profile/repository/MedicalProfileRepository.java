package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalProfileRepository
        extends JpaRepository<MedicalProfile, UUID> {

    Optional<MedicalProfile> findByHealthProfileId(UUID healthProfileId);
    boolean existsByHealthProfileId(UUID healthProfileId);

}