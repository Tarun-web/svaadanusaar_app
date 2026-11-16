package com.dietapp.diet_app.health_profile.controller;

import com.dietapp.diet_app.auth.filter.JwtFilter;
import com.dietapp.diet_app.common.config.SecurityConfig;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.common.security.JsonAuthenticationEntryPoint;
import com.dietapp.diet_app.common.service.TokenBlacklistService;
import com.dietapp.diet_app.common.util.JwtUtil;
import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.mapper.CurrentNutritionProfileMapperImpl;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
import com.dietapp.diet_app.health_profile.service.CurrentNutritionProfile.CurrentNutritionProfileServiceImpl;
import com.dietapp.diet_app.health_profile.service.HealthProfileContext.HealthProfileContextServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@code /api/v1/health-profile/current-nutrition} through the REAL Spring
 * Security filter chain ({@link SecurityConfig} + {@link JwtFilter}), the real
 * {@link JwtUtil}, the real {@link AuthenticationFacade}, bean validation, the
 * application's auto-configured Jackson, the real exception handler, the real service
 * and the real MapStruct mapper.
 *
 * <p>Only the persistence boundary ({@link HealthProfileRepository}) and the token
 * blacklist store are mocked, so no database is touched.</p>
 */
@WebMvcTest(controllers = CurrentNutritionProfileController.class)
@Import({
        SecurityConfig.class,
        JwtFilter.class,
        JsonAuthenticationEntryPoint.class,
        JwtUtil.class,
        AuthenticationFacade.class,
        HealthProfileContextServiceImpl.class,
        CurrentNutritionProfileServiceImpl.class,
        CurrentNutritionProfileMapperImpl.class
})
@TestPropertySource(properties = {
        // Test-only signing key: the real application secret is never used here.
        "app.jwt.secret=test-only-signing-key-for-security-integration-tests-0123456789abcdef",
        "app.jwt.expiry=3600000"
})
class CurrentNutritionProfileSecurityIntegrationTest {

    private static final String URL = "/api/v1/health-profile/current-nutrition";

    private static final String AUTH_REQUIRED_MESSAGE = "Authentication is required to access this resource.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.jwt.secret}")
    private String secret;

    @MockitoBean
    private HealthProfileRepository healthProfileRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private UUID userA;
    private UUID userB;
    private HealthProfile profileA;
    private HealthProfile profileB;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        profileA = profileWithIntake("80.00");
        profileB = profileWithIntake("150.00");

        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(profileA));
        when(healthProfileRepository.findByUserId(userB)).thenReturn(Optional.of(profileB));
        when(healthProfileRepository.save(any(HealthProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Autowired
    private org.springframework.context.ApplicationContext context;

    // ── environment sanity ───────────────────────────────────────────

    @Test
    void usesTheApplicationsAutoConfiguredJacksonMapper_andNoDatabase() {
        // Boot 4 auto-configures Jackson 3 (tools.jackson); documents which mapper serves the API.
        assertThat(context.getBeansOfType(tools.jackson.databind.json.JsonMapper.class)).isNotEmpty();
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(jakarta.persistence.EntityManagerFactory.class)).isEmpty();
        // The real security chain is in place.
        assertThat(context.getBeansOfType(org.springframework.security.web.SecurityFilterChain.class)).hasSize(1);
    }
    // ── helpers ──────────────────────────────────────────────────────

    private static HealthProfile profileWithIntake(String grams) {
        HealthProfile profile = HealthProfile.builder().build();
        if (grams != null) {
            profile.setCurrentNutritionProfile(
                    CurrentNutritionProfile.builder().currentProteinGrams(new BigDecimal(grams)).build());
        }
        return profile;
    }

    private static BigDecimal intakeOf(HealthProfile profile) {
        return profile.getCurrentNutritionProfile() == null
                ? null
                : profile.getCurrentNutritionProfile().getCurrentProteinGrams();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenFor(UUID userId) {
        return jwtUtil.generateToken(userId);
    }

    private String expiredTokenFor(UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date(now.getTime() - 7_200_000))
                .setExpiration(new Date(now.getTime() - 3_600_000))
                .signWith(signingKey())
                .compact();
    }

    private String tokenSignedWithOtherKey(UUID userId) {
        Key other = Keys.hmacShaKeyFor(
                "a-completely-different-signing-key-that-is-long-enough-for-hs256-0123456789"
                        .getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(other)
                .compact();
    }

    private String unsignedTokenFor(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .compact();
    }

    private static MockHttpServletRequestBuilder withBearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder putAs(UUID user, String body) {
        return withBearer(put(URL), tokenFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder getAs(UUID user) {
        return withBearer(get(URL), tokenFor(user));
    }

    private static String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    // ── unauthenticated ──────────────────────────────────────────────

    @Test
    void unauthenticatedGet_returns401_andNothingIsRead() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED_MESSAGE));

        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void unauthenticatedPut_returns401_andNothingIsWritten() throws Exception {
        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED_MESSAGE));

        verifyNoInteractions(healthProfileRepository);
        assertThat(intakeOf(profileA)).isEqualByComparingTo("80.00");
    }

    @Test
    void unauthenticatedRequest_challengesWithBearerAndNothingElse() throws Exception {
        MvcResult result = mvc.perform(get(URL)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(result.getResponse().getRedirectedUrl()).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isNull();
    }

    @Test
    void unauthenticatedUnsupportedMethod_isRejectedBeforeRouting() throws Exception {
        mvc.perform(delete(URL)).andExpect(status().isUnauthorized());
    }

    // ── invalid, expired, blacklisted, malformed credentials ─────────

    @Test
    void garbageToken_isRejected() throws Exception {
        mvc.perform(withBearer(get(URL), "not-a-jwt")).andExpect(status().isUnauthorized());
        mvc.perform(withBearer(put(URL), "not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() throws Exception {
        mvc.perform(withBearer(get(URL), tokenSignedWithOtherKey(userA))).andExpect(status().isUnauthorized());
        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void unsignedToken_isRejected() throws Exception {
        mvc.perform(withBearer(get(URL), unsignedTokenFor(userA))).andExpect(status().isUnauthorized());
        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void expiredToken_isRejected_forGetAndPut() throws Exception {
        String expired = expiredTokenFor(userA);

        mvc.perform(withBearer(get(URL), expired)).andExpect(status().isUnauthorized());
        mvc.perform(withBearer(put(URL), expired)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(healthProfileRepository);
        assertThat(intakeOf(profileA)).isEqualByComparingTo("80.00");
    }

    @Test
    void blacklistedToken_isRejected_forGetAndPut() throws Exception {
        String token = tokenFor(userA);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        mvc.perform(withBearer(get(URL), token)).andExpect(status().isUnauthorized());
        mvc.perform(withBearer(put(URL), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentProteinGrams\": 1}"))
                .andExpect(status().isUnauthorized());

        verify(tokenBlacklistService, org.mockito.Mockito.atLeastOnce()).isBlacklisted(token);
        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void blacklistOnlyAffectsThatToken() throws Exception {
        String revoked = tokenFor(userA);
        when(tokenBlacklistService.isBlacklisted(revoked)).thenReturn(true);

        mvc.perform(withBearer(get(URL), revoked)).andExpect(status().isUnauthorized());
        mvc.perform(getAs(userB)).andExpect(status().isOk());
    }

    @Test
    void wrongAuthorizationScheme_isRejected() throws Exception {
        String token = tokenFor(userA);

        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Basic " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void rejectionResponses_neverEchoTheSubmittedToken() throws Exception {
        String token = tokenSignedWithOtherKey(userA);

        MvcResult result = mvc.perform(withBearer(get(URL), token)).andReturn();

        assertThat(bodyOf(result)).doesNotContain(token);
        assertThat(result.getResponse().getHeaderNames())
                .noneMatch(name -> String.valueOf(result.getResponse().getHeader(name)).contains(token));
    }

    // ── consistent 401 contract ──────────────────────────────────────

    /** Every way of being unauthenticated, as a request builder. */
    private java.util.Map<String, MockHttpServletRequestBuilder> unauthenticatedVariants() {
        String blacklisted = tokenFor(userA);
        when(tokenBlacklistService.isBlacklisted(blacklisted)).thenReturn(true);

        java.util.Map<String, MockHttpServletRequestBuilder> variants = new java.util.LinkedHashMap<>();
        variants.put("missing", get(URL));
        variants.put("garbage", withBearer(get(URL), "not-a-jwt"));
        variants.put("empty bearer", get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer "));
        variants.put("wrong key", withBearer(get(URL), tokenSignedWithOtherKey(userA)));
        variants.put("unsigned", withBearer(get(URL), unsignedTokenFor(userA)));
        variants.put("expired", withBearer(get(URL), expiredTokenFor(userA)));
        variants.put("blacklisted", withBearer(get(URL), blacklisted));
        variants.put("wrong scheme", get(URL).header(HttpHeaders.AUTHORIZATION, "Basic " + tokenFor(userA)));
        return variants;
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\",?", "").replace(",}", "}");
    }

    @Test
    void everyKindOfAuthenticationFailure_returnsTheIdenticalSafe401() throws Exception {
        java.util.Set<String> bodies = new java.util.HashSet<>();

        for (var variant : unauthenticatedVariants().entrySet()) {
            MvcResult result = mvc.perform(variant.getValue())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value(AUTH_REQUIRED_MESSAGE))
                    .andExpect(jsonPath("$.path").value(URL))
                    .andReturn();

            assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                    .as(variant.getKey()).isEqualTo("Bearer");
            assertThat(result.getResponse().getContentType())
                    .as(variant.getKey()).startsWith(MediaType.APPLICATION_JSON_VALUE);

            bodies.add(withoutTimestamp(bodyOf(result)));
        }

        // missing, malformed, wrong-key, unsigned, expired and blacklisted are indistinguishable
        assertThat(bodies).hasSize(1);
        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void unauthenticatedResponse_usesTheStandardErrorResponseShape() throws Exception {
        MvcResult result = mvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").isString())
                .andReturn();

        // timestamp is ISO-8601 like every other ErrorResponse, produced by the same JSON mapper
        String timestamp = com.jayway.jsonpath.JsonPath.read(bodyOf(result), "$.timestamp");
        java.time.LocalDateTime.parse(timestamp);

        // only ErrorResponse fields are present
        java.util.Map<String, Object> fields = com.jayway.jsonpath.JsonPath.read(bodyOf(result), "$");
        assertThat(fields.keySet())
                .isSubsetOf("timestamp", "status", "error", "message", "path", "validationErrors");
        assertThat(fields.get("validationErrors")).isNull();
    }

    @Test
    void unauthenticatedResponse_hasTheSameShapeAsOtherErrorResponses() throws Exception {
        MvcResult unauthenticated = mvc.perform(get(URL)).andReturn();
        MvcResult badRequest = mvc.perform(putAs(userA, "{\"currentProteinGrams\": -1}")).andReturn();

        java.util.Map<String, Object> a = com.jayway.jsonpath.JsonPath.read(bodyOf(unauthenticated), "$");
        java.util.Map<String, Object> b = com.jayway.jsonpath.JsonPath.read(bodyOf(badRequest), "$");

        assertThat(a.keySet()).containsAll(java.util.List.of("timestamp", "status", "error", "message", "path"));
        assertThat(b.keySet()).containsAll(a.keySet().stream().filter(k -> !k.equals("validationErrors")).toList());
    }

    @Test
    void unauthenticatedResponse_exposesNoTokenOrValidationDetails() throws Exception {
        String secretValue = secret;
        java.util.List<String> forbidden = java.util.List.of(
                "Bearer ey", "eyJ", "JWT", "Jwt", "jwt", "signature", "Signature", "expired", "Expired",
                "malformed", "Malformed", "blacklist", "Blacklist", "io.jsonwebtoken", "Exception",
                "claims", "Claims", "com.dietapp", "org.springframework", userA.toString(), secretValue);

        for (var variant : unauthenticatedVariants().entrySet()) {
            MvcResult result = mvc.perform(variant.getValue()).andReturn();
            String body = bodyOf(result);

            for (String word : forbidden) {
                assertThat(body).as(variant.getKey() + " body must not contain " + word).doesNotContain(word);
            }
            for (String header : result.getResponse().getHeaderNames()) {
                String value = String.valueOf(result.getResponse().getHeaders(header));
                assertThat(value).as(variant.getKey() + " header " + header)
                        .doesNotContain("eyJ").doesNotContain(userA.toString()).doesNotContain(secretValue);
            }
        }
    }

    @Test
    void unauthenticatedResponse_isNeverARedirectOrLoginPage() throws Exception {
        for (var variant : unauthenticatedVariants().entrySet()) {
            MvcResult result = mvc.perform(variant.getValue()).andReturn();

            assertThat(result.getResponse().getStatus()).as(variant.getKey()).isEqualTo(401);
            assertThat(result.getResponse().getRedirectedUrl()).as(variant.getKey()).isNull();
            assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).as(variant.getKey()).isNull();
            assertThat(bodyOf(result)).as(variant.getKey()).doesNotContain("<html").doesNotContain("login");
        }
    }

    @Test
    void unauthenticatedWriteAttempts_return401ForAllMethods() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"currentProteinGrams\": 5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(healthProfileRepository);
    }

    @Test
    void unknownProtectedPath_returns401WithoutRevealingWhetherItExists() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED_MESSAGE))
                .andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"));
    }

    @Test
    void publicAuthPaths_stayOutsideTheProtectedChain() throws Exception {
        // permitAll: no 401 from the entry point (no controller here, so the app answers 404)
        MvcResult result = mvc.perform(get("/api/v1/auth/anything")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(401).isNotEqualTo(403);
    }

    // ── authenticated but not permitted ──────────────────────────────

    @Test
    void authenticatedButForbidden_returns403_notThe401() throws Exception {
        MvcResult result = mvc.perform(withBearer(get("/test/admin-only"), tokenFor(userA)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(bodyOf(result)).doesNotContain(AUTH_REQUIRED_MESSAGE);
    }

    @Test
    void sameForbiddenEndpoint_withoutToken_isStill401() throws Exception {
        mvc.perform(get("/test/admin-only"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED_MESSAGE));
    }
    // ── valid credentials ────────────────────────────────────────────

    @Test
    void validToken_allowsGet() throws Exception {
        mvc.perform(getAs(userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(80.0));
    }

    @Test
    void validToken_allowsPut() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 95.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(95.5));
    }

    // ── cross-user isolation ─────────────────────────────────────────

    @Test
    void get_returnsOnlyTheCallersOwnValue() throws Exception {
        mvc.perform(getAs(userA)).andExpect(jsonPath("$.currentProteinGrams").value(80.0));
        mvc.perform(getAs(userB)).andExpect(jsonPath("$.currentProteinGrams").value(150.0));
    }

    @Test
    void put_changesOnlyTheCallersOwnProfile() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 91}")).andExpect(status().isOk());

        assertThat(intakeOf(profileA)).isEqualByComparingTo("91");
        assertThat(intakeOf(profileB)).isEqualByComparingTo("150.00");

        verify(healthProfileRepository).findByUserId(userA);
        verify(healthProfileRepository, never()).findByUserId(userB);
        verify(healthProfileRepository).save(profileA);
        verify(healthProfileRepository, never()).save(profileB);
    }

    @Test
    void put_cannotTargetAnotherUserThroughTheRequestBody() throws Exception {
        String body = "{\"currentProteinGrams\": 11,"
                + "\"userId\": \"" + userB + "\","
                + "\"healthProfileId\": \"" + UUID.randomUUID() + "\"}";

        mvc.perform(putAs(userA, body)).andExpect(status().isOk());

        assertThat(intakeOf(profileA)).isEqualByComparingTo("11");
        assertThat(intakeOf(profileB)).isEqualByComparingTo("150.00");
        verify(healthProfileRepository, never()).findByUserId(userB);
    }

    @Test
    void put_cannotTargetAnotherUserThroughQueryParameterOrHeader() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 12}")
                        .param("userId", userB.toString())
                        .header("X-User-Id", userB.toString()))
                .andExpect(status().isOk());

        assertThat(intakeOf(profileA)).isEqualByComparingTo("12");
        assertThat(intakeOf(profileB)).isEqualByComparingTo("150.00");
        verify(healthProfileRepository, never()).findByUserId(userB);
    }

    @Test
    void userWithoutProfile_getsGeneric404_withoutLeakingIdentifiers() throws Exception {
        UUID stranger = UUID.randomUUID();

        MvcResult result = mvc.perform(getAs(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain(stranger.toString());
        verify(healthProfileRepository, never()).save(any());
    }

    // ── JSON semantics through the real Jackson configuration ───────

    @Test
    void omittedField_preservesStoredValue_andDoesNotSave() throws Exception {
        mvc.perform(putAs(userA, "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(80.0));

        assertThat(intakeOf(profileA)).isEqualByComparingTo("80.00");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void explicitNull_clearsStoredValue() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").doesNotExist());

        assertThat(intakeOf(profileA)).isNull();
        verify(healthProfileRepository).save(profileA);

        mvc.perform(getAs(userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").doesNotExist());
    }

    @Test
    void numericValue_updatesStoredValue_andIsVisibleToSubsequentGet() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 72.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(72.5));

        assertThat(intakeOf(profileA)).isEqualByComparingTo("72.5");

        mvc.perform(getAs(userA)).andExpect(jsonPath("$.currentProteinGrams").value(72.5));
    }

    @Test
    void zero_isAValueNotAnOmission() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(0));

        assertThat(intakeOf(profileA)).isEqualByComparingTo("0");
    }

    @Test
    void forgedSuppliedFlag_isIgnored() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGramsSupplied\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").value(80.0));

        assertThat(intakeOf(profileA)).isEqualByComparingTo("80.00");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void explicitNull_onProfileWithNoValue_staysNull() throws Exception {
        profileA.setCurrentNutritionProfile(null);

        mvc.perform(putAs(userA, "{\"currentProteinGrams\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinGrams").doesNotExist());

        assertThat(intakeOf(profileA)).isNull();
    }

    // ── request validation ───────────────────────────────────────────

    @Test
    void maximumStorableValue_isAccepted() throws Exception {
        mvc.perform(putAs(userA, "{\"currentProteinGrams\": 999999.99}"))
                .andExpect(status().isOk());

        assertThat(intakeOf(profileA)).isEqualByComparingTo("999999.99");
    }

    @Test
    void negativeValue_isRejectedWith400_andNotStored_andNotEchoed() throws Exception {
        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": -123.45}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.validationErrors[0].field").value("currentProteinGrams"))
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("123.45").doesNotContain("rejectedValue");
        assertThat(intakeOf(profileA)).isEqualByComparingTo("80.00");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void tooManyIntegerDigits_isRejectedWith400_andNotEchoed() throws Exception {
        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": 1234567.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("currentProteinGrams"))
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("1234567");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void tooManyDecimalPlaces_isRejectedWith400_andNotEchoed() throws Exception {
        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": 72.123}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("72.123");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void nonNumericValue_isRejectedWith400_withFixedMessage_andNotEchoed() throws Exception {
        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": \"lots-of-protein-SECRET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed."))
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("lots-of-protein-SECRET");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void malformedJson_isRejectedWith400_andNothingLeaks() throws Exception {
        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": 72.5, \"secret\": \"hunter2\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed."))
                .andReturn();

        String body = bodyOf(result);
        assertThat(body).doesNotContain("hunter2");
        assertThat(body).doesNotContain("Jackson").doesNotContain("tools.jackson")
                .doesNotContain("com.fasterxml").doesNotContain("org.springframework")
                .doesNotContain("com.dietapp").doesNotContain("JSON parse error");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void missingBody_isRejectedWith400() throws Exception {
        mvc.perform(withBearer(put(URL), tokenFor(userA)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed."));

        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void jsonArrayInsteadOfObject_isRejectedWith400() throws Exception {
        mvc.perform(putAs(userA, "[1, 2, 3]")).andExpect(status().isBadRequest());
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void unsupportedMediaType_isRejectedWith415_andBodyIsNotEchoed() throws Exception {
        MvcResult result = mvc.perform(withBearer(put(URL), tokenFor(userA))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("currentProteinGrams=SECRET-VALUE"))
                .andExpect(status().isUnsupportedMediaType())
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("SECRET-VALUE");
        verify(healthProfileRepository, never()).save(any());
    }

    @Test
    void unsupportedMethod_whenAuthenticated_returns405() throws Exception {
        mvc.perform(withBearer(delete(URL), tokenFor(userA)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("This HTTP method is not supported for this endpoint."));
    }

    // ── failures behind the web layer ────────────────────────────────

    @Test
    void optimisticLockFailure_returns409_withGenericMessage_throughRealChain() throws Exception {
        when(healthProfileRepository.save(any(HealthProfile.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(HealthProfile.class, userA));

        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": 70}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This resource was modified by another request. Please refresh and try again."))
                .andReturn();

        assertThat(bodyOf(result)).doesNotContain("HealthProfile").doesNotContain(userA.toString());
    }

    @Test
    void unexpectedPersistenceFailure_returnsGeneric500_withoutSqlOrSubmittedValue() throws Exception {
        when(healthProfileRepository.save(any(HealthProfile.class)))
                .thenThrow(new RuntimeException(
                        "could not execute statement [update health_profiles set current_protein_grams=77.7]"
                                + " jdbc:postgresql://db.internal:5432/postgres"));

        MvcResult result = mvc.perform(putAs(userA, "{\"currentProteinGrams\": 77.7}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andReturn();

        String body = bodyOf(result);
        assertThat(body).doesNotContain("health_profiles")
                .doesNotContain("jdbc:")
                .doesNotContain("db.internal")
                .doesNotContain("77.7")
                .doesNotContain("could not execute");
    }

    /** Test-only endpoint that denies every authenticated user (they hold no authorities). */
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class AccessDeniedTestConfig {

        @org.springframework.context.annotation.Bean
        AdminOnlyController adminOnlyController() {
            return new AdminOnlyController();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class AdminOnlyController {

        @org.springframework.web.bind.annotation.GetMapping("/test/admin-only")
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() {
            return "secret";
        }
    }
}