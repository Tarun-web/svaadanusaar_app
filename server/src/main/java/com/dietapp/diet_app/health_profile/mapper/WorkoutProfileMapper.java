package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.WorkoutProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.WorkoutProfileResponse;
import com.dietapp.diet_app.health_profile.entity.WorkoutProfile;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = ActivityMapper.class
)
public interface WorkoutProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    WorkoutProfile toEntity(WorkoutProfileRequest request);

    @Mapping(source = "healthProfile.id", target = "healthProfileId")
    WorkoutProfileResponse toResponse(WorkoutProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            WorkoutProfileRequest request,
            @MappingTarget WorkoutProfile entity
    );

}