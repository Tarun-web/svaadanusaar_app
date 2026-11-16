package com.dietapp.diet_app.common.exception;

import com.dietapp.diet_app.health_profile.controller.CurrentNutritionProfileController;
import com.dietapp.diet_app.health_profile.dto.request.CurrentNutritionProfileRequest;
import com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile.CurrentNutritionProfileService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the optimistic-lock handler of {@link RestExceptionHandler} and that the
 * pre-existing handlers keep their behaviour.
 */
class RestExceptionHandlerOptimisticLockTest {

    private static final String GENERIC_MESSAGE =
            "This resource was modified by another request. Please refresh and try again.";

    private static final String UNEXPECTED_MESSAGE =
            "An unexpected error occurred. Please try again later.";

    /** Details that must never reach a client. */
    private static final String LEAKY_ENTITY = "com.dietapp.diet_app.health_profile.entity.HealthProfile";
    private static final String LEAKY_SQL =
            "update health_profiles set version=? where id=? and version=?";
    private static final String LEAKY_ID = "3f2c9b1e-0000-4000-8000-123456789abc";

    /** Throws whatever it is told to, so each handler can be exercised in isolation. */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/optimistic-object")
        public String objectOptimistic() {
            throw new ObjectOptimisticLockingFailureException(
                    "Row was updated or deleted by another transaction: " + LEAKY_SQL
                            + " [" + LEAKY_ENTITY + "#" + LEAKY_ID + "]",
                    new RuntimeException("cause: " + LEAKY_SQL));
        }

        @GetMapping("/test/optimistic-object-by-class")
        public String objectOptimisticByClass() {
            throw new ObjectOptimisticLockingFailureException(
                    com.dietapp.diet_app.health_profile.entity.HealthProfile.class,
                    java.util.UUID.fromString(LEAKY_ID));
        }

        @GetMapping("/test/optimistic-spring")
        public String springOptimistic() {
            throw new OptimisticLockingFailureException("spring-level: " + LEAKY_SQL);
        }

        @GetMapping("/test/optimistic-jpa")
        public String jpaOptimistic() {
            throw new OptimisticLockException("jpa-level: " + LEAKY_ENTITY + " " + LEAKY_SQL);
        }

        @GetMapping("/test/business")
        public String business() {
            throw new BusinessException("business rule broken");
        }

        @GetMapping("/test/not-found")
        public String notFound() {
            throw new ResourceNotFoundException("thing missing");
        }

        @GetMapping("/test/already-exists")
        public String alreadyExists() {
            throw new ResourceAlreadyExistsException("thing exists");
        }

        @GetMapping("/test/email-taken")
        public String emailTaken() {
            throw new EmailAlreadyTakenException("email in use");
        }

        @GetMapping("/test/invalid-otp")
        public String invalidOtp() {
            throw new InvalidOtpException("bad otp");
        }

        @GetMapping("/test/user-not-found")
        public String userNotFound() {
            throw new UserNotFoundException("no such user");
        }

        @GetMapping("/test/incomplete-profile")
        public String incompleteProfile() {
            throw new IncompleteProfileException(
                    "Height is required. internal-detail " + LEAKY_SQL, java.util.List.of("heightCm"));
        }

        @GetMapping("/test/unexpected")
        public String unexpected() {
            throw new IllegalStateException("some internal failure");
        }

        @GetMapping("/test/unexpected-leaky")
        public String unexpectedLeaky() {
            throw new RuntimeException(
                    "could not execute statement [" + LEAKY_SQL + "] on " + LEAKY_ENTITY
                            + " id=" + LEAKY_ID + " jdbc:postgresql://db.internal:5432/postgres",
                    new java.sql.SQLException("FATAL: password authentication failed for user \"app\""));
        }

        @GetMapping("/test/unexpected-null-message")
        public String unexpectedNullMessage() {
            throw new NullPointerException();
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private String body(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andReturn();
        return result.getResponse().getContentAsString();
    }

    // ── optimistic-lock handling ─────────────────────────────────────

    @Test
    void objectOptimisticLockingFailure_returns409WithGenericMessage() throws Exception {
        mvc.perform(get("/test/optimistic-object"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/optimistic-object"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void objectOptimisticLockingFailure_builtFromClassAndId_returns409() throws Exception {
        mvc.perform(get("/test/optimistic-object-by-class"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void springOptimisticLockingFailure_returns409WithGenericMessage() throws Exception {
        mvc.perform(get("/test/optimistic-spring"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void jpaOptimisticLockException_returns409WithGenericMessage() throws Exception {
        mvc.perform(get("/test/optimistic-jpa"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void optimisticLockResponses_doNotExposeInternalDetails() throws Exception {
        for (String url : new String[]{
                "/test/optimistic-object",
                "/test/optimistic-object-by-class",
                "/test/optimistic-spring",
                "/test/optimistic-jpa"}) {

            String json = body(url);

            assertThat(json).as(url).contains(GENERIC_MESSAGE);
            assertThat(json).as(url)
                    .doesNotContain(LEAKY_SQL)
                    .doesNotContain(LEAKY_ENTITY)
                    .doesNotContain(LEAKY_ID)
                    .doesNotContain("HealthProfile")
                    .doesNotContain("health_profiles")
                    .doesNotContain("another transaction")
                    .doesNotContain("cause:")
                    .doesNotContain("Exception")
                    .doesNotContain("stackTrace")
                    .doesNotContain("trace");
        }
    }

    @Test
    void optimisticLockResponse_hasNoValidationErrors() throws Exception {
        mvc.perform(get("/test/optimistic-object"))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    // ── end to end through the real nutrition controller ─────────────

    @Test
    void nutritionEndpoint_whenServiceHitsOptimisticLock_returns409() throws Exception {
        CurrentNutritionProfileService service = mock(CurrentNutritionProfileService.class);
        when(service.saveOrUpdate(any(CurrentNutritionProfileRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(
                        com.dietapp.diet_app.health_profile.entity.HealthProfile.class,
                        java.util.UUID.fromString(LEAKY_ID)));

        MockMvc nutritionMvc = MockMvcBuilders
                .standaloneSetup(new CurrentNutritionProfileController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        MvcResult result = nutritionMvc.perform(
                        put("/api/v1/health-profile/current-nutrition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentProteinGrams\": 72.5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("HealthProfile")
                .doesNotContain(LEAKY_ID);
    }

    // ── existing handlers are unaffected ─────────────────────────────

    @Test
    void businessException_stillReturns400WithItsMessage() throws Exception {
        mvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("business rule broken"));
    }

    @Test
    void resourceNotFound_stillReturns404WithItsMessage() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("thing missing"));
    }

    @Test
    void userNotFound_stillReturns404WithItsMessage() throws Exception {
        mvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no such user"));
    }

    @Test
    void resourceAlreadyExists_stillReturns409WithItsMessage() throws Exception {
        mvc.perform(get("/test/already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("thing exists"));
    }

    @Test
    void emailAlreadyTaken_stillReturns409WithItsMessage() throws Exception {
        mvc.perform(get("/test/email-taken"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("email in use"));
    }

    @Test
    void invalidOtp_stillReturns401WithItsMessage() throws Exception {
        mvc.perform(get("/test/invalid-otp"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("bad otp"));
    }

    @Test
    void unexpectedException_stillGoesThroughCatchAllAs500() throws Exception {
        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value(UNEXPECTED_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    void unexpectedException_withNullMessage_returnsSameGeneric500() throws Exception {
        mvc.perform(get("/test/unexpected-null-message"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(UNEXPECTED_MESSAGE));
    }

    // ── incomplete profile ───────────────────────────────────────────

    @Test
    void incompleteProfile_returns422WithFixedMessageAndFieldNames() throws Exception {
        mvc.perform(get("/test/incomplete-profile"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Content"))
                .andExpect(jsonPath("$.message").value(RestExceptionHandler.INCOMPLETE_PROFILE_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/incomplete-profile"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors.length()").value(1))
                .andExpect(jsonPath("$.validationErrors[0].field").value("heightCm"))
                .andExpect(jsonPath("$.validationErrors[0].message")
                        .value(RestExceptionHandler.INCOMPLETE_PROFILE_FIELD_MESSAGE));
    }

    @Test
    void incompleteProfile_doesNotEchoTheExceptionMessage() throws Exception {
        assertThat(body("/test/incomplete-profile"))
                .doesNotContain("internal-detail", "Height is required", LEAKY_SQL,
                        "IllegalStateException", "IncompleteProfileException", "com.dietapp");
    }

    @Test
    void plainIllegalStateException_isStillA500_notMappedAsIncompleteProfile() throws Exception {
        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(UNEXPECTED_MESSAGE));
    }

    @Test
    void unexpectedException_doesNotExposeInternalDetails() throws Exception {
        for (String url : new String[]{
                "/test/unexpected",
                "/test/unexpected-leaky",
                "/test/unexpected-null-message"}) {

            String json = body(url);

            assertThat(json).as(url).contains(UNEXPECTED_MESSAGE);
            assertThat(json).as(url)
                    .doesNotContain("some internal failure")
                    .doesNotContain("could not execute statement")
                    .doesNotContain(LEAKY_SQL)
                    .doesNotContain(LEAKY_ENTITY)
                    .doesNotContain(LEAKY_ID)
                    .doesNotContain("jdbc:")
                    .doesNotContain("db.internal")
                    .doesNotContain("password")
                    .doesNotContain("SQLException")
                    .doesNotContain("RuntimeException")
                    .doesNotContain("NullPointerException")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("stackTrace")
                    .doesNotContain("\tat ")
                    .doesNotContain("com.dietapp");
        }
    }

    @Test
    void unexpectedException_isLoggedServerSide_withoutWritingToStandardError() throws Exception {
        java.io.PrintStream originalErr = System.err;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(captured, true));
        try {
            mvc.perform(get("/test/unexpected")).andReturn();
        } finally {
            System.setErr(originalErr);
        }

        // printStackTrace() would have written the trace to System.err.
        assertThat(captured.toString()).doesNotContain("IllegalStateException");
    }

    @Test
    void genericConflictMessage_isNotUsedByOtherConflictHandlers() throws Exception {
        assertThat(body("/test/already-exists")).doesNotContain(GENERIC_MESSAGE);
        assertThat(body("/test/email-taken")).doesNotContain(GENERIC_MESSAGE);
    }
}
