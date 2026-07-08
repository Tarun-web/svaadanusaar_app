package com.dietapp.diet_app.subscription.service;

import com.dietapp.diet_app.subscription.dto.response.SubscriptionStatusResponse;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import com.dietapp.diet_app.subscription.repository.UserSubscriptionRepository;
import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Transactional
@Service
@AllArgsConstructor
public class SubscriptionService {

    @Autowired
    private final UserSubscriptionRepository userSubscriptionRepository;
    @Autowired
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    // Start a new subscription
    public SubscriptionStatusResponse startSubscription(UUID userId, String planId) {
        // Implementation for starting a subscription
        // Validate plan
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Invalid plan"));

        // end existing subscription if any
        userSubscriptionRepository.findFirstByUserIdAndStatus(userId, "ACTIVE")
                .ifPresent(subscription -> {
                    subscription.setStatus("EXPIRED");
                    subscription.setUpdatedAt(Instant.now());
                    userSubscriptionRepository.save(subscription);
                });

        Instant now = Instant.now();
//        Instant endsAt = now.plus(plan.getMonths(), ChronoUnit.MONTHS);
        Instant endsAt = Instant.from(LocalDateTime.now()
                .plusMonths(plan.getMonths())); // For testing, 30 days instead of months

        // Create new subscription
        UserSubscription newSubscription = new UserSubscription();
        newSubscription.setUserId(userId);
        newSubscription.setPlanId(planId);
        newSubscription.setStartsAt(now);
        newSubscription.setEndsAt(endsAt);
        newSubscription.setStatus("ACTIVE");
        newSubscription.setAutoRenew(false);
        newSubscription.setCreatedAt(now);
        newSubscription.setUpdatedAt(now);

        userSubscriptionRepository.save(newSubscription);
        return new SubscriptionStatusResponse(
                newSubscription.getPlanId(),
                newSubscription.getStartsAt(),
                newSubscription.getEndsAt(),
                newSubscription.getStatus()
        );
    }

    // Cancel subscription
    public SubscriptionStatusResponse cancelSubscription(UUID userId){
        UserSubscription sub = userSubscriptionRepository.findFirstByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        Instant now = Instant.now();

        sub.setStatus("CANCELLED");
        sub.setUpdatedAt(now);
        userSubscriptionRepository.save(sub);

        return new SubscriptionStatusResponse(
                sub.getPlanId(),
                sub.getStartsAt(),
                now,
                "CANCELLED"
        );
    }


}
