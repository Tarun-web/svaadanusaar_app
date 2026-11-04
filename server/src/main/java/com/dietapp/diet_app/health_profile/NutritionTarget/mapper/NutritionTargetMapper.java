package com.dietapp.diet_app.health_profile.NutritionTarget.mapper;

import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.NutritionTargetResponse;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.NutritionTarget;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps a persisted {@link NutritionTarget} entity to a
 * {@link NutritionTargetResponse}.
 *
 * <p>Fields that are live-calculated (macros, adaptation state, warnings) are
 * NOT populated by this mapper — they are set directly on the response by
 * {@code NutritionTargetServiceImpl}.  The unmapped-target policy is therefore
 * set to {@code IGNORE} to avoid compile errors for those extra fields.</p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NutritionTargetMapper {

    NutritionTargetResponse toResponse(NutritionTarget entity);
}
