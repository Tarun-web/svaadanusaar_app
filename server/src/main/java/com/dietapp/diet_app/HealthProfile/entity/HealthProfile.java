package com.dietapp.diet_app.HealthProfile.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private Integer age;
    private String gender;
    private Integer heightCm;
    private BigDecimal weightKg;

    private String goal;           // fat_loss, muscle_gain, body_recomposition
    private String trainingLevel;  // beginner, intermediate, advanced
    private String activityLevel;  // sedentary, lightly_active, moderately_active, very_active, extra_active
    private String dietType;
    private Integer mealsPerDay;

    @Column(columnDefinition = "jsonb")
    private String origin; // store JSON as String for now

    @Column(columnDefinition = "text[]")
    private String[] allergies;

    @Column(columnDefinition = "text[]")
    private String[] diseases;


    private Instant createdAt;
    private Instant updatedAt;

}
