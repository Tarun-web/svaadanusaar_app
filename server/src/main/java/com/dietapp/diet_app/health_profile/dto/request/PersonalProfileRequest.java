package com.dietapp.diet_app.health_profile.dto.request;

import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.LivingArrangement;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.Region;
import com.dietapp.diet_app.health_profile.enums.State;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalProfileRequest {

    /**
     * Health Profile identifier.
     */
    @NotNull(message = "Health profile id is required.")
    private UUID healthProfileId;

    /**
     * Date of birth.
     */
    @NotNull(message = "Date of birth is required.")
    private LocalDate dateOfBirth;

    /**
     * Gender.
     */
    @NotNull(message = "Gender is required.")
    private Gender gender;

    /**
     * Height in centimeters.
     */
    @NotNull(message = "Height is required.")
    @Min(value = 80, message = "Height must be at least 80 cm.")
    @Max(value = 250, message = "Height cannot exceed 250 cm.")
    private Double heightCm;

    /**
     * Occupation.
     */
    private Occupation occupation;

    /**
     * Region.
     */
    private Region region;

    /**
     * State.
     */
    private State state;

    /**
     * Living arrangement.
     */
    private LivingArrangement livingArrangement;

    /**
     * Daily schedule.
     */
    private LocalTime wakeUpTime;

    private LocalTime sleepTime;

    private LocalTime breakfastTime;

    private LocalTime lunchTime;

    private LocalTime dinnerTime;

    /**
     * Night shift worker.
     */
    @Builder.Default
    private Boolean nightShift = false;
}