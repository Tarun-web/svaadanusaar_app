package com.dietapp.diet_app.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    private String planId;
    private boolean autoRenew;
}
