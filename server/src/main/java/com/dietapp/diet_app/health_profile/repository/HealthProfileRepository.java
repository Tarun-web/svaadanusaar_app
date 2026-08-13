package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, UUID> {

    Optional<HealthProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
//User
// │
//         └── 1 : 1
//         ▼
//HealthProfile
//      │
//              ├── PersonalProfile
//      ├── GoalProfile
//      ├── DietProfile
//      ├── WorkoutProfile
//      ├── MedicalProfile
//      └── PreferenceProfile

