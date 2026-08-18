package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.PersonalProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.PersonalProfileResponse;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonalProfileMapper {

    PersonalProfile toEntity(PersonalProfileRequest request);

    PersonalProfileResponse toResponse(PersonalProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            PersonalProfileRequest request,
            @MappingTarget PersonalProfile entity
    );

}