package com.dietapp.diet_app.HealthProfile.repository;

import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, UUID> {

    // find by user id
    Optional<HealthProfile> findByUserId(UUID userId);
}
