package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.SupplementType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementProfileResponse {

    private UUID id;

    private Boolean openToSupplements;

    private Set<SupplementType> currentSupplements = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}