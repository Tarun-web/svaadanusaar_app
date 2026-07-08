package com.dietapp.diet_app.subscription_plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private String id;
    private int months;
    private int price;
    private int chatbotDailyLimit;

}
