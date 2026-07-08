package com.dietapp.diet_app.diet.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents nutritional values per 100g
 */
@Data
@AllArgsConstructor
public class FoodItem {

    private String name;

    // per 100g values
    private int calories;
    private int protein;
    private int carbs;
    private int fats;
}

