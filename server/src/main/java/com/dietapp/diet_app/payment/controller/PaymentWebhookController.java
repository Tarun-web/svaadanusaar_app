package com.dietapp.diet_app.payment.controller;

import com.dietapp.diet_app.payment.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                              @RequestHeader("X-Razorpay-Signature") String signature,
                                              @RequestHeader("x-razorpay-event-id") String eventId){

        paymentWebhookService.processWebhook(payload, signature, eventId);
        return ResponseEntity.ok().build();
    }
}
