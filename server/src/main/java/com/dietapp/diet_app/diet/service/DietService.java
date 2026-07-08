package com.dietapp.diet_app.diet.service;

import com.dietapp.diet_app.diet.dto.DietTargets;
import com.dietapp.diet_app.diet.entity.DietPlan;
import com.dietapp.diet_app.diet.model.FoodItem;
import com.dietapp.diet_app.diet.repository.DietPlanRepository;
import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import com.dietapp.diet_app.HealthProfile.repository.HealthProfileRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
public class DietService {

    @Autowired
    private final HealthProfileRepository healthProfileRepository;
    @Autowired
    private final DietCalculatorService dietCalculatorService;
    @Autowired
    private final DietPlanRepository dietPlanRepository;
    @Autowired
    private final FoodDatabase foodDatabase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * Generates a fresh diet plan for the user based on their health profile.
     * - Deactivates any existing active plan
     * - Calculates new targets
     * - Persists them
     */


    // generate diet
    public DietTargets generateDietPlan(UUID userId){

        Instant now = Instant.now();

        // get health profile info
        HealthProfile hp = healthProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Health profile not found for user ID: " + userId));

        // Fetch last diet plan (if any)
        DietPlan previous = dietPlanRepository
                .findTopByUserIdOrderByVersionDesc(userId)
                .orElse(null);

        int newVersion = (previous == null) ? 1 : previous.getVersion() + 1;

        // Expire Previous active Plan
        if (previous != null && previous.getValidTill() == null) {
            previous.setValidTill(now);
            previous.setStatus("EXPIRED");
            dietPlanRepository.save(previous);
        }

        // calculate new diet targets
        DietTargets targets = dietCalculatorService.calculateDietTargets(hp);

        // 5️⃣ Build JSON payload (future-proof)
        Map<String, Object> dietJson = new HashMap<>();
        dietJson.put("calories", targets.getCalories());
        dietJson.put("protein", targets.getProteinG());
        dietJson.put("carbs", targets.getCarbsG());
        dietJson.put("fats", targets.getFatsG());
        dietJson.put("source", "SYSTEM_GENERATED");


        // Persist new Diet Plan
        DietPlan newPlan = DietPlan.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .version(newVersion)
                .status("ACTIVE")
                .validFrom(now)
                .validTill(null)
                .dailyCalories(targets.getCalories())
                .proteinTarget(targets.getProteinG())
                // For quick access to macros, we store them in separate columns. The full structure goes into dietJson.
                .dietJson(buildDietJson(targets.getCalories(),
                        targets.getProteinG(),
                        targets.getCarbsG(),
                        targets.getFatsG()))

                .generatedAt(now)
                .createdAt(now)
                .build();

        dietPlanRepository.save(newPlan);
        return targets;
    }


    /*
     * Returns the currently active diet plan for the user.
     */
    public DietTargets getActiveDietPlan(UUID userId){

        DietPlan plan = dietPlanRepository.findCurrentDiet(userId, Instant.now())
                .orElseThrow(() -> new RuntimeException("No active diet plan found for user ID: " + userId));

        // extract macronutrient targets from dietJson
        Map<String, Object> json = plan.getDietJson();

        int carbs = ((Number) json.get("carbs")).intValue();
        int fats  = ((Number) json.get("fats")).intValue();

        return new DietTargets(
                plan.getDailyCalories(),
                plan.getProteinTarget(),
                carbs,
                fats
        );

    }
    // For frontend: Get the full diet JSON structure for the active plan
    public Map<String, Object> getCurrentDietJson(UUID userId) {
        DietPlan diet = dietPlanRepository.findActiveDietPlan(userId)
                .orElseThrow();
        return diet.getDietJson();
    }


    // extract integer value from dietJson by key
    private int extract(String json, String key) {
        try {
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            return ((Number) map.get(key)).intValue();
        } catch (Exception e) {
            throw new RuntimeException("Invalid diet JSON", e);
        }
    }

    // Build a detailed diet JSON structure (can be expanded in future with meals, food options, etc.)
    public Map<String, Object> buildDietJson(
            int totalCalories,
            int proteinTarget,
            int carbTarget,
            int fatTarget) {

        Map<String, Integer> mealSplit = splitCalories(totalCalories);

        List<Map<String, Object>> meals = new ArrayList<>();

        for (var entry : mealSplit.entrySet()) {

            String mealName = entry.getKey();
            int mealCalories = entry.getValue();

            List<FoodItem> foods = foodDatabase.getFoodsForMeal(mealName);

            List<Map<String, Object>> foodJson = getMaps(mealCalories, foods);

            meals.add(Map.of(
                    "name", mealName,
                    "targetCalories", mealCalories,
                    "foods", foodJson
            ));
        }

        return Map.of(
                "summary", Map.of(
                        "calories", totalCalories,
                        "protein", proteinTarget,
                        "carbs", carbTarget,
                        "fats", fatTarget
                ),
                "meals", meals
        );
    }

    private List<Map<String, Object>> getMaps(int mealCalories, List<FoodItem> foods) {
        List<Map<String, Object>> foodJson = new ArrayList<>();

        int caloriesPerFood = mealCalories / Math.max(1, foods.size());

        for (FoodItem food : foods) {

            int grams = calculateGrams(caloriesPerFood, food.getCalories());

            Map<String, Object> f = Map.of(
                    "name", food.getName(),
                    "quantity_g", grams,
                    "calories", (grams * food.getCalories()) / 100,
                    "protein", (grams * food.getProtein()) / 100,
                    "carbs", (grams * food.getCarbs()) / 100,
                    "fats", (grams * food.getFats()) / 100
            );

            foodJson.add(f);
        }
        return foodJson;
    }


    // Example of splitting calories into meals (can be expanded in future)
    private Map<String, Integer> splitCalories(int totalCalories) {
        return Map.of(
                "Breakfast", (int) (totalCalories * 0.25),
                "Lunch", (int) (totalCalories * 0.35),
                "Snack", (int) (totalCalories * 0.15),
                "Dinner", (int) (totalCalories * 0.25)
        );
    }
    /**
     * Calculates required grams for given calories
     */
    private int calculateGrams(int targetCalories, int caloriesPer100g) {
        return (int) Math.round((targetCalories * 100.0) / caloriesPer100g);
    }





}
