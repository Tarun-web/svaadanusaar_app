package com.dietapp.diet_app.progress.repository;

import com.dietapp.diet_app.progress.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeightLogRepository extends JpaRepository<WeightLog, UUID> {

    // Find a weight log by user ID and log date
    Optional<WeightLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate);

    // Find the most recent weight log for a user
    Optional<WeightLog> findTopByUserIdOrderByLogDateDesc(UUID userId);

    // Find all weight logs for a user ordered by date descending
    List<WeightLog> findByUserIdOrderByLogDateDesc(UUID userId);

    // Find last 7 days weight logs for a user
    @Query("""
        SELECT w FROM WeightLog w
        WHERE w.userId = :userId
        ORDER BY w.logDate DESC
        LIMIT 7
        """)
    List<WeightLog> findLast7ByUser(UUID userId);
}

