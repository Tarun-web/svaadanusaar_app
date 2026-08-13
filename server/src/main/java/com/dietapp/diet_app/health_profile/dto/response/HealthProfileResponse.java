package com.dietapp.diet_app.health_profile.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileResponse {

    /**
     * Health Profile Id.
     */
    private UUID id;

    /**
     * Owner of the profile.
     */
    private UUID userId;

    /**
     * Indicates whether onboarding
     * has been completed.
     */
    private Boolean onboardingCompleted;

    /**
     * Completion percentage.
     */
    private Integer profileCompletionPercentage;

    /**
     * Audit information.
     */
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}