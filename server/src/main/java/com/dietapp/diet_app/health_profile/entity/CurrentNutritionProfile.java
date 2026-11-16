package com.dietapp.diet_app.health_profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * Embedded value object stored as columns of {@code health_profiles}.
 * Deliberately not an entity and does not extend {@code BaseEntity}:
 * an embeddable cannot own an id or version, and inheriting them would
 * collide with HealthProfile's own columns.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentNutritionProfile {

    @Column(name = "current_protein_grams", precision = 8, scale = 2)
    private BigDecimal currentProteinGrams;
}
