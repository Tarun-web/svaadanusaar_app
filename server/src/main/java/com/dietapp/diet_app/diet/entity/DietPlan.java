package com.dietapp.diet_app.diet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "diet_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietPlan {

    @Id
    private UUID id;

    /**
     * Owner of this diet plan
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Version of diet for the user
     * Starts at 1 and increments on every regeneration
     */
    @Column(nullable = false)
    private int version;

    /**
     * ACTIVE / EXPIRED
     * (use enum later if needed)
     */
    @Column(nullable = false)
    private String status;

    /**
     * Time window during which this diet is valid
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_till")
    private Instant validTill;

    /**
     * High-level numeric targets (quick access fields)
     */
    @Column(name = "daily_calories", nullable = false)
    private int dailyCalories;

    @Column(name = "protein_target", nullable = false)
    private int proteinTarget;

    /**
     * Flexible JSON payload:
     * - macros
     * - meal structure (future)
     * - food options (future)
     * - AI explanation (future)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diet_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> dietJson;


    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
