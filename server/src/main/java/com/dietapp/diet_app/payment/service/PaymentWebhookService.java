package com.dietapp.diet_app.payment.service;

import com.dietapp.diet_app.payment.entity.Payment;
import com.dietapp.diet_app.payment.entity.PaymentWebhookEvent;
import com.dietapp.diet_app.payment.repository.PaymentRepository;
import com.dietapp.diet_app.payment.repository.PaymentWebhookEventRepository;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import com.dietapp.diet_app.subscription.repository.UserSubscriptionRepository;
import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final PaymentRepository  paymentRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional
    public void processWebhook(String payload, String signatureId, String eventId){

        // verify signature
        verifySignature(payload, signatureId);

        // Idempotency
        if (isAlreadyProcessed(eventId)) {
            return;
        }

        JSONObject webhook = new JSONObject(payload);
        String event = webhook.getString("event");



        switch (event) {

            case "subscription.charged" ->
                    handleSubscriptionCharged(webhook, eventId);

            case "subscription.cancelled" ->
                    handleSubscriptionCancelled(webhook, eventId);

            case "subscription.halted" ->
                    handleSubscriptionHalted(webhook, eventId);

            case "subscription.completed" ->
                    handleSubscriptionCompleted(webhook, eventId);

            default -> {
                // Ignore events we haven't implemented yet
            }
        }

        PaymentWebhookEvent webhookEvent =
                new PaymentWebhookEvent();

        webhookEvent.setId(UUID.randomUUID());
        webhookEvent.setProviderEventId(eventId);
        webhookEvent.setEventType(event);
        webhookEvent.setReceivedAt(Instant.now());

        paymentWebhookEventRepository.save(webhookEvent);

    }

    // signature verification
    private void verifySignature(String payload, String signature){

        try{
            boolean isValid = Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );
        } catch (RazorpayException e) {
            throw new SecurityException("Invalid webhook signature", e);
        }
    }

    // Idempotency
    private boolean isAlreadyProcessed(String eventId) {

        return paymentWebhookEventRepository
                .existsByProviderEventId(eventId);
    }

    // handle subscription.charged event
    private void handleSubscriptionCharged(JSONObject webhook, String eventId) {

        JSONObject payload =
                webhook.getJSONObject("payload");

        JSONObject subscriptionEntity =
                payload
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        // find the subscription by razorpaySubscriptionId
        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local subscription not found: "
                                                + razorpaySubscriptionId
                                ));

        /*
         * Razorpay has successfully charged
         * the next billing cycle.
         */
        UUID userId = subscription.getUserId();

        String planId = subscription.getPlanId();

        // find the plan
        SubscriptionPlan plan =
                subscriptionPlanRepository
                        .findById(planId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Plan not found: " + planId
                                ));

        JSONObject paymentEntity =
                payload.has("payment")
                        ? payload
                        .getJSONObject("payment")
                        .getJSONObject("entity")
                        : null;

        Instant now = Instant.now();

        Payment payment = new Payment();

        payment.setId(UUID.randomUUID());
        payment.setUserId(userId);
        payment.setSubscriptionId(subscription.getId());
        payment.setPlanId(planId);
        payment.setProvider("RAZORPAY");

        payment.setRazorpaySubscriptionId(
                razorpaySubscriptionId
        );

        if (paymentEntity != null) {

            payment.setPaymentId(
                    paymentEntity.getString("id")
            );

            payment.setAmount(
                    paymentEntity.getInt("amount") / 100
            );

            payment.setCurrency(
                    paymentEntity.getString("currency")
            );
        } else{
            payment.setAmount(plan.getPrice());
            payment.setCurrency("INR");
        }

        payment.setStatus("SUCCESS");
        payment.setProviderEventId(eventId);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        paymentRepository.save(payment);

        /*
         * Move the local subscription into
         * the next billing period.
         */
        subscription.setStartsAt(now);

        subscription.setEndsAt(
                now.atZone(ZoneId.systemDefault())
                        .plusMonths(plan.getMonths())
                        .toInstant()
        );

        subscription.setStatus("ACTIVE");
        subscription.setAutoRenew(true);
        subscription.setUpdatedAt(now);

        userSubscriptionRepository.save(subscription);
    }

    // handle cancel subscription
    private void handleSubscriptionCancelled(JSONObject webhook, String eventId) {
        JSONObject subscriptionEntity =
                webhook
                        .getJSONObject("payload")
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local subscription not found"
                                ));

        subscription.setAutoRenew(false);
        subscription.setStatus("CANCELLED");
        subscription.setUpdatedAt(Instant.now());

        userSubscriptionRepository.save(subscription);

    }

    // halted subscription
    private void handleSubscriptionHalted(
            JSONObject webhook,
            String eventId
    ) {

        JSONObject subscriptionEntity =
                webhook
                        .getJSONObject("payload")
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local subscription not found"
                                ));

        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(Instant.now());

        /*
         * Keep ACTIVE until endsAt.
         * Your getSubscriptionStatus() will eventually
         * transition it to EXPIRED.
         */

        userSubscriptionRepository.save(subscription);
    }

    // completed subscription
    private void handleSubscriptionCompleted(
            JSONObject webhook,
            String eventId
    ) {

        JSONObject subscriptionEntity =
                webhook
                        .getJSONObject("payload")
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Local subscription not found"
                                ));

        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(Instant.now());

        userSubscriptionRepository.save(subscription);
    }
}
