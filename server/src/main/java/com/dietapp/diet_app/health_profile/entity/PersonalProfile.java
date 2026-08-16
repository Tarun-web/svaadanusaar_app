package com.dietapp.diet_app.health_profile.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.LivingArrangement;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.enums.Region;
import com.dietapp.diet_app.health_profile.enums.State;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "personal_profiles",
        indexes = {
                @Index(name = "idx_personal_profile_health_profile", columnList = "health_profile_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalProfile extends BaseEntity {


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_profile_id",
            nullable = false,
            unique = true
    )
    private HealthProfile healthProfile;

    @NotNull
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @NotNull
    @Min(80)
    @Max(250)
    @Column(nullable = false)
    private Double heightCm;

    @Enumerated(EnumType.STRING)
    private Occupation occupation;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    private State state;

    @Enumerated(EnumType.STRING)
    private LivingArrangement livingArrangement;

    private LocalTime wakeUpTime;

    private LocalTime sleepTime;

    private LocalTime breakfastTime;

    private LocalTime lunchTime;

    private LocalTime dinnerTime;

    @Builder.Default
    @Column(nullable = false)
    private Boolean nightShift = false;

}