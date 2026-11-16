package com.dietapp.diet_app.health_profile.NutritionTarget.scheduler;

import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of the scheduler's batch query.
 *
 * @param profile                   the (detached) adaptation profile; its
 *                                  {@code healthProfile} is an uninitialised
 *                                  proxy and must not be dereferenced
 * @param healthProfileId           the owning health profile's id
 * @param latestReportedIntakeGrams the user's latest reported intake from
 *                                  {@code health_profiles}; {@code null} when unknown
 */
public record EligibleProteinProfile(
        ProteinAdaptationProfile profile,
        UUID healthProfileId,
        BigDecimal latestReportedIntakeGrams
) {
}
