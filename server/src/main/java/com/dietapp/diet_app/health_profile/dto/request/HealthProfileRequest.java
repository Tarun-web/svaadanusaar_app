package com.dietapp.diet_app.health_profile.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileRequest {

    /**
     * User for whom the Health Profile
     * should be created.
     */
    @NotNull(message = "User Id is required.")
    private UUID userId;

}