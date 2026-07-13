package com.dietapp.diet_app.subscription.controller;

import com.dietapp.diet_app.subscription.dto.request.StartSubscriptionRequest;
import com.dietapp.diet_app.subscription.dto.response.SubscriptionStatusResponse;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import com.dietapp.diet_app.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription")
@AllArgsConstructor
public class SubscriptionController {

    @Autowired
    private final SubscriptionService subscriptionService;

    // Start a new subscription
    @PostMapping("/start")
    public SubscriptionStatusResponse startSubscription(@RequestBody @Valid StartSubscriptionRequest req,
                                                        Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return subscriptionService.startSubscription(userId, req.getPlanId());
    }

    // cancel subscription
    @PostMapping("/cancel")
    public SubscriptionStatusResponse cancelSubscription(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return subscriptionService.cancelSubscription(userId);
    }

    // Get current subscription status
    @GetMapping("/status")
    public SubscriptionStatusResponse getSubscriptionStatus(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return subscriptionService.getSubscriptionStatus(userId);
    }

    // Get subscription history for a user
    @GetMapping("/history")
    public List<UserSubscription> getSubscriptionHistory(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return subscriptionService.getSubscriptionHistory(userId);
    }
}
