package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.NutritionPreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.NutritionPreferenceResponse;
import com.dietapp.diet_app.health_profile.entity.NutritionPreferenceProfile;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NutritionPreferenceMapper {
    /**
     * Request DTO → Entity
     */
    NutritionPreferenceProfile toEntity(
            NutritionPreferenceRequest request
    );

    /**
     * Entity → Response DTO
     */
    NutritionPreferenceResponse toResponse(
            NutritionPreferenceProfile entity
    );

    /**
     * Update existing entity. No new object will be created
     */
    @InheritConfiguration(name = "toEntity")
    void updateEntity(
        NutritionPreferenceRequest request,
        @MappingTarget
        NutritionPreferenceProfile entity
    );
}
