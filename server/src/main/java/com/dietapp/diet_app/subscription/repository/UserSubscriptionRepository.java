package com.dietapp.diet_app.subscription.repository;

import com.dietapp.diet_app.subscription.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    Optional<UserSubscription> findFirstByUserIdAndStatus(UUID userId, String status);

    // Find all subscriptions for a user
    List<UserSubscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    // Find subscriptions that have ended but not yet marked EXPIRED
    @Query("SELECT s FROM UserSubscription s WHERE s.endsAt < :now AND s.status != 'EXPIRED' AND s.status != 'CANCELLED'")
    List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    Optional<UserSubscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
