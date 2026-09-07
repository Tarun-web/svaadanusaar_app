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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
    @Transactional
    public UserSubscription startSubscription(
            UUID userId,
            String planId,
            boolean autoRenew,
            String razorpaySubscriptionId
    ) {
        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid plan"));

        userSubscriptionRepository
                .findFirstByUserIdAndStatus(userId, "ACTIVE")
                .ifPresent(subscription -> {
                    subscription.setStatus("EXPIRED");
                    subscription.setUpdatedAt(Instant.now());
                    userSubscriptionRepository.save(subscription);
                });

        Instant now = Instant.now();

        Instant endsAt = now
                .atZone(ZoneId.systemDefault())
                .plusMonths(plan.getMonths())
                .toInstant();

        UserSubscription newSubscription =
                new UserSubscription();

        newSubscription.setUserId(userId);
        newSubscription.setPlanId(planId);
        newSubscription.setStartsAt(now);
        newSubscription.setEndsAt(endsAt);
        newSubscription.setStatus("ACTIVE");
        newSubscription.setAutoRenew(autoRenew);
        newSubscription.setRazorpaySubscriptionId(
                razorpaySubscriptionId
        );
        newSubscription.setCreatedAt(now);
        newSubscription.setUpdatedAt(now);

        return userSubscriptionRepository.save(newSubscription);
    }

    // Cancel subscription
//    public SubscriptionStatusResponse cancelSubscription(UUID userId){
//        UserSubscription sub = userSubscriptionRepository.findFirstByUserIdAndStatus(userId, "ACTIVE")
//                .orElseThrow(() -> new RuntimeException("No active subscription found"));
//
//        Instant now = Instant.now();
//
//        sub.setStatus("CANCELLED");
//        sub.setUpdatedAt(now);
//        userSubscriptionRepository.save(sub);
//
//        return new SubscriptionStatusResponse(
//                sub.getPlanId(),
//                sub.getStartsAt(),
//                now,
//                "CANCELLED"
//        );
//    }

    // Get current subscription status for a user
    public SubscriptionStatusResponse getSubscriptionStatus(UUID userId) {
        // First, check if subscription has expired and auto-update if needed
        Optional<UserSubscription> sub = userSubscriptionRepository.findFirstByUserIdAndStatus(userId, "ACTIVE");

        if (sub.isPresent()) {
            UserSubscription subscription = sub.get();
            LocalDateTime endsAtLocal = subscription.getEndsAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // If subscription has ended, mark as EXPIRED
            if (LocalDateTime.now().isAfter(endsAtLocal)) {
                subscription.setStatus("EXPIRED");
                subscription.setUpdatedAt(Instant.now());
                userSubscriptionRepository.save(subscription);

                return new SubscriptionStatusResponse(
                        subscription.getPlanId(),
                        subscription.getStartsAt(),
                        subscription.getEndsAt(),
                        "EXPIRED"
                );
            }

            return new SubscriptionStatusResponse(
                    subscription.getPlanId(),
                    subscription.getStartsAt(),
                    subscription.getEndsAt(),
                    "ACTIVE"
            );
        }

        // No active subscription
        return new SubscriptionStatusResponse(null, null, null, "NO_SUBSCRIPTION");
    }

    // Get subscription history for a user (all subscriptions, newest first)
    public List<UserSubscription> getSubscriptionHistory(UUID userId) {
        return userSubscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    // Background job: mark all expired subscriptions as EXPIRED (scheduled to run nightly)
    @Transactional
    public void markExpiredSubscriptions() {
        List<UserSubscription> expiredSubs = userSubscriptionRepository
                .findExpiredSubscriptions(Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());

        for (UserSubscription sub : expiredSubs) {
            sub.setStatus("EXPIRED");
            sub.setUpdatedAt(Instant.now());
        }

        if (!expiredSubs.isEmpty()) {
            userSubscriptionRepository.saveAll(expiredSubs);
            System.out.println("Marked " + expiredSubs.size() + " subscriptions as EXPIRED");
        }
    }


}
