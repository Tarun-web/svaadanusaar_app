package com.dietapp.diet_app.subscription.repository;

import com.dietapp.diet_app.subscription.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    Optional<UserSubscription> findFirstByUserIdAndStatus(UUID userId, String status);
}
