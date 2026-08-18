package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.WorkoutProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.WorkoutProfileResponse;
import com.dietapp.diet_app.health_profile.entity.WorkoutProfile;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring"
)
public interface WorkoutProfileMapper {

    WorkoutProfile toEntity(WorkoutProfileRequest request);

    WorkoutProfileResponse toResponse(WorkoutProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            WorkoutProfileRequest request,
            @MappingTarget WorkoutProfile entity
    );

}