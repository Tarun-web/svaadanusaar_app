package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.LifestylePreferenceRequest;
import com.dietapp.diet_app.health_profile.dto.response.LifestylePreferenceResponse;
import com.dietapp.diet_app.health_profile.entity.LifestylePreferenceProfile;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
@Mapper(componentModel = "spring")
public interface LifestylePreferenceMapper {

    @Mapping(target = "healthProfile", ignore = true)
    LifestylePreferenceProfile toEntity(LifestylePreferenceRequest request);

    @Mapping(source = "healthProfile.id", target = "healthProfileId")
    LifestylePreferenceResponse toResponse(
            LifestylePreferenceProfile entity
    );

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            LifestylePreferenceRequest request,
            @MappingTarget LifestylePreferenceProfile entity
    );

}