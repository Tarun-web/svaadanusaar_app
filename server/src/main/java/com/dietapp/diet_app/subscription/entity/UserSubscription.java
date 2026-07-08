package com.dietapp.diet_app.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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

    private Instant createdAt;
    private Instant updatedAt;
}
