package com.dietapp.diet_app.subscription_plan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SubscriptionPlan {

    @Id
    private String id;
    private int months;
    private int price;
    private int chatbotDailyLimit;
    private boolean isActive;
}
