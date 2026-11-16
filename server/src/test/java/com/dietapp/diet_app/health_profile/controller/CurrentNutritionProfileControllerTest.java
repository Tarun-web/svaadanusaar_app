package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.dto.response.CurrentNutritionProfileResponse;
import com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile.CurrentNutritionProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises real JSON deserialisation and bean validation, which is the only
 * place "field omitted" can be told apart from "field explicitly null".
 */
class CurrentNutritionProfileControllerTest {

    private static final String URL = "/api/v1/health-profile/current-nutrition";

    private CurrentNutritionProfileService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CurrentNutritionProfileService.class);
        when(service.saveOrUpdate(any())).thenReturn(
                CurrentNutritionProfileResponse.builder().currentProteinGrams(new BigDecimal("72.5")).build());
        mvc = MockMvcBuilders.standaloneSetup(new CurrentNutritionProfileController(service)).build();
    }

    private CurrentNutritionProfileRequest capturedRequest() {
        ArgumentCaptor<CurrentNutritionProfileRequest> captor =
                ArgumentCaptor.forClass(CurrentNutritionProfileRequest.class);
        verify(service).saveOrUpdate(captor.capture());
        return captor.getValue();
    }

    private void putOk(String body) throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // ── mapping of the three JSON states ─────────────────────────────

    @Test
    void numericValue_isSupplied() throws Exception {
        putOk("{\"currentProteinGrams\": 72.5}");

        CurrentNutritionProfileRequest r = capturedRequest();
        assertThat(r.suppliesCurrentProteinGrams()).isTrue();
        assertThat(r.getCurrentProteinGrams()).isEqualByComparingTo("72.5");
    }

    @Test
    void omittedField_isNotSupplied() throws Exception {
        putOk("{}");

        CurrentNutritionProfileRequest r = capturedRequest();
        assertThat(r.suppliesCurrentProteinGrams()).isFalse();
        assertThat(r.getCurrentProteinGrams()).isNull();
    }

    @Test
    void explicitNull_isSuppliedAsNull() throws Exception {
        putOk("{\"currentProteinGrams\": null}");

        CurrentNutritionProfileRequest r = capturedRequest();
        assertThat(r.suppliesCurrentProteinGrams()).isTrue();
        assertThat(r.getCurrentProteinGrams()).isNull();
    }

    @Test
    void clientCannotForgeTheSuppliedFlag() throws Exception {
        putOk("{\"currentProteinGramsSupplied\": true}");

        assertThat(capturedRequest().suppliesCurrentProteinGrams()).isFalse();
    }

    // ── validation ───────────────────────────────────────────────────

    @Test
    void negativeValue_isRejected() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": -1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void tooManyDecimalPlaces_isRejected() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 70.123}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void valueExceedingColumnPrecision_isRejected() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 1000000}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void zeroAndHighButStorableValues_areAccepted() throws Exception {
        putOk("{\"currentProteinGrams\": 0}");
        putOk("{\"currentProteinGrams\": 999999.99}");

        verify(service, times(2)).saveOrUpdate(any());
    }

    @Test
    void nonNumericValue_isRejected() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": \"lots\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ── response ─────────────────────────────────────────────────────

    @Test
    void put_returnsStoredValue() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 72.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(72.5));
    }

    @Test
    void get_returnsCurrentValue() throws Exception {
        when(service.getMyProfile()).thenReturn(
                CurrentNutritionProfileResponse.builder().currentProteinGrams(new BigDecimal("64")).build());

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(64));
    }
}
