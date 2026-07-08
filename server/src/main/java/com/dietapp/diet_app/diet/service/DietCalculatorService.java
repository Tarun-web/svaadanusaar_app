package com.dietapp.diet_app.diet.service;

import com.dietapp.diet_app.diet.dto.DietTargets;
import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DietCalculatorService {

    public DietTargets calculateDietTargets(HealthProfile hp){

        // get BMR
        double bmr = calculateBmr(hp);

        // get TDEE
        double tdee = bmr * activityMultiplier(hp.getActivityLevel());

        int targetCalories = adjustForGoals( (int) Math.round(tdee), hp.getGoal());

        // calculate macronutrients in grams
        int proteinG = calculateProteinSafe(hp);
        int proteinCalories = proteinG * 4;

        // 25% of calories from fats
        int fatsG = (int) Math.round((0.25 * tdee) / 9);
        int fatsCalories = fatsG * 9;

        // remaining calories from carbs
        int carbsCalories = (int) Math.round(tdee - proteinCalories - fatsCalories);
        int carbsG = (int) Math.round(carbsCalories / 4.0);

        return new DietTargets(
                targetCalories,
                proteinG,
                carbsG,
                fatsG
        );
    }

    //calculate BMR using Mifflin-St Jeor Equation
    public double calculateBmr(HealthProfile hp){
        if(hp.getGender().equalsIgnoreCase("male")){ // male
            return 10 * hp.getWeightKg().doubleValue()
                    + 6.25 * hp.getHeightCm()
                    - 5 * hp.getAge()
                    + 5;
        } else {                                                 // female
            return 10 * hp.getWeightKg().doubleValue()
                    + 6.25 * hp.getHeightCm()
                    - 5 * hp.getAge()
                    - 161;
        }
    }

    // get activity multiplier based on activity level
    public double activityMultiplier(String activityLevel){
        return switch (activityLevel.toLowerCase()) {
            case "sedentary" -> 1.2;
            case "lightly active" -> 1.375;
            case "moderate" -> 1.55;
            case "very active" -> 1.725;
            case "extra active" -> 1.9;
            default -> throw new IllegalArgumentException("Invalid activity level: " + activityLevel);
        };
    }

    // adjust calories based on goal
    private int adjustForGoals(int calories, String goal){
        return switch (goal.toLowerCase()) {
            case "fat_loss" -> calories - 400;
            case "muscle_gain" -> calories + 300;
            case "body_recomposition" -> calories - 100;
            case "weight_gain" -> calories + 500;
            default -> calories;
        };
    }

    // calculate protein requirement based on training level and goal
    private int calculateProtein(HealthProfile hp) {
        double proteinPerKg;

        String level = hp.getTrainingLevel() == null ? "" : hp.getTrainingLevel().toLowerCase();
        String goal = hp.getGoal() == null ? "" : hp.getGoal().toLowerCase();

        switch (level) {
            case "beginner" -> proteinPerKg = 1.2;
            case "intermediate" -> proteinPerKg = 1.5;
            case "advanced" -> proteinPerKg = 1.8;
            default -> proteinPerKg = 1.4; // safe default
        }

        switch (goal) {
            case "fat_loss" -> proteinPerKg += 0.2;
            case "muscle_gain" -> proteinPerKg += 0.4;
            case "body_recomposition" -> proteinPerKg += 0.3;
        }

        // Clamp to safe physiological range
        proteinPerKg = Math.min(proteinPerKg, 2.3);

        return (int) Math.round(proteinPerKg * hp.getWeightKg().doubleValue());
    }

    // Ensure protein does not exceed 2.2g/kg for safety
    public int calculateProteinSafe(HealthProfile hp) {

        int protein = calculateProtein(hp);

        double maxProtein = 2.2 * hp.getWeightKg().doubleValue();

        if (protein > maxProtein) {
            protein = (int) Math.round(maxProtein);
        }

        return protein;
    }



}
