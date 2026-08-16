package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.ActivityRequest;
import com.dietapp.diet_app.health_profile.dto.request.MedicalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.ActivityResponse;
import com.dietapp.diet_app.health_profile.dto.response.MedicalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MedicalProfileMapper {

    @Mapping(target = "healthProfile", ignore = true)
    MedicalProfile toEntity(MedicalProfileRequest request);

    @Mapping(source = "healthProfile.id", target = "healthProfileId")
    MedicalProfileResponse toResponse(MedicalProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            MedicalProfileRequest request,
            @MappingTarget MedicalProfile entity
    );

}