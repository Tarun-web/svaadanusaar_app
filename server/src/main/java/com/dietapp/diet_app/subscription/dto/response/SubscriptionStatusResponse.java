package com.dietapp.diet_app.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
public class SubscriptionStatusResponse {
    private String planId;
    private Instant startsAt;
    private Instant endsAt;
    private String status;
}



