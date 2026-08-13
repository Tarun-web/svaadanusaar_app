package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.FitnessGoalRequest;
import com.dietapp.diet_app.health_profile.dto.response.FitnessGoalResponse;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FitnessGoalMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    FitnessGoalProfile toEntity(FitnessGoalRequest request);

    @Mapping(source = "healthProfile.id", target = "healthProfileId")
    FitnessGoalResponse toResponse(FitnessGoalProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            FitnessGoalRequest request,
            @MappingTarget FitnessGoalProfile entity
    );

}