package com.dietapp.diet_app.payment.controller;

import com.dietapp.diet_app.payment.dto.request.CreateOrderRequest;
import com.dietapp.diet_app.payment.dto.request.VerifyPaymentRequest;
import com.dietapp.diet_app.payment.dto.response.CreateOrderResponse;
import com.dietapp.diet_app.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Authenticator;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // Method to create order using RazorPay Client
    @PostMapping("/create-order")
    public CreateOrderResponse createPayment(@RequestBody CreateOrderRequest createOrderRequest,
                                             Authentication auth) throws RazorpayException {

        // fetch user
        UUID user = (UUID) auth.getPrincipal();

        // call the paymentService
        return paymentService.createOrder(user, createOrderRequest.getPlanId());

    }

    // Method to verify Payment
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestBody VerifyPaymentRequest request,
            Authentication auth) throws RazorpayException {

        UUID userId =
                (UUID) auth.getPrincipal();

        paymentService.verifyPayment(
                userId,
                request);

        return ResponseEntity.ok(
                "Payment verified successfully");
    }
}
