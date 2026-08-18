package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.CookingProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CookingProfileResponse;
import com.dietapp.diet_app.health_profile.entity.CookingProfile;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CookingProfileMapper {

    CookingProfile toEntity(CookingProfileRequest request);

    CookingProfileResponse toResponse(CookingProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            CookingProfileRequest request,
            @MappingTarget CookingProfile entity
    );

}