package com.dietapp.diet_app.health_profile.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifestylePreferenceResponse {

    private UUID id;

    private UUID healthProfileId;

    private Boolean hosteller;

    private Boolean officeLunchAvailable;

    private Boolean travelsFrequently;

    private Boolean foodDeliveryAvailable;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}