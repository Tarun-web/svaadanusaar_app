package com.dietapp.diet_app.payment.repository;

import com.dietapp.diet_app.payment.entity.Payment;
import com.dietapp.diet_app.subscription.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentId(
            String paymentId);

    Optional<Payment> findByOrderId(
            String orderId);

    Optional<Payment> findByRazorpaySubscriptionId(
            String razorpaySubscriptionId
    );
}

