package com.dietapp.diet_app.payment.controller;

import com.dietapp.diet_app.payment.dto.request.CreatePaymentRequest;
import com.dietapp.diet_app.payment.dto.request.VerifyPaymentRequest;
import com.dietapp.diet_app.payment.dto.response.CreatePaymentResponse;
import com.dietapp.diet_app.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // Method to create order using RazorPay Client
    @PostMapping("/create")
    public CreatePaymentResponse createPayment(@RequestBody CreatePaymentRequest createPaymentRequest,
                                             Authentication auth) throws RazorpayException {

        UUID userId = (UUID) auth.getPrincipal();

        return paymentService.createPayment(
                userId,
                createPaymentRequest.getPlanId(),
                createPaymentRequest.isAutoRenew()
        );

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
