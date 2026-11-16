package com.dietapp.diet_app.health_profile.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentNutritionProfileResponse {

    /**
     * Self-reported daily protein intake in grams; {@code null} when not supplied.
     */
    private BigDecimal currentProteinGrams;
}
