package com.dietapp.diet_app.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    private UUID id;

    private UUID userId;

    private UUID subscriptionId;

    private String provider;

    private String orderId;

    private String paymentId;

    private String providerEventId;

    private Integer amount;

    private String currency;

    private String status;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "razorpay_subscription_id")
    private String razorpaySubscriptionId;

    private Instant createdAt;

    private Instant updatedAt;
}
