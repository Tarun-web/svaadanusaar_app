package com.dietapp.diet_app.health_profile.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifestylePreferenceRequest {

    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    @Builder.Default
    private Boolean hosteller = false;

    @Builder.Default
    private Boolean officeLunchAvailable = false;

    @Builder.Default
    private Boolean travelsFrequently = false;

    @Builder.Default
    private Boolean foodDeliveryAvailable = true;

}