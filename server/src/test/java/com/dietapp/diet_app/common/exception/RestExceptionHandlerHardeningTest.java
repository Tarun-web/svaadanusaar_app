package com.dietapp.diet_app.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hardening tests for {@link RestExceptionHandler}: framework exceptions get fixed safe
 * messages, validation responses never echo rejected input, and previously established
 * behaviour is unchanged.
 */
class RestExceptionHandlerHardeningTest {

    /** Values a client might submit that must never be echoed back. */
    private static final String SECRET_PASSWORD = "hunter2-correct-horse";
    private static final String SECRET_TOKEN = "eyJhbGciOiJIUzI1NiJ9.SECRET.PAYLOAD";

    private static final String VALIDATION_MESSAGE = "Validation failed.";
    private static final String MALFORMED_MESSAGE = "The request body is missing or malformed.";
    private static final String METHOD_MESSAGE = "This HTTP method is not supported for this endpoint.";
    private static final String MEDIA_TYPE_MESSAGE =
            "The request content type is not supported for this endpoint.";
    private static final String PARAMETER_MESSAGE = "A required request parameter is missing.";
    private static final String UNEXPECTED_MESSAGE =
            "An unexpected error occurred. Please try again later.";
    private static final String OPTIMISTIC_MESSAGE =
            "This resource was modified by another request. Please refresh and try again.";

    /** Details that must not reach a client. */
    private static final String[] FRAMEWORK_LEAKS = {
            "JSON parse error", "Unexpected character", "Unrecognized token",
            "com.fasterxml", "tools.jackson", "Jackson", "org.springframework",
            "HttpMessageNotReadable", "HttpRequestMethodNotSupported",
            "HttpMediaTypeNotSupported", "MissingServletRequestParameter",
            "Request method", "Content-Type 'text/plain'", "stackTrace", "com.dietapp"
    };

    static class LoginRequest {
        @NotBlank(message = "Email is required.")
        private String email;

        @Size(min = 30, message = "Password is too short.")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /** Method used only to obtain a realistic method-validation property path. */
    @SuppressWarnings("unused")
    public void login(@Valid LoginRequest request) {
    }

    @RestController
    static class TestController {

        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String body(@RequestBody @Valid LoginRequest request) {
            return "ok";
        }

        @GetMapping("/test/param")
        public String param(@RequestParam("q") String q) {
            return q;
        }

        @GetMapping("/test/get-only")
        public String getOnly() {
            return "ok";
        }

        @GetMapping("/test/constraint")
        public String constraint() throws Exception {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

            LoginRequest bad = new LoginRequest();
            bad.setEmail("");
            bad.setPassword(SECRET_PASSWORD);

            Method method = RestExceptionHandlerHardeningTest.class.getMethod("login", LoginRequest.class);
            Set<ConstraintViolation<RestExceptionHandlerHardeningTest>> violations =
                    validator.forExecutables().validateParameters(
                            new RestExceptionHandlerHardeningTest(), method, new Object[]{bad});

            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/test/constraint-empty")
        public String constraintEmpty() {
            throw new ConstraintViolationException("internal.Class.method.arg0: leaked", null);
        }

        @GetMapping("/test/business")
        public String business() {
            throw new BusinessException("Sport type is required when activity type is SPORTS.");
        }

        @GetMapping("/test/not-found")
        public String notFound() {
            throw new ResourceNotFoundException("Health Profile not found.");
        }

        @GetMapping("/test/already-exists")
        public String alreadyExists() {
            throw new ResourceAlreadyExistsException("Health Profile already exists for this user.");
        }

        @GetMapping("/test/email-taken")
        public String emailTaken() {
            throw new EmailAlreadyTakenException("Email already in use");
        }

        @GetMapping("/test/invalid-otp")
        public String invalidOtp() {
            throw new InvalidOtpException("Invalid OTP");
        }

        @GetMapping("/test/user-not-found")
        public String userNotFound() {
            throw new UserNotFoundException("User not found");
        }

        @GetMapping("/test/optimistic")
        public String optimistic() {
            throw new OptimisticLockingFailureException("update health_profiles set version=?");
        }

        @GetMapping("/test/unexpected")
        public String unexpected() {
            throw new IllegalStateException("could not execute statement [select * from users]");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private String json(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private void assertNoFrameworkLeaks(String json) {
        for (String leak : FRAMEWORK_LEAKS) {
            assertThat(json).as("leaked: " + leak).doesNotContain(leak);
        }
    }

    // ── Part B: framework exceptions ─────────────────────────────────

    @Test
    void malformedJson_returns400WithFixedMessage() throws Exception {
        MvcResult result = mvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a@b.com\", \"password\": \"" + SECRET_PASSWORD + "\" "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(MALFORMED_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/body"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        String body = json(result);
        assertThat(body).doesNotContain(SECRET_PASSWORD);
        assertNoFrameworkLeaks(body);
    }

    @Test
    void wrongFieldType_returns400WithFixedMessage() throws Exception {
        MvcResult result = mvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": {\"nested\": \"" + SECRET_TOKEN + "\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MALFORMED_MESSAGE))
                .andReturn();

        assertThat(json(result)).doesNotContain(SECRET_TOKEN);
        assertNoFrameworkLeaks(json(result));
    }

    @Test
    void missingBody_returns400WithFixedMessage() throws Exception {
        MvcResult result = mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MALFORMED_MESSAGE))
                .andReturn();

        assertNoFrameworkLeaks(json(result));
    }

    @Test
    void unsupportedHttpMethod_returns405WithFixedMessageAndAllowHeader() throws Exception {
        MvcResult result = mvc.perform(delete("/test/get-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value(METHOD_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/get-only"))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.ALLOW)).contains("GET");
        assertNoFrameworkLeaks(json(result));
        assertThat(json(result)).doesNotContain("DELETE");
    }

    @Test
    void unsupportedMediaType_returns415WithFixedMessageAndAcceptHeader() throws Exception {
        MvcResult result = mvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(SECRET_PASSWORD))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message").value(MEDIA_TYPE_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/body"))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.ACCEPT)).contains("application/json");
        assertThat(json(result)).doesNotContain(SECRET_PASSWORD);
        assertNoFrameworkLeaks(json(result));
    }

    @Test
    void missingRequiredParameter_returns400WithFixedMessage() throws Exception {
        MvcResult result = mvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(PARAMETER_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/param"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("q"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("This parameter is required."))
                .andReturn();

        assertNoFrameworkLeaks(json(result));
        // The framework message ("Required request parameter 'q' for method parameter type String ...")
        assertThat(json(result)).doesNotContain("String").doesNotContain("method parameter");
    }

    @Test
    void presentParameter_isNotAffected() throws Exception {
        mvc.perform(get("/test/param").param("q", "x")).andExpect(status().isOk());
    }

    // ── Part A: validation never exposes rejected values ─────────────

    @Test
    void methodArgumentValidation_doesNotExposeRejectedValues() throws Exception {
        MvcResult result = mvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"\", \"password\": \"" + SECRET_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(jsonPath("$.validationErrors.length()").value(2))
                .andReturn();

        String body = json(result);
        assertThat(body)
                .doesNotContain(SECRET_PASSWORD)
                .doesNotContain("rejectedValue");
        // field names and user-facing messages are preserved
        assertThat(body)
                .contains("\"field\":\"password\"")
                .contains("Password is too short.")
                .contains("\"field\":\"email\"")
                .contains("Email is required.");
    }

    @Test
    void constraintViolation_returns400WithFieldErrorsAndNoInternalPathOrValue() throws Exception {
        MvcResult result = mvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(jsonPath("$.path").value("/test/constraint"))
                .andReturn();

        String body = json(result);
        assertThat(body)
                .doesNotContain(SECRET_PASSWORD)
                .doesNotContain("rejectedValue")
                // ConstraintViolationException#getMessage() contains the full path, e.g. "login.request.password"
                .doesNotContain("login")
                .doesNotContain("arg0")
                .doesNotContain("request.")
                .doesNotContain("RestExceptionHandlerHardeningTest")
                .contains("\"field\":\"password\"")
                .contains("Password is too short.");
    }

    @Test
    void constraintViolation_withoutViolations_doesNotFallBackToExceptionMessage() throws Exception {
        MvcResult result = mvc.perform(get("/test/constraint-empty"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andReturn();

        assertThat(json(result)).doesNotContain("internal.Class").doesNotContain("leaked");
    }

    // ── Part A: established contracts are unchanged ──────────────────

    @Test
    void businessException_stillReturns400WithItsUserFacingMessage() throws Exception {
        mvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Sport type is required when activity type is SPORTS."))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    void resourceNotFound_stillReturns404WithItsMessage() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Health Profile not found."));
    }

    @Test
    void userNotFound_stillReturns404WithItsMessage() throws Exception {
        mvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void resourceAlreadyExists_stillReturns409WithItsMessage() throws Exception {
        mvc.perform(get("/test/already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Health Profile already exists for this user."));
    }

    @Test
    void emailAlreadyTaken_stillReturns409WithItsMessage() throws Exception {
        mvc.perform(get("/test/email-taken"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void invalidOtp_stillReturns401WithItsMessage() throws Exception {
        mvc.perform(get("/test/invalid-otp"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    // ── regression: earlier hardening is intact ──────────────────────

    @Test
    void optimisticLock_stillReturns409WithGenericMessage() throws Exception {
        MvcResult result = mvc.perform(get("/test/optimistic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(OPTIMISTIC_MESSAGE))
                .andReturn();

        assertThat(json(result)).doesNotContain("health_profiles").doesNotContain("version");
    }

    @Test
    void unexpectedException_stillReturnsGeneric500() throws Exception {
        MvcResult result = mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(UNEXPECTED_MESSAGE))
                .andReturn();

        assertThat(json(result)).doesNotContain("select").doesNotContain("could not execute");
    }

    @Test
    void frameworkExceptionsAreNoLongerReportedAs500() throws Exception {
        // Before this change all four fell into the catch-all handler.
        mvc.perform(get("/test/param")).andExpect(status().is(400));
        mvc.perform(delete("/test/get-only")).andExpect(status().is(405));
        mvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().is(415));
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().is(400));
    }
}
