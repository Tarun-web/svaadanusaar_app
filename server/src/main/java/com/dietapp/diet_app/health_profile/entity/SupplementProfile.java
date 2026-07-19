package com.dietapp.diet_app.health_profile.entity;
import com.dietapp.diet_app.health_profile.enums.SupplementType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.List;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementProfile {

    private Boolean openToSupplements;

    @Column(columnDefinition = "jsonb")
    private List<SupplementType> currentSupplements;  // stored as JSONB (enum names)

}
