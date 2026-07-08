package com.dietapp.diet_app.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    private String orderId;
    private Integer amount;
    private String currency;
    private String keyId;
}
