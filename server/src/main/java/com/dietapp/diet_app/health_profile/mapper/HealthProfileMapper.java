package com.dietapp.diet_app.health_profile.mapper;

import com.dietapp.diet_app.health_profile.dto.request.HealthProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.HealthProfileResponse;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HealthProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "onboardingCompleted", ignore = true)
    @Mapping(target = "profileCompletionPercentage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    HealthProfile toEntity(HealthProfileRequest request);

    @Mapping(source = "user.id", target = "userId")
    HealthProfileResponse toResponse(HealthProfile entity);

}