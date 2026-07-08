package com.dietapp.diet_app.diet.repository;

import com.dietapp.diet_app.diet.entity.DietPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DietPlanRepository
        extends JpaRepository<DietPlan, UUID> {

    // Find the current valid diet plan for a user
    @Query("""
            SELECT d
            FROM DietPlan d
            WHERE d.userId = :userId
            AND d.validFrom <= :now
            AND (d.validTill IS NULL OR d.validTill >= :now)
            ORDER BY d.version DESC
            """)
    Optional<DietPlan> findCurrentDiet(UUID userId, Instant now);

    // Find the latest diet plan by version for a user
    Optional<DietPlan> findTopByUserIdOrderByVersionDesc(UUID userId);

    // find active diet plans
    @Lock(LockModeType.PESSIMISTIC_WRITE) // lock the active diet plan for update (used during regeneration)
    @Query("""
            SELECT d FROM DietPlan d
            WHERE d.userId = :userId
            AND d.status = 'ACTIVE'
            ORDER BY d.version DESC
            """)
    Optional<DietPlan> findActiveDietPlan(UUID userId);

}

