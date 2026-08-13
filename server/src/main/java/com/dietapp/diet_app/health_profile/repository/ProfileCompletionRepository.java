package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileCompletionRepository extends JpaRepository<HealthProfile, UUID> {

    boolean existsByHealthProfileId(
            UUID healthProfileId
    );
}
