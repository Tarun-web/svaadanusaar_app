package com.dietapp.diet_app.health_profile.repository;

import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.enums.MedicalCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalProfileRepository
        extends JpaRepository<MedicalProfile, UUID> {

    Optional<MedicalProfile> findByHealthProfileId(UUID healthProfileId);
    boolean existsByHealthProfileId(UUID healthProfileId);

    /**
     * From the given health profiles, returns the IDs of those that have at
     * least one of the given medical conditions.
     *
     * <p>Single query for the whole batch — avoids loading each profile's
     * lazy {@code medicalConditions} collection one by one.</p>
     */
    @Query("""
       SELECT DISTINCT mp.healthProfile.id
       FROM MedicalProfile mp
       JOIN mp.medicalConditions c
       WHERE mp.healthProfile.id IN :healthProfileIds
       AND c IN :conditions
       """)
    List<UUID> findHealthProfileIdsWithAnyCondition(
            @Param("healthProfileIds") Collection<UUID> healthProfileIds,
            @Param("conditions") Collection<MedicalCondition> conditions
    );

}