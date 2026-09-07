package com.dietapp.diet_app.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    private Instant startsAt;
    private Instant endsAt;
    private String status; // ACTIVE, EXPIRED, CANCELLED
    private boolean autoRenew;

    @Column(name = "razorpay_subscription_id")
    private String razorpaySubscriptionId;

    private Instant createdAt;
    private Instant updatedAt;
}
