package com.dietapp.diet_app.health_profile.service.ProfileCompletion;

import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Every profile-section save ends with refreshProfileCompletion, which loads and
 * re-saves the HealthProfile. Updating any other section must not clear the
 * embedded self-reported protein intake.
 */
class ProfileCompletionPreservesIntakeTest {

    private HealthProfileRepository healthProfileRepository;
    private ProfileCompletionServiceImpl service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        healthProfileRepository = mock(HealthProfileRepository.class);
        service = new ProfileCompletionServiceImpl(
                mock(PersonalProfileRepository.class),
                mock(FitnessGoalProfileRepository.class),
                mock(NutritionPreferenceProfileRepository.class),
                mock(WorkoutProfileRepository.class),
                mock(MedicalProfileRepository.class),
                mock(CookingProfileRepository.class),
                mock(LifestylePreferenceProfileRepository.class),
                mock(SupplementProfileRepository.class),
                healthProfileRepository);
    }

    @Test
    void refreshingCompletion_keepsEmbeddedProteinIntake() {
        HealthProfile profile = HealthProfile.builder()
                .currentNutritionProfile(CurrentNutritionProfile.builder()
                        .currentProteinGrams(new BigDecimal("72.50")).build())
                .build();
        when(healthProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        service.refreshProfileCompletion(id);

        verify(healthProfileRepository).save(profile);
        assertThat(profile.getCurrentNutritionProfile()).isNotNull();
        assertThat(profile.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("72.50");
    }

    @Test
    void refreshingCompletion_doesNotCreateAnEmbeddedValueFromNothing() {
        HealthProfile profile = HealthProfile.builder().build();
        when(healthProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        service.refreshProfileCompletion(id);

        assertThat(profile.getCurrentNutritionProfile()).isNull();
    }
}
