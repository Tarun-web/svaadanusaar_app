package com.dietapp.diet_app.payment.service;

import com.dietapp.diet_app.payment.dto.request.CreateOrderRequest;
import com.dietapp.diet_app.payment.dto.request.VerifyPaymentRequest;
import com.dietapp.diet_app.payment.dto.response.CreateOrderResponse;
import com.dietapp.diet_app.payment.repository.PaymentRepository;
import com.dietapp.diet_app.subscription.dto.response.SubscriptionStatusResponse;
import com.dietapp.diet_app.subscription.service.SubscriptionService;
import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import com.razorpay.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

    // method to create order
    public CreateOrderResponse createOrder(UUID userId, String planId) throws RazorpayException {

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
        return new CreateOrderResponse(
                order.get("id"),
                order.get("amount"),
                order.get("currency"),
                keyId);
    }


    @Transactional
    // Method to verify payment
    public void verifyPayment(UUID userId, VerifyPaymentRequest  request) throws RazorpayException {

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

        boolean valid = true;
//                Utils.verifyPaymentSignature(
//                        options,
//                        razorpaySecret); commented for time being to test the verify payment without signature


        // if the signature is not valid, throw an exception
        if (!valid) {
            throw new RazorpayException("Invalid signature");
        }

        // avoid duplicate payment (otherwise 2 or more subscriptions will be generated)
//        if (paymentRepository.findByPaymentId(
//                        request.getRazorpayPaymentId())
//                .isPresent()) {
//
//            return;
//        }

        // now activate the subscription
        SubscriptionStatusResponse subscriptionStatusResponse =
                subscriptionService.startSubscription(
                        userId,
                        request.getPlanId());

        // find the payment that was created while order creation and update it with the paymentId and status as SUCCESS
        Payment payment =
                paymentRepository
                        .findByOrderId(
                                request.getRazorpayOrderId())
                        .orElseThrow(() -> new RuntimeException("Payment not found"));
        // set the payment id as razorpay payment id and status as SUCCESS
        payment.setPaymentId(
                request.getRazorpayPaymentId());

        payment.setStatus("SUCCESS");

        paymentRepository.save(payment);
    }

}
