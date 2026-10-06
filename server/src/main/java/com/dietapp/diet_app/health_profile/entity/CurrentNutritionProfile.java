package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentNutritionProfile extends BaseEntity {

    @Column(name = "current_protein_grams", precision = 8, scale = 2)
    private BigDecimal currentProteinGrams;
}
