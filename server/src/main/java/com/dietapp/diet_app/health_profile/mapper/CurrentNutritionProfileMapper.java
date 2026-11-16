package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurrentNutritionProfileMapper {

    CurrentNutritionProfile toEmbeddable(CurrentNutritionProfileRequest request);

    /**
     * Null-safe: a HealthProfile with no embedded value yields a response
     * whose {@code currentProteinGrams} is null.
     */
    @Mapping(source = "currentNutritionProfile.currentProteinGrams",
             target = "currentProteinGrams")
    CurrentNutritionProfileResponse toResponse(HealthProfile healthProfile);
}
