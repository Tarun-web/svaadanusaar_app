package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.FitnessGoalRequest;
import com.dietapp.diet_app.health_profile.dto.response.FitnessGoalResponse;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FitnessGoalMapper {

    FitnessGoalProfile toEntity(FitnessGoalRequest request);

    FitnessGoalResponse toResponse(FitnessGoalProfile entity);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(
            FitnessGoalRequest request,
            @MappingTarget FitnessGoalProfile entity
    );

}