package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.PersonalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.PersonalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonalProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PersonalProfile toEntity(PersonalProfileRequest request);

    @Mapping(source = "healthProfile.id", target = "healthProfileId")
    PersonalProfileResponse toResponse(PersonalProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            PersonalProfileRequest request,
            @MappingTarget PersonalProfile entity
    );

}