package com.dietapp.diet_app.health_profile.NutritionTarget.mapper;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.NutritionTargetResponse;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.NutritionTarget;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NutritionTargetMapper {

    NutritionTargetResponse toResponse(NutritionTarget entity);
}
