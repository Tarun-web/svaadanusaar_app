package com.dietapp.diet_app.payment.repository;

import com.dietapp.diet_app.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentWebhookEventRepository
        extends JpaRepository<PaymentWebhookEvent, UUID> {

    boolean existsByProviderEventId(
            String providerEventId
    );
}
