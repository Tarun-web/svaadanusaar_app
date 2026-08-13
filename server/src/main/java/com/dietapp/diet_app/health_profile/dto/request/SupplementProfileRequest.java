package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.SupplementType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementProfileRequest {

    @NotNull(message = "Health Profile Id is required.")
    private UUID healthProfileId;

    @Builder.Default
    private Boolean openToSupplements = false;

    @Builder.Default
    private Set<SupplementType> currentSupplements = new HashSet<>();

}