package com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.CurrentNutritionProfileMapper;
import com.dietapp.diet_app.health_profile.mapper.CurrentNutritionProfileMapperImpl;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service-level behaviour of the user-reported protein intake write path.
 * Uses the real MapStruct mapper; only the repository and the JWT-based
 * profile lookup are mocked.
 */
class CurrentNutritionProfileServiceImplTest {

    private HealthProfileContextService context;
    private HealthProfileRepository repository;
    private CurrentNutritionProfileMapper mapper;
    private CurrentNutritionProfileServiceImpl service;

    private HealthProfile profile;

    @BeforeEach
    void setUp() {
        context = mock(HealthProfileContextService.class);
        repository = mock(HealthProfileRepository.class);
        mapper = new CurrentNutritionProfileMapperImpl();
        service = new CurrentNutritionProfileServiceImpl(context, repository, mapper);

        profile = HealthProfile.builder().build();
        when(context.getCurrentUserHealthProfile()).thenReturn(profile);
        when(repository.save(any(HealthProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CurrentNutritionProfileRequest supplied(String grams) {
        CurrentNutritionProfileRequest r = new CurrentNutritionProfileRequest();
        r.setCurrentProteinGrams(grams == null ? null : new BigDecimal(grams));
        return r;
    }

    private void givenStoredIntake(String grams) {
        profile.setCurrentNutritionProfile(
                CurrentNutritionProfile.builder().currentProteinGrams(new BigDecimal(grams)).build());
    }

    // ── saving ───────────────────────────────────────────────────────

    @Test
    void savesSuppliedIntake() {
        CurrentNutritionProfileResponse response = service.saveOrUpdate(supplied("72.50"));

        assertThat(profile.getCurrentNutritionProfile()).isNotNull();
        assertThat(profile.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("72.50");
        assertThat(response.getCurrentProteinGrams()).isEqualByComparingTo("72.50");
        verify(repository).save(profile);
    }

    @Test
    void zeroIsStoredAsZero_notTreatedAsMissing() {
        service.saveOrUpdate(supplied("0"));

        assertThat(profile.getCurrentNutritionProfile()).isNotNull();
        assertThat(profile.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("0");
    }

    // ── updating ─────────────────────────────────────────────────────

    @Test
    void updatesExistingIntake() {
        givenStoredIntake("50");

        CurrentNutritionProfileResponse response = service.saveOrUpdate(supplied("85.25"));

        assertThat(profile.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("85.25");
        assertThat(response.getCurrentProteinGrams()).isEqualByComparingTo("85.25");
    }

    // ── omitting ─────────────────────────────────────────────────────

    @Test
    void omittedField_doesNotClearExistingIntake_andDoesNotSave() {
        givenStoredIntake("50");

        // field never set, as when the JSON body is {}
        CurrentNutritionProfileResponse response =
                service.saveOrUpdate(new CurrentNutritionProfileRequest());

        assertThat(profile.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("50");
        assertThat(response.getCurrentProteinGrams()).isEqualByComparingTo("50");
        verify(repository, never()).save(any());
    }

    @Test
    void omittedField_onProfileWithNoIntake_staysNull() {
        CurrentNutritionProfileResponse response =
                service.saveOrUpdate(new CurrentNutritionProfileRequest());

        assertThat(profile.getCurrentNutritionProfile()).isNull();
        assertThat(response.getCurrentProteinGrams()).isNull();
        verify(repository, never()).save(any());
    }

    // ── explicit clearing ────────────────────────────────────────────

    @Test
    void explicitNull_clearsStoredIntake() {
        givenStoredIntake("50");

        CurrentNutritionProfileResponse response = service.saveOrUpdate(supplied(null));

        assertThat(profile.getCurrentNutritionProfile()).isNull();
        assertThat(response.getCurrentProteinGrams()).isNull();
        verify(repository).save(profile);
    }

    // ── reading ──────────────────────────────────────────────────────

    @Test
    void getMyProfile_returnsStoredIntake() {
        givenStoredIntake("64");

        assertThat(service.getMyProfile().getCurrentProteinGrams()).isEqualByComparingTo("64");
    }

    @Test
    void getMyProfile_withNullEmbedded_returnsNullIntake() {
        assertThat(service.getMyProfile().getCurrentProteinGrams()).isNull();
    }

    // ── ownership ────────────────────────────────────────────────────

    @Test
    void alwaysResolvesTheProfileFromTheAuthenticatedContext() {
        service.saveOrUpdate(supplied("70"));
        service.getMyProfile();

        verify(context, times(2)).getCurrentUserHealthProfile();
        // the service has no way to address another user's profile
        verifyNoMoreInteractions(context);
        verify(repository, never()).findById(any());
        verify(repository, never()).findByUserId(any());
    }
}
