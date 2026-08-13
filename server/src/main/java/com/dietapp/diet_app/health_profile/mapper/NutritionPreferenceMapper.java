package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.NutritionPreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.NutritionPreferenceResponse;
import com.dietapp.diet_app.health_profile.entity.NutritionPreferenceProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NutritionPreferenceMapper {
    /**
     * Request DTO → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    NutritionPreferenceProfile toEntity(
            NutritionPreferenceRequest request
    );

    /**
     * Entity → Response DTO
     */
    @Mapping(
            source = "healthProfile.id",
            target = "healthProfileId"
    )
    NutritionPreferenceResponse toResponse(
            NutritionPreferenceProfile entity
    );

    /**
     * Update existing entity. No new object will be created
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(
        NutritionPreferenceRequest request,
        @MappingTarget
        NutritionPreferenceProfile entity
    );
}
