package com.dietapp.diet_app.health_profile.NutritionTarget.repository;

import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.enums.ProteinAdaptationStage;
import com.dietapp.diet_app.health_profile.NutritionTarget.scheduler.EligibleProteinProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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

    /**
     * One keyset-paginated batch of profiles the scheduled process may still
     * count a day for: stage in {@code stages} and not yet evaluated on or
     * after {@code today}.
     *
     * <p>Each row carries the user's latest reported intake, read as a scalar
     * from {@code health_profiles} in the same statement. The
     * {@code HealthProfile} entity itself is never loaded (it would pull in its
     * eager inverse one-to-one associations), so a whole batch costs exactly
     * one query. Keyset paging ({@code id > afterId}) stays correct while rows
     * leave the result set as they are processed.</p>
     */
    @Query("""
            SELECT new com.dietapp.diet_app.health_profile.NutritionTarget.scheduler.EligibleProteinProfile(
                       p, p.healthProfile.id, h.currentNutritionProfile.currentProteinGrams)
            FROM ProteinAdaptationProfile p
            JOIN p.healthProfile h
            WHERE p.adaptationStage IN :stages
              AND (p.lastEvaluatedOn IS NULL OR p.lastEvaluatedOn < :today)
              AND p.id > :afterId
            ORDER BY p.id
            """)
    List<EligibleProteinProfile> findEligibleForScheduledEvaluation(
            @Param("stages") Collection<ProteinAdaptationStage> stages,
            @Param("today") LocalDate today,
            @Param("afterId") UUID afterId,
            Pageable pageable
    );
}
