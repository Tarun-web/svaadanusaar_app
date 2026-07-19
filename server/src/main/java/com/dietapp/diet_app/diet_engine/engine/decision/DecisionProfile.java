package com.dietapp.diet_app.diet_engine.engine.decision;

import jakarta.persistence.Embeddable;

@Embeddable
public class DecisionProfile {

    private Boolean allowBudgetOptimization;

    private Boolean allowMealPrepRecipes;

    private Boolean prioritizeTaste;

    private Boolean prioritizeCost;

    private Boolean prioritizeVariety;

    private Boolean allowRestaurantMeals;

}