package com.dietapp.diet_app.diet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DietTargets {
    private int calories;
    private int proteinG;
    private int carbsG;
    private int fatsG;
}
