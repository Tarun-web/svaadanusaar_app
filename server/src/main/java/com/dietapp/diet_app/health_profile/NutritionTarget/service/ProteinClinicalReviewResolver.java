package com.dietapp.diet_app.health_profile.NutritionTarget.service;

import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.ProteinCalculator;
import com.dietapp.diet_app.health_profile.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Determines, for many health profiles at once, which ones currently require
 * protein clinical review.
 *
 * <p>The decision uses the users' <em>current</em> medical conditions and the
 * single rule in {@link ProteinCalculator#CLINICAL_REVIEW_CONDITIONS}; nothing
 * is stored on the adaptation profile, so a condition that is added or
 * removed takes effect on the next call.</p>
 */
@Component
@RequiredArgsConstructor
public class ProteinClinicalReviewResolver {

    private final MedicalProfileRepository medicalProfileRepository;

    /**
     * Returns the subset of {@code healthProfileIds} that currently require
     * clinical review, using one database query regardless of batch size.
     */
    @Transactional(readOnly = true)
    public Set<UUID> findRequiringClinicalReview(
            Collection<UUID> healthProfileIds) {

        if (healthProfileIds == null || healthProfileIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(
                medicalProfileRepository.findHealthProfileIdsWithAnyCondition(
                        healthProfileIds,
                        ProteinCalculator.CLINICAL_REVIEW_CONDITIONS
                )
        );
    }
}
