package com.dietapp.diet_app.diet.service;

import com.dietapp.diet_app.diet.model.FoodItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Simple curated food list for MVP
 * Later replace with external API
 */
@Component
public class FoodDatabase {

    private final Map<String, List<FoodItem>> mealFoods = Map.of(

            "Breakfast", List.of(
                    new FoodItem("Oats", 389, 16, 66, 7),
                    new FoodItem("Boiled Eggs", 155, 13, 1, 11),
                    new FoodItem("Milk", 60, 3, 5, 3),
                    new FoodItem("Banana", 89, 1, 23, 0)
            ),

            "Lunch", List.of(
                    new FoodItem("Rice", 130, 2, 28, 0),
                    new FoodItem("Chicken Breast", 165, 31, 0, 3),
                    new FoodItem("Paneer", 265, 18, 3, 20),
                    new FoodItem("Dal", 116, 9, 20, 1)
            ),

            "Snack", List.of(
                    new FoodItem("Peanuts", 567, 26, 16, 49),
                    new FoodItem("Apple", 52, 0, 14, 0),
                    new FoodItem("Yogurt", 59, 10, 3, 0),
                    new FoodItem("Protein Shake", 120, 24, 3, 1)
            ),

            "Dinner", List.of(
                    new FoodItem("Roti", 120, 3, 18, 1),
                    new FoodItem("Egg Curry", 155, 13, 1, 11),
                    new FoodItem("Paneer", 265, 18, 3, 20),
                    new FoodItem("Vegetables", 80, 3, 15, 0)
            )
    );

    public List<FoodItem> getFoodsForMeal(String mealName) {
        return mealFoods.getOrDefault(mealName, List.of());
    }
}

