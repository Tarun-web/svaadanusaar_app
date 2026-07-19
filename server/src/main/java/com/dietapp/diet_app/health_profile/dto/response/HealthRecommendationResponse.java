package com.dietapp.diet_app.health_profile.dto.response;
import com.dietapp.diet_app.health_profile.enums.RecommendationType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecommendationResponse {

    private String title;

    private String description;

    private RecommendationType type;
}
