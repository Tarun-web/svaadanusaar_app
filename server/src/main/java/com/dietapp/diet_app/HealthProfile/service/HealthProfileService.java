package com.dietapp.diet_app.HealthProfile.service;

import com.dietapp.diet_app.HealthProfile.dto.request.HealthProfileRequest;
import com.dietapp.diet_app.HealthProfile.dto.response.HealthProfileResponse;
import com.dietapp.diet_app.HealthProfile.entity.HealthProfile;
import com.dietapp.diet_app.HealthProfile.repository.HealthProfileRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class HealthProfileService {

    @Autowired
    private final HealthProfileRepository healthProfileRepository;

    // Update and save health profile
    public HealthProfileResponse upsert(UUID userId, HealthProfileRequest req) {
        // Implementation for upserting health profile goes here

        // Fetch existing profile or create new one
        HealthProfile hp = healthProfileRepository.findById(userId)
                .orElseGet(() -> {
                    HealthProfile newHp = new HealthProfile();
                    newHp.setUserId(userId);
                    newHp.setCreatedAt(Instant.now());
                    return newHp;
                });

        // Update fields
        hp.setAge(req.getAge());
        hp.setGender(req.getGender());
        hp.setHeightCm(req.getHeightCm());
        hp.setWeightKg(req.getWeightKg());
        hp.setGoal(req.getGoal());
        hp.setTrainingLevel(req.getTrainingLevel());
        hp.setActivityLevel(req.getActivityLevel());
        hp.setDietType(req.getDietType());
        hp.setMealsPerDay(req.getMealsPerDay());
        hp.setOrigin(req.getOrigin());
        hp.setAllergies(req.getAllergies());
        hp.setDiseases(req.getDiseases());
        hp.setUpdatedAt(Instant.now());

        healthProfileRepository.save(hp);
        return toResponse(hp);
    }

    // Get health profile by user ID
    public HealthProfileResponse getByUserId(UUID userId) {
        HealthProfile hp = healthProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Health profile not found for user ID: " + userId));
        return toResponse(hp);
    }

    // Convert entity to response DTO
    private HealthProfileResponse toResponse(HealthProfile hp) {
        return new HealthProfileResponse(
                hp.getUserId(),
                hp.getAge(),
                hp.getGender(),
                hp.getHeightCm(),
                hp.getWeightKg(),
                hp.getGoal(),
                hp.getTrainingLevel(),
                hp.getActivityLevel(),
                hp.getDietType(),
                hp.getMealsPerDay(),
                hp.getOrigin(),
                hp.getAllergies(),
                hp.getDiseases()
        );
    }
}
