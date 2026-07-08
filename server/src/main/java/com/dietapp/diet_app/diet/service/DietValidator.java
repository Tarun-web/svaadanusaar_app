package com.dietapp.diet_app.diet.service;

import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import org.springframework.stereotype.Component;

@Component
public class DietValidator {

    public void validateProfile(HealthProfile hp) {

        if (hp == null) throw new RuntimeException("Health profile missing");

        if (hp.getWeightKg() == null || hp.getWeightKg().doubleValue() <= 0)
            throw new RuntimeException("Invalid weight");

        if (hp.getGoal() == null)
            throw new RuntimeException("Goal not set");

        if (hp.getTrainingLevel() == null)
            throw new RuntimeException("Training level missing");

        if (hp.getMealsPerDay() == null || hp.getMealsPerDay() < 2)
            throw new RuntimeException("Invalid meal count");
    }
}
