package com.dietapp.diet_app.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentRequest {
    private String razorpayOrderId;

    private String razorpaySubscriptionId;

    private String razorpayPaymentId;

    private String razorpaySignature;
}
