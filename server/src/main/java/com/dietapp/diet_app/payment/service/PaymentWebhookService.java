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

        System.out.println("========== RAZORPAY WEBHOOK ==========");
        System.out.println(payload);
        System.out.println("======================================");

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
            case "payment.failed" ->
                    handlePaymentFailed(payload, eventId);
            case "subscription.pending" ->
                    handleSubscriptionPending(payload, eventId);

            default -> {
                // Ignore events we haven't implemented yet
            }
        }

        PaymentWebhookEvent webhookEvent =
                new PaymentWebhookEvent();

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
            if (!isValid) {
                throw new SecurityException(
                        "Invalid webhook signature"
                );
            }
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
    private void handleSubscriptionCharged(
            JSONObject webhook,
            String eventId
    ) {

        JSONObject payload =
                webhook.getJSONObject("payload");

        JSONObject subscriptionEntity =
                payload
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        JSONObject paymentEntity =
                payload
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpayPaymentId =
                paymentEntity.getString("id");

        Instant now = Instant.now();

        /*
         * First, check whether this exact Razorpay payment
         * was already processed.
         */
        Payment payment =
                paymentRepository
                        .findByPaymentId(razorpayPaymentId)
                        .orElse(null);

        /*
         * If it doesn't exist, look for the PENDING payment
         * created when we created the Razorpay subscription.
         */
        if (payment == null) {

            payment =
                    paymentRepository
                            .findFirstByRazorpaySubscriptionIdAndStatusOrderByCreatedAtDesc(
                                    razorpaySubscriptionId,
                                    "PENDING"
                            )
                            .orElse(null);
        }

        /*
         * If no local payment exists at all, create one.
         */
        if (payment == null) {

            payment = new Payment();

            payment.setUserId(null);
            payment.setPlanId(null);
            payment.setSubscriptionId(null);
            payment.setProvider("RAZORPAY");
            payment.setRazorpaySubscriptionId(
                    razorpaySubscriptionId
            );
            payment.setCreatedAt(now);
        }

        /*
         * Fill/update payment information from Razorpay.
         */
        payment.setPaymentId(razorpayPaymentId);
        payment.setAmount(
                paymentEntity.getInt("amount") / 100
        );
        payment.setCurrency(
                paymentEntity.getString("currency")
        );
        payment.setStatus("SUCCESS");
        payment.setProviderEventId(eventId);
        payment.setUpdatedAt(now);

        /*
         * Find the local subscription.
         */
        UserSubscription subscription =
                userSubscriptionRepository
                        .findByRazorpaySubscriptionId(
                                razorpaySubscriptionId
                        )
                        .orElse(null);

        /*
         * If /verify has not created the local subscription yet,
         * create it from the PENDING payment.
         */
        if (subscription == null) {

            if (payment.getUserId() == null
                    || payment.getPlanId() == null) {

                throw new RuntimeException(
                        "Cannot create local subscription. "
                                + "Pending payment information is missing."
                );
            }

            subscription =
                    createLocalSubscriptionFromWebhook(
                            payment,
                            subscriptionEntity
                    );
        }

        /*
         * Make sure the payment is linked to the
         * local subscription.
         */
        payment.setUserId(subscription.getUserId());
        payment.setSubscriptionId(subscription.getId());
        payment.setPlanId(subscription.getPlanId());
        payment.setRazorpaySubscriptionId(
                razorpaySubscriptionId
        );

        paymentRepository.save(payment);

        /*
         * Razorpay's current_end is the actual end of
         * the current paid billing cycle.
         */
        long currentEnd =
                subscriptionEntity.getLong("current_end");

        Instant endsAt =
                Instant.ofEpochSecond(currentEnd);

        subscription.setEndsAt(endsAt);
        subscription.setStatus("ACTIVE");
        subscription.setAutoRenew(true);
        subscription.setUpdatedAt(now);

        userSubscriptionRepository.save(subscription);
    }
    private UserSubscription createLocalSubscriptionFromWebhook(
            Payment payment,
            JSONObject subscriptionEntity
    ) {

        Instant now = Instant.now();

        long currentStart =
                subscriptionEntity.getLong("current_start");

        long currentEnd =
                subscriptionEntity.getLong("current_end");

        UserSubscription subscription =
                new UserSubscription();

        subscription.setUserId(payment.getUserId());
        subscription.setPlanId(payment.getPlanId());

        subscription.setStartsAt(
                Instant.ofEpochSecond(currentStart)
        );

        subscription.setEndsAt(
                Instant.ofEpochSecond(currentEnd)
        );

        subscription.setStatus("ACTIVE");
        subscription.setAutoRenew(true);

        subscription.setRazorpaySubscriptionId(
                subscriptionEntity.getString("id")
        );

        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);

        return userSubscriptionRepository.save(subscription);
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

    // handle failed payment
    private void handlePaymentFailed(
            String payload,
            String eventId
    ) {

        JSONObject webhook = new JSONObject(payload);

        JSONObject paymentEntity =
                webhook.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpayPaymentId =
                paymentEntity.getString("id");

        // Idempotency
        if (paymentRepository
                .findByPaymentId(razorpayPaymentId)
                .isPresent()) {
            return;
        }

        Instant now = Instant.now();

        Payment payment = new Payment();

        payment.setProvider("RAZORPAY");
        payment.setPaymentId(razorpayPaymentId);

        if (paymentEntity.has("order_id")
                && !paymentEntity.isNull("order_id")) {

            payment.setOrderId(
                    paymentEntity.getString("order_id")
            );
        }

        payment.setAmount(
                paymentEntity.getInt("amount") / 100
        );

        payment.setCurrency(
                paymentEntity.getString("currency")
        );

        payment.setStatus("FAILED");

        payment.setProviderEventId(eventId);

        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        paymentRepository.save(payment);
    }

    // handle pending subscription
    private void handleSubscriptionPending(
            String payload,
            String eventId
    ) {

        JSONObject webhook = new JSONObject(payload);

        JSONObject payloadObject =
                webhook.getJSONObject("payload");

        JSONObject subscriptionEntity =
                payloadObject
                        .getJSONObject("subscription")
                        .getJSONObject("entity");

        JSONObject paymentEntity =
                payloadObject
                        .getJSONObject("payment")
                        .getJSONObject("entity");

        String razorpaySubscriptionId =
                subscriptionEntity.getString("id");

        String razorpayPaymentId =
                paymentEntity.getString("id");

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
         * Find the FAILED payment created by payment.failed
         */
        Payment payment =
                paymentRepository
                        .findByPaymentId(razorpayPaymentId)
                        .orElse(null);

        if (payment != null) {

            payment.setUserId(subscription.getUserId());
            payment.setSubscriptionId(subscription.getId());
            payment.setPlanId(subscription.getPlanId());
            payment.setRazorpaySubscriptionId(
                    razorpaySubscriptionId
            );

            payment.setUpdatedAt(Instant.now());

            paymentRepository.save(payment);
        }

        /*
         * Razorpay is retrying the payment.
         *
         * Do NOT expire the user's current subscription.
         * Access remains available until endsAt.
         */

        subscription.setUpdatedAt(Instant.now());

        userSubscriptionRepository.save(subscription);
    }
}
