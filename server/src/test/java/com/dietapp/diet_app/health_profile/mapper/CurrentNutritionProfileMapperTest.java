package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentNutritionProfileMapperTest {

    private final CurrentNutritionProfileMapper mapper = new CurrentNutritionProfileMapperImpl();

    @Test
    void request_mapsToEmbeddable() {
        CurrentNutritionProfileRequest request =
                CurrentNutritionProfileRequest.of(new BigDecimal("72.50"));

        CurrentNutritionProfile embeddable = mapper.toEmbeddable(request);

        assertThat(embeddable.getCurrentProteinGrams()).isEqualByComparingTo("72.50");
    }

    @Test
    void healthProfile_mapsToResponse() {
        HealthProfile profile = HealthProfile.builder()
                .currentNutritionProfile(CurrentNutritionProfile.builder()
                        .currentProteinGrams(new BigDecimal("55")).build())
                .build();

        assertThat(mapper.toResponse(profile).getCurrentProteinGrams()).isEqualByComparingTo("55");
    }

    @Test
    void healthProfileWithNullEmbedded_mapsToNullIntake_withoutError() {
        CurrentNutritionProfileResponse response =
                mapper.toResponse(HealthProfile.builder().build());

        assertThat(response).isNotNull();
        assertThat(response.getCurrentProteinGrams()).isNull();
    }

    @Test
    void healthProfileWithEmbeddedButNullGrams_mapsToNullIntake() {
        HealthProfile profile = HealthProfile.builder()
                .currentNutritionProfile(new CurrentNutritionProfile())
                .build();

        assertThat(mapper.toResponse(profile).getCurrentProteinGrams()).isNull();
    }
}
