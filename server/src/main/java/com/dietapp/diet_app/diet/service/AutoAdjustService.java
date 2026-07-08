package com.dietapp.diet_app.diet.service;

import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import com.dietapp.diet_app.HealthProfile.repository.HealthProfileRepository;
import com.dietapp.diet_app.diet.dto.DietTargets;
import com.dietapp.diet_app.diet.entity.DietPlan;
import com.dietapp.diet_app.diet.repository.DietPlanRepository;
import com.dietapp.diet_app.progress.entity.WeightLog;
import com.dietapp.diet_app.progress.repository.WeightLogRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutoAdjustService {

    @Autowired
    private final WeightLogRepository weightLogRepository;
    @Autowired
    private final HealthProfileRepository healthProfileRepository;
    @Autowired
    private final DietPlanRepository dietPlanRepository;
    @Autowired
    private final DietCalculatorService dietCalculatorService;
    @Autowired
    private final DietValidator dietValidator;
    @Autowired
    private final DietService dietService;

    @Transactional
    public void autoAdjustDietPlans(UUID userId){

        // fetch last 7 days weight logs
        List<WeightLog> logs = weightLogRepository.findLast7ByUser(userId);

        // if no logs or < 7 logs, skip
        if(logs.isEmpty() || logs.size() < 7) return;

        logs.sort((a, b) -> b.getLogDate().compareTo(a.getLogDate())); // sort desc by date

        BigDecimal firstWeight = logs.get(0).getWeightKg();
        BigDecimal lastWeight = logs.get(6).getWeightKg();

        BigDecimal weeklyChange = firstWeight.subtract(lastWeight);

        // fetch current diet plan
        DietPlan currentDiet = dietPlanRepository.findActiveDietPlan(userId)
                .orElse(null);

        // skip if no dietplan exists
        if(currentDiet == null) return;

        // Prevent adjusting too frequently (must be at least 7 days old)
        if(currentDiet.getGeneratedAt() != null &&
        currentDiet.getGeneratedAt().isAfter(Instant.now().minusSeconds(7 * 86400))) {
            // Diet plan is newer than the oldest log → skip adjustment
            return;
        }

        int oldCalories = currentDiet.getDailyCalories();
        int newCalories = oldCalories;

        BigDecimal ONE = new BigDecimal("1.0");
        BigDecimal POINT_TWO = new BigDecimal("0.2");

        // 3️⃣ Apply adjustment rules
        if (weeklyChange.compareTo(ONE) > 0) {
            // Losing too fast → increase calories
            newCalories = oldCalories + 200;

        } else if (weeklyChange.compareTo(POINT_TWO) < 0) {
            // No progress → reduce calories
            newCalories = oldCalories - 150;

        } else {
            // Good progress → no change
            return;
        }


        // Recalculate macros based on new calories and existing protein ratio
        HealthProfile hp = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Health profile not found for user ID: " + userId));

        // Validate health profile before calculations
        dietValidator.validateProfile(hp);

        DietTargets adjustedTargets = dietCalculatorService.calculateDietTargets(hp);

        int minCalories = (int) (dietCalculatorService.calculateBmr(hp) * 0.75);
        int maxCalories = (int) (dietCalculatorService.calculateBmr(hp) * dietCalculatorService.activityMultiplier(hp.getActivityLevel()) * 1.25);
        // Clamp calories to safe range (75% - 125% of BMR)
        newCalories = Math.max(minCalories, newCalories);
        newCalories = Math.min(maxCalories, newCalories);



        // 6️⃣ Expire old diet
        currentDiet.setStatus("EXPIRED");
        currentDiet.setValidTill(Instant.now());
        dietPlanRepository.save(currentDiet);

        // 7️⃣ Create new diet version
        DietPlan newDiet = new DietPlan();

        newDiet.setId(UUID.randomUUID());
        newDiet.setUserId(userId);
        newDiet.setVersion(currentDiet.getVersion() + 1);
        newDiet.setGeneratedAt(Instant.now());
        newDiet.setValidFrom(Instant.now());
        newDiet.setStatus("ACTIVE");

        newDiet.setDailyCalories(newCalories);
        newDiet.setProteinTarget(adjustedTargets.getProteinG());

        // JSON payload stored in JSONB
        newDiet.setDietJson(dietService.buildDietJson(newCalories, adjustedTargets.getProteinG(), adjustedTargets.getCarbsG(), adjustedTargets.getFatsG()));

        dietPlanRepository.save(newDiet);
    }
}
