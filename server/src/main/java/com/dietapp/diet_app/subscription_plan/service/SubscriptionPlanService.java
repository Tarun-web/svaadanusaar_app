package com.dietapp.diet_app.subscription_plan.service;

import com.dietapp.diet_app.subscription_plan.dto.PlanResponse;
import com.dietapp.diet_app.subscription_plan.repository.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionPlanService {

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    public List<PlanResponse> getActivePlans(){
        return subscriptionPlanRepository.findByIsActiveTrue()
                .stream()
                .map(plan -> new PlanResponse(
                        plan.getId(),
                        plan.getMonths(),
                        plan.getPrice(),
                        plan.getChatbotDailyLimit()
                ))
                .toList();
    }

}
