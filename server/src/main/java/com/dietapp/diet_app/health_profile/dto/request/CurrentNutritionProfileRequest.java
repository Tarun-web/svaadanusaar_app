package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentNutritionProfileRequest {

    @Builder.Default
    private BigDecimal currentProteinGrams = BigDecimal.ZERO;
}
