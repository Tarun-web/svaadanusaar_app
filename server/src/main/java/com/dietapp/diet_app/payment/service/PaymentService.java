package com.dietapp.diet_app.payment.service;

import com.dietapp.diet_app.payment.dto.request.VerifyPaymentRequest;
import com.dietapp.diet_app.payment.dto.response.CreatePaymentResponse;
import com.dietapp.diet_app.payment.repository.PaymentRepository;
import com.dietapp.diet_app.subscription.dto.response.SubscriptionStatusResponse;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import com.dietapp.diet_app.subscription.service.SubscriptionService;
import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import com.razorpay.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.dietapp.diet_app.payment.entity.Payment;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;


    // fetch the razorpay key
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    // decides to call createRazorpayOrder() or createRazorpaySubscription()
    public CreatePaymentResponse createPayment(
            UUID userId,
            String planId,
            boolean autoRenew
    ) throws RazorpayException {

        SubscriptionPlan plan = subscriptionPlanRepository
                .findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        if (!plan.isActive()) {
            throw new RuntimeException("Plan is not active");
        }

        if (autoRenew) {
            return createRazorpaySubscription(userId, plan);
        }

        return createRazorparOrder(userId, plan.getId());
    }

    // method to create order without subscription (one time payment)
    public CreatePaymentResponse createRazorparOrder(UUID userId, String planId) throws RazorpayException {

        // fetch the existing plan
        SubscriptionPlan plan = subscriptionPlanRepository
                .findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // create order using razorpay client
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", plan.getPrice() * 100); // amount in the smallest currency unit
        orderRequest.put("currency", "INR");
        String receipt = "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        orderRequest.put("receipt", receipt);

        // Create the order from razorpay
        Order order = razorpayClient.orders.create(orderRequest);

        // create a payment with status as PENDING
        Payment payment = new Payment();

        payment.setId(UUID.randomUUID());

        payment.setUserId(userId);

        payment.setProvider("RAZORPAY");

        payment.setOrderId(
                order.get("id"));

        payment.setAmount(
                plan.getPrice());

        payment.setStatus("PENDING");

        payment.setCurrency("INR");

        payment.setCreatedAt(
                Instant.now());

        paymentRepository.save(payment);

        // return the response
        return new CreatePaymentResponse(
                order.get("id"),
                null,
                order.get("amount"),
                order.get("currency"),
                keyId,
                false);
    }

    // method to call subscription api of razorpay to create subscription
    private CreatePaymentResponse createRazorpaySubscription(
            UUID userId, SubscriptionPlan plan
    ) throws RazorpayException {

        // if plan not found
        if (plan.getRazorpayPlanId() == null) {
            throw new RuntimeException(
                    "Razorpay plan is not configured for " + plan.getId()
            );
        }

        // create subscription using razorpay client
        JSONObject subscriptionRequest = new JSONObject();

        subscriptionRequest.put(
                "plan_id",
                plan.getRazorpayPlanId()
        );

        subscriptionRequest.put(
                "total_count",
                calculateTotalCount(plan.getMonths())
        );

        subscriptionRequest.put(
                "quantity",
                1
        );

        subscriptionRequest.put(
                "customer_notify",
                true
        );

        Subscription razorpaySubscription =
                razorpayClient.subscriptions.create(subscriptionRequest);

        String razorpaySubscriptionId =
                razorpaySubscription.get("id");

        /*
         * We don't yet create the ACTIVE local subscription.
         *
         * The customer still has to complete Razorpay's
         * authorization/checkout.
         */

        return new CreatePaymentResponse(
                null,
                razorpaySubscriptionId,
                plan.getPrice() * 100,
                "INR",
                keyId,
                true
        );

    }

    // decides to call verifyOrderPayment() or verifySubscriptionPayment()
    @Transactional
    public void verifyPayment(
            UUID userId,
            VerifyPaymentRequest request
    ) throws RazorpayException {

        if (request.getRazorpaySubscriptionId() != null) {

            verifySubscriptionPayment(
                    userId,
                    request
            );

            return;
        }

        if (request.getRazorpayOrderId() != null) {

            verifyOrderPayment(
                    userId,
                    request
            );

            return;
        }

        throw new RazorpayException(
                "Neither order ID nor subscription ID supplied"
        );
    }

    @Transactional
    // Method to verify payment for one time payment
    public void verifyOrderPayment(UUID userId, VerifyPaymentRequest  request) throws RazorpayException {

        JSONObject options = new JSONObject();
        options.put(
                "razorpay_order_id",
                request.getRazorpayOrderId());

        options.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId());

        options.put(
                "razorpay_signature",
                request.getRazorpaySignature());

        // verify from the signature received from the frontend
        boolean valid =
                Utils.verifyPaymentSignature(
                        options,
                        razorpaySecret);

        // if the signature is not valid, throw an exception
        if (!valid) {
            throw new RazorpayException("Invalid signature");
        }

        // payment not found
        Payment payment = paymentRepository
                .findByOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Payment not found")
                );

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "Payment does not belong to user"
            );
        }
        // avoid duplicate payment (otherwise 2 or more subscriptions will be generated)
        if (paymentRepository.findByPaymentId(
                        request.getRazorpayPaymentId())
                .isPresent()) {

            return;
        }

        // set the payment id as razorpay payment id and status as SUCCESS
        payment.setPaymentId(
                request.getRazorpayPaymentId());

        payment.setStatus("SUCCESS");

        payment.setUpdatedAt(Instant.now());

        paymentRepository.save(payment);

        // now activate the subscription
        subscriptionService.startSubscription(
                userId,
                payment.getPlanId(),
                false,
                null
        );

    }

    // verify subscription based razorpay payment
    private void verifySubscriptionPayment(
            UUID userId,
            VerifyPaymentRequest request
    ) throws RazorpayException {

        JSONObject options = new JSONObject();

        options.put(
                "razorpay_subscription_id",
                request.getRazorpaySubscriptionId()
        );

        options.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId()
        );

        options.put(
                "razorpay_signature",
                request.getRazorpaySignature()
        );

        boolean valid =
                Utils.verifySubscription(
                        options,
                        razorpaySecret
                );

        if (!valid) {
            throw new RazorpayException(
                    "Invalid subscription signature"
            );
        }

        Payment payment =
                paymentRepository
                        .findByRazorpaySubscriptionId(
                                request.getRazorpaySubscriptionId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Subscription payment not found"
                                ));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "Subscription does not belong to user"
            );
        }

        if (payment.getPaymentId() != null) {
            return;
        }

        payment.setPaymentId(
                request.getRazorpayPaymentId()
        );

        payment.setStatus("SUCCESS");
        payment.setUpdatedAt(Instant.now());

        paymentRepository.save(payment);

        UserSubscription subscription =
                subscriptionService.startSubscription(
                        userId,
                        payment.getPlanId(),
                        true,
                        request.getRazorpaySubscriptionId()
                );

    }


    // Helper method to calculate total count of subscription based on months
    private int calculateTotalCount(int months) {

        return switch (months) {

            case 1 -> 1200;  // 100 years

            case 3 -> 400;   // 100 years

            case 6 -> 200;   // 100 years

            case 12 -> 100;  // 100 years

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported subscription duration: " + months
                    );
        };
    }

}
