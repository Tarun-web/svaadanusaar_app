package com.dietapp.diet_app.health_profile.dto.response;

import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.LivingArrangement;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.Region;
import com.dietapp.diet_app.health_profile.enums.State;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalProfileResponse {

    /**
     * Personal Profile Id.
     */
    private UUID id;

    /**
     * Personal Information.
     */
    private LocalDate dateOfBirth;

    private Gender gender;

    private Double heightCm;

    private BigDecimal weightKg;

    /**
     * Lifestyle Information.
     */
    private Occupation occupation;

    private Region region;

    private State state;

    private LivingArrangement livingArrangement;

    /**
     * Daily Routine.
     */
    private LocalTime wakeUpTime;

    private LocalTime sleepTime;

    private LocalTime breakfastTime;

    private LocalTime lunchTime;

    private LocalTime dinnerTime;

    private Boolean nightShift;

    /**
     * Audit Fields.
     */
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}