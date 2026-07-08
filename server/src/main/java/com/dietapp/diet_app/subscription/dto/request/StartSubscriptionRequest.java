package com.dietapp.diet_app.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartSubscriptionRequest {

    @NotBlank
    private String planId;
}
