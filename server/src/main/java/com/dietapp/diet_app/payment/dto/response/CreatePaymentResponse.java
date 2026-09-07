package com.dietapp.diet_app.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CreatePaymentResponse {
    private String orderId;

    private String subscriptionId;

    private int amount;

    private String currency;

    private String keyId;

    private boolean autoRenew;
}
