package com.dietapp.diet_app.subscription_plan.controller;

import com.dietapp.diet_app.subscription_plan.dto.PlanResponse;
import com.dietapp.diet_app.subscription_plan.service.SubscriptionPlanService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionPlanController {

    @Autowired
    private final SubscriptionPlanService subscriptionService;

    // Get all active subscription plans
    @GetMapping("/plans/active")
    public List<PlanResponse> getActivePlans(){
        return subscriptionService.getActivePlans();
    }
}
