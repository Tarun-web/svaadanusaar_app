package com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator;
import com.dietapp.diet_app.health_profile.NutritionTarget.dto.response.CalorieSafetyConstraints;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.MedicalProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CalorieSafetyEngineTest {

    private final CalorieSafetyEngine engine =
            new CalorieSafetyEngine();


    @Test
    void shouldHandleNormalUser() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        MedicalProfile medical =
                medicalProfile();

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        medical
                );

        assertNotNull(result);

        assertFalse(
                result.requiresClinicalReview()
        );

        assertNotNull(
                result.minimumCalories()
        );

        assertNotNull(
                result.maximumCalories()
        );

        assertTrue(
                result.minimumCalories()
                        .compareTo(
                                result.maximumCalories()
                        ) <= 0
        );
    }


    @Test
    void shouldWarnForUnderweightUser() {

        PersonalProfile personalProfile =
                personalProfile(
                        50.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        null
                );

        assertNotNull(result);

        assertTrue(
                result.warnings()
                        .stream()
                        .anyMatch(
                                warning ->
                                        warning
                                                .toLowerCase()
                                                .contains("below")
                        )
        );

        assertEquals(
                BigDecimal.valueOf(300),
                result.maximumDailyDeficit()
        );
    }


    @Test
    void shouldWarnForFattyLiver() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        MedicalProfile medical =
                medicalProfile(
                        MedicalCondition.FATTY_LIVER
                );

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        medical
                );

        assertTrue(
                result.warnings()
                        .stream()
                        .anyMatch(
                                warning ->
                                        warning
                                                .toLowerCase()
                                                .contains("fatty liver")
                        )
        );

        assertFalse(
                result.requiresClinicalReview()
        );
    }


    @Test
    void shouldRequireClinicalReviewForKidneyDisease() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        MedicalProfile medical =
                medicalProfile(
                        MedicalCondition.KIDNEY_DISEASE
                );

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        medical
                );

        assertTrue(
                result.requiresClinicalReview()
        );
    }


    @Test
    void shouldRequireClinicalReviewForLiverDisease() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        MedicalProfile medical =
                medicalProfile(
                        MedicalCondition.LIVER_DISEASE
                );

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        medical
                );

        assertTrue(
                result.requiresClinicalReview()
        );
    }


    @Test
    void shouldRequireClinicalReviewForCeliacDisease() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        MedicalProfile medical =
                medicalProfile(
                        MedicalCondition.CELIAC
                );

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        medical
                );

        assertTrue(
                result.requiresClinicalReview()
        );
    }


    @Test
    void shouldNotFailWhenMedicalProfileIsNull() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        CalorieSafetyConstraints result =
                engine.resolve(
                        personalProfile,
                        goal,
                        null
                );

        assertNotNull(result);

        assertFalse(
                result.requiresClinicalReview()
        );
    }


    @Test
    void shouldRejectMissingPersonalProfile() {

        FitnessGoalProfile goal =
                fitnessGoal();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        engine.resolve(
                                null,
                                goal,
                                null
                        )
        );
    }


    @Test
    void shouldRejectMissingWeight() {

        PersonalProfile personalProfile =
                personalProfile(
                        null,
                        172.0,
                        Gender.MALE
                );

        FitnessGoalProfile goal =
                fitnessGoal();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        engine.resolve(
                                personalProfile,
                                goal,
                                null
                        )
        );
    }


    @Test
    void shouldRejectMissingFitnessGoal() {

        PersonalProfile personalProfile =
                personalProfile(
                        78.0,
                        172.0,
                        Gender.MALE
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        engine.resolve(
                                personalProfile,
                                null,
                                null
                        )
        );
    }


    // ---------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------

    private PersonalProfile personalProfile(
            Double weight,
            Double height,
            Gender gender
    ) {

        return PersonalProfile.builder()
                .weightKg(
                        weight == null
                                ? null
                                : BigDecimal.valueOf(weight)
                )
                .heightCm(height)
                .gender(gender)
                .dateOfBirth(
                        LocalDate.of(
                                2001,
                                5,
                                15
                        )
                )
                .build();
    }


    private FitnessGoalProfile fitnessGoal() {

        return FitnessGoalProfile.builder()
                .primaryGoal(Goal.FAT_LOSS)
                .targetWeightKg(68.0)
                .targetDate(
                        LocalDate.of(
                                2027,
                                1,
                                1
                        )
                )
                .weeklyWeightChangeKg(
                        0.5
                )
                .build();
    }


    private MedicalProfile medicalProfile(
            MedicalCondition... conditions
    ) {

        return MedicalProfile.builder()
                .medicalConditions(
                        Set.of(conditions)
                )
                .build();
    }


    private MedicalProfile medicalProfile() {

        return MedicalProfile.builder()
                .medicalConditions(
                        Set.of()
                )
                .build();
    }
}
