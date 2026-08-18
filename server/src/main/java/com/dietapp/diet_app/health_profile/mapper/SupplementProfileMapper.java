package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.SupplementProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.SupplementProfileResponse;
import com.dietapp.diet_app.health_profile.entity.SupplementProfile;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplementProfileMapper {

    SupplementProfile toEntity(SupplementProfileRequest request);

    SupplementProfileResponse toResponse(SupplementProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            SupplementProfileRequest request,
            @MappingTarget SupplementProfile entity
    );

}