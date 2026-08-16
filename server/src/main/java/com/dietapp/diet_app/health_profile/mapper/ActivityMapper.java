package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.ActivityRequest;
import com.dietapp.diet_app.health_profile.dto.response.ActivityResponse;
import com.dietapp.diet_app.health_profile.entity.ActivityProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    @Mapping(target = "workoutProfile", ignore = true)
    ActivityProfile toEntity(ActivityRequest request);

    ActivityResponse toResponse(ActivityProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            ActivityRequest request,
            @MappingTarget ActivityProfile entity
    );

}