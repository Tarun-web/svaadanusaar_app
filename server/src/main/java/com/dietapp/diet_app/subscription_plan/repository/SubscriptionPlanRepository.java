package com.dietapp.diet_app.subscription_plan.repository;

import com.dietapp.diet_app.subscription_plan.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPlanRepository  extends JpaRepository<SubscriptionPlan, String> {

    List<SubscriptionPlan> findByIsActiveTrue();
}
