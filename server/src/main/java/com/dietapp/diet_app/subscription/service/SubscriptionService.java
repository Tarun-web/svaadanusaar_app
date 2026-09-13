package com.dietapp.diet_app.subscription.service;

import com.dietapp.diet_app.subscription.dto.response.SubscriptionStatusResponse;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import com.dietapp.diet_app.subscription.repository.UserSubscriptionRepository;
import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RazorpayClient razorpayClient;

    // Create a local subscription after successful payment verification
    public UserSubscription createSubscription(
            UUID userId,
            String planId,
            boolean autoRenew,
            String razorpaySubscriptionId
    ) {
        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid plan"));

        // Expire the user's previous active subscription
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

    // Renew an existing Razorpay subscription
    public void renewSubscription(
            String razorpaySubscriptionId,
            Instant currentEnd
    ) {
        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local subscription not found for Razorpay subscription: "
                                                + razorpaySubscriptionId
                                ));

        subscription.setEndsAt(currentEnd);
        subscription.setStatus("ACTIVE");
        subscription.setAutoRenew(true);
        subscription.setUpdatedAt(Instant.now());

        userSubscriptionRepository.save(subscription);
    }

    // disable autorenew
    public void disableAutoRenew(String razorpaySubscriptionId) {
        userSubscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId)
                .ifPresent(subscription -> {
                    subscription.setAutoRenew(false);
                    subscription.setUpdatedAt(Instant.now());
                    userSubscriptionRepository.save(subscription);
                });
    }

    // Get current subscription status
    public SubscriptionStatusResponse getSubscriptionStatus(UUID userId) {

        Optional<UserSubscription> sub =
                userSubscriptionRepository
                        .findFirstByUserIdAndStatus(userId, "ACTIVE");

        if (sub.isEmpty()) {
            return new SubscriptionStatusResponse(
                    null,
                    null,
                    null,
                    "NO_SUBSCRIPTION"
            );
        }

        UserSubscription subscription = sub.get();

        Instant now = Instant.now();

        if (subscription.getEndsAt() != null
                && !now.isBefore(subscription.getEndsAt())) {

            subscription.setStatus("EXPIRED");
            subscription.setUpdatedAt(now);

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

    // Get subscription history
    public List<UserSubscription> getSubscriptionHistory(UUID userId) {
        return userSubscriptionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    // Cancel Razorpay subscription at the end of current billing cycle
    public SubscriptionStatusResponse cancelSubscription(UUID userId)
            throws RazorpayException {

        UserSubscription subscription =
                userSubscriptionRepository
                        .findFirstByUserIdAndStatus(userId, "ACTIVE")
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No active subscription found"
                                ));

        if (!subscription.isAutoRenew()
                || subscription.getRazorpaySubscriptionId() == null) {

            throw new RuntimeException(
                    "Subscription is not set to auto-renew"
            );
        }

        JSONObject options = new JSONObject();
        options.put("cancel_at_cycle_end", 1);

        razorpayClient.subscriptions.cancel(
                subscription.getRazorpaySubscriptionId(),
                options
        );

        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(Instant.now());

        userSubscriptionRepository.save(subscription);

        return new SubscriptionStatusResponse(
                subscription.getPlanId(),
                subscription.getStartsAt(),
                subscription.getEndsAt(),
                subscription.getStatus()
        );
    }

    // Background job to mark expired subscriptions
    public void markExpiredSubscriptions() {

        Instant now = Instant.now();

        List<UserSubscription> expiredSubs =
                userSubscriptionRepository
                        .findExpiredSubscriptions(now);

        for (UserSubscription sub : expiredSubs) {
            sub.setStatus("EXPIRED");
            sub.setUpdatedAt(now);
        }

        if (!expiredSubs.isEmpty()) {
            userSubscriptionRepository.saveAll(expiredSubs);

            System.out.println(
                    "Marked "
                            + expiredSubs.size()
                            + " subscriptions as EXPIRED"
            );
        }
    }
}