package com.dietapp.diet_app.health_profile.NutritionTarget.controller;

import com.dietapp.diet_app.auth.filter.JwtFilter;
import com.dietapp.diet_app.common.config.SecurityConfig;
import com.dietapp.diet_app.common.security.AuthenticationFacade;
import com.dietapp.diet_app.common.security.JsonAuthenticationEntryPoint;
import com.dietapp.diet_app.common.service.TokenBlacklistService;
import com.dietapp.diet_app.common.util.JwtUtil;
import com.dietapp.diet_app.health_profile.NutritionTarget.entity.ProteinAdaptationProfile;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.NutritionTargetServiceImpl;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.ProteinAdaptationService;
import com.dietapp.diet_app.health_profile.NutritionTarget.service.calculator.*;
import com.dietapp.diet_app.health_profile.entity.CurrentNutritionProfile;
import com.dietapp.diet_app.health_profile.entity.FitnessGoalProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import com.dietapp.diet_app.health_profile.entity.PersonalProfile;
import com.dietapp.diet_app.health_profile.enums.Gender;
import com.dietapp.diet_app.health_profile.enums.Goal;
import com.dietapp.diet_app.health_profile.enums.Occupation;
import com.dietapp.diet_app.health_profile.repository.HealthProfileRepository;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDate;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/nutrition-targets} through the REAL security chain, JWT
 * handling, exception handler, auto-configured Jackson, the real
 * {@link NutritionTargetServiceImpl}, {@link ProteinAdaptationService} and all
 * real calculators.
 *
 * <p>Only the two repositories and the token blacklist are mocked, so the
 * adaptation profile "table" is an in-memory map. <strong>No database is
 * touched and this is not PostgreSQL verification.</strong></p>
 */
@WebMvcTest(controllers = NutritionTargetController.class)
@Import({
        SecurityConfig.class, JwtFilter.class, JsonAuthenticationEntryPoint.class,
        JwtUtil.class, AuthenticationFacade.class, HealthProfileContextServiceImpl.class,
        NutritionTargetServiceImpl.class, ProteinAdaptationService.class,
        GoalCalorieCalculator.class, TdeeCalculator.class, BmrCalculator.class,
        ActivityLevelResolver.class, ActivityFactorStrategy.class, NeatCalculator.class,
        TefCalculator.class, CalorieSafetyEngine.class, MacroCalculator.class,
        ProteinCalculator.class, FatCalculator.class, CarbohydrateCalculator.class,
        FibreCalculator.class, WaterCalculator.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-signing-key-for-security-integration-tests-0123456789abcdef",
        "app.jwt.expiry=3600000"
})
class NutritionTargetControllerSecurityTest {

    private static final String URL = "/api/v1/nutrition-targets";
    private static final String AUTH_REQUIRED = "Authentication is required to access this resource.";

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @Value("${app.jwt.secret}") private String secret;

    @MockitoBean private HealthProfileRepository healthProfileRepository;
    @MockitoBean private ProteinAdaptationProfileRepository adaptationRepository;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;

    private final Map<HealthProfile, ProteinAdaptationProfile> adaptationTable = new IdentityHashMap<>();
    private UUID userA;
    private UUID userB;
    private HealthProfile profileA;
    private HealthProfile profileB;

    @BeforeEach
    void setUp() {
        adaptationTable.clear();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        profileA = healthProfile("80.00");
        profileB = healthProfile("45.00");

        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(profileA));
        when(healthProfileRepository.findByUserId(userB)).thenReturn(Optional.of(profileB));
        when(adaptationRepository.findByHealthProfile(any())).thenAnswer(inv ->
                Optional.ofNullable(adaptationTable.get(inv.<HealthProfile>getArgument(0))));
        when(adaptationRepository.save(any(ProteinAdaptationProfile.class))).thenAnswer(inv -> {
            ProteinAdaptationProfile p = inv.getArgument(0);
            adaptationTable.put(p.getHealthProfile(), p);
            return p;
        });
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static HealthProfile healthProfile(String intake) {
        HealthProfile hp = HealthProfile.builder().build();
        hp.setId(UUID.randomUUID());
        PersonalProfile pp = PersonalProfile.builder()
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusYears(30).minusDays(1))
                .heightCm(180.0).weightKg(new BigDecimal("80"))
                .occupation(Occupation.OFFICE).build();
        pp.setHealthProfile(hp);
        FitnessGoalProfile fg = FitnessGoalProfile.builder().primaryGoal(Goal.MAINTENANCE).build();
        fg.setHealthProfile(hp);
        hp.setPersonalProfile(pp);
        hp.setFitnessGoalProfile(fg);
        if (intake != null) {
            hp.setCurrentNutritionProfile(CurrentNutritionProfile.builder()
                    .currentProteinGrams(new BigDecimal(intake)).build());
        }
        return hp;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenFor(UUID user) {
        return jwtUtil.generateToken(user);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getAs(UUID user) {
        return get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(user));
    }

    private static String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    // ── authentication ─────────────────────────────────────────────────

    @Test
    void noToken_is401_withJsonBody() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED));
    }

    @Test
    void garbageToken_is401() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED));
    }

    @Test
    void wrongSchemeAndEmptyBearer_are401() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_is401() throws Exception {
        String expired = Jwts.builder().setSubject(userA.toString())
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key()).compact();

        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AUTH_REQUIRED));
    }

    @Test
    void tokenSignedWithAnotherKey_is401() throws Exception {
        Key other = Keys.hmacShaKeyFor(
                "a-completely-different-signing-key-that-is-long-enough-for-hs256-0123456789"
                        .getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().setSubject(userA.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(other).compact();

        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blacklistedToken_is401_andOtherUsersAreUnaffected() throws Exception {
        String revoked = tokenFor(userA);
        when(tokenBlacklistService.isBlacklisted(revoked)).thenReturn(true);

        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + revoked))
                .andExpect(status().isUnauthorized());
        mvc.perform(getAs(userB)).andExpect(status().isOk());
    }

    @Test
    void unauthenticatedResponses_doNotLeakInternals() throws Exception {
        String text = body(mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer x.y.z"))
                .andExpect(status().isUnauthorized()).andReturn());

        assertThat(text).doesNotContain("io.jsonwebtoken", "Exception", "at com.", "secret");
    }

    @Test
    void unauthenticatedRequest_neverReachesTheRepositories() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());

        org.mockito.Mockito.verifyNoInteractions(healthProfileRepository, adaptationRepository);
    }

    // ── authorisation / method handling ────────────────────────────────

    @Test
    void otherMethods_requireAuthenticationFirst() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL)).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void otherMethods_whenAuthenticated_are405() throws Exception {
        for (var builder : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
                post(URL), put(URL), delete(URL)}) {
            mvc.perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(userA)))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ── success + serialization ────────────────────────────────────────

    @Test
    void authenticatedUser_getsTargetWithPolicyAFields() throws Exception {
        mvc.perform(getAs(userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinIntakeGrams").value(80.00))
                .andExpect(jsonPath("$.adaptationBaselineProteinGrams").value(80.00))
                .andExpect(jsonPath("$.adaptationBaselineOutOfDate").value(false))
                .andExpect(jsonPath("$.tdee").isNumber())
                .andExpect(jsonPath("$.finalCalories").isNumber())
                .andExpect(jsonPath("$.proteinGrams").isNumber())
                .andExpect(jsonPath("$.longTermProteinGrams").isNumber())
                .andExpect(jsonPath("$.fatGrams").isNumber())
                .andExpect(jsonPath("$.carbohydrateGrams").isNumber())
                .andExpect(jsonPath("$.fibreGrams").isNumber())
                .andExpect(jsonPath("$.waterMl").isNumber())
                .andExpect(jsonPath("$.adaptationStage").isString())
                .andExpect(jsonPath("$.rampUpRequired").isBoolean())
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void responseContainsExactlyTheDocumentedFields() throws Exception {
        String json = body(mvc.perform(getAs(userA)).andExpect(status().isOk()).andReturn());
        var node = new tools.jackson.databind.json.JsonMapper().readTree(json);

        java.util.Set<String> names = new java.util.TreeSet<>();
        node.propertyNames().forEach(names::add);

        assertThat(names).containsExactlyInAnyOrder(
                "tdee", "proposedCalories", "finalCalories", "safetyAdjusted", "status",
                "requiresClinicalReview", "proteinGrams", "longTermProteinGrams", "fatGrams",
                "carbohydrateGrams", "fibreGrams", "waterMl", "adaptationStage", "rampUpRequired",
                "nextEligibleProteinIncreaseDate", "currentProteinIntakeGrams",
                "adaptationBaselineProteinGrams", "adaptationBaselineOutOfDate", "warnings");
    }

    @Test
    void unknownIntake_isSerialisedAsExplicitNull_andWarned() throws Exception {
        HealthProfile hp = healthProfile(null);
        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(hp));

        String json = body(mvc.perform(getAs(userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinIntakeGrams").value((Object) null))
                .andExpect(jsonPath("$.adaptationBaselineProteinGrams").value((Object) null))
                .andExpect(jsonPath("$.adaptationBaselineOutOfDate").value(false))
                .andReturn());

        assertThat(json).contains("\"currentProteinIntakeGrams\":null");
        assertThat(json).contains("Current protein intake is unknown");
    }

    // ── cross-user isolation ───────────────────────────────────────────

    @Test
    void eachUserSeesOnlyTheirOwnIntake() throws Exception {
        mvc.perform(getAs(userA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinIntakeGrams").value(80.00));
        mvc.perform(getAs(userB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinIntakeGrams").value(45.00));
    }

    @Test
    void adaptationProfiles_areKeptPerUser() throws Exception {
        mvc.perform(getAs(userA)).andExpect(status().isOk());
        mvc.perform(getAs(userB)).andExpect(status().isOk());

        assertThat(adaptationTable).hasSize(2);
        assertThat(adaptationTable.get(profileA).getCurrentProteinGrams()).isEqualByComparingTo("80");
        assertThat(adaptationTable.get(profileB).getCurrentProteinGrams()).isEqualByComparingTo("45");
    }

    @Test
    void repositoryIsQueriedOnlyForTheAuthenticatedUser() throws Exception {
        mvc.perform(getAs(userA)).andExpect(status().isOk());

        org.mockito.Mockito.verify(healthProfileRepository).findByUserId(userA);
        org.mockito.Mockito.verify(healthProfileRepository, org.mockito.Mockito.never()).findByUserId(userB);
    }

    @Test
    void tokenSubject_cannotBeOverriddenByRequestParameters() throws Exception {
        mvc.perform(getAs(userA).param("userId", userB.toString()).param("healthProfileId", "x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProteinIntakeGrams").value(80.00));
    }

    // ── error mapping ──────────────────────────────────────────────────

    @Test
    void userWithoutHealthProfile_is404() throws Exception {
        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.empty());

        mvc.perform(getAs(userA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void optimisticLockFailureWhileSaving_is409() throws Exception {
        when(adaptationRepository.save(any(ProteinAdaptationProfile.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(ProteinAdaptationProfile.class, "x"));

        mvc.perform(getAs(userA)).andExpect(status().isConflict());
    }

    @Test
    void unexpectedFailure_isGeneric500_withoutInternals() throws Exception {
        when(adaptationRepository.save(any(ProteinAdaptationProfile.class)))
                .thenThrow(new IllegalStateException("secret-internal-detail 123.45"));

        String text = body(mvc.perform(getAs(userA))
                .andExpect(status().isInternalServerError()).andReturn());

        assertThat(text).doesNotContain("secret-internal-detail", "123.45", "IllegalStateException");
    }

    @Test
    void incompleteProfile_missingHeight_is422_withFieldAndNoInternals() throws Exception {
        HealthProfile hp = healthProfile("80");
        hp.getPersonalProfile().setHeightCm(null);
        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(hp));

        String text = body(mvc.perform(getAs(userA))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Content"))
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.validationErrors[0].field").value("heightCm"))
                .andReturn());

        assertThat(text).doesNotContain("IllegalStateException", "IncompleteProfileException",
                "com.dietapp", "Height is required", "stackTrace");
    }

    @Test
    void incompleteProfile_missingWeightOrGender_is422() throws Exception {
        HealthProfile noWeight = healthProfile("80");
        noWeight.getPersonalProfile().setWeightKg(null);
        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(noWeight));
        mvc.perform(getAs(userA)).andExpect(status().is(422))
                .andExpect(jsonPath("$.validationErrors[0].field").value("weightKg"));

        HealthProfile noGender = healthProfile("80");
        noGender.getPersonalProfile().setGender(null);
        when(healthProfileRepository.findByUserId(userB)).thenReturn(Optional.of(noGender));
        mvc.perform(getAs(userB)).andExpect(status().is(422))
                .andExpect(jsonPath("$.validationErrors[0].field").value("gender"));
    }

    @Test
    void incompleteProfile_missingSections_are422() throws Exception {
        HealthProfile noPersonal = healthProfile("80");
        noPersonal.setPersonalProfile(null);
        when(healthProfileRepository.findByUserId(userA)).thenReturn(Optional.of(noPersonal));
        mvc.perform(getAs(userA)).andExpect(status().is(422))
                .andExpect(jsonPath("$.validationErrors[0].field").value("personalProfile"));

        HealthProfile noGoal = healthProfile("80");
        noGoal.setFitnessGoalProfile(null);
        when(healthProfileRepository.findByUserId(userB)).thenReturn(Optional.of(noGoal));
        mvc.perform(getAs(userB)).andExpect(status().is(422))
                .andExpect(jsonPath("$.validationErrors[0].field").value("fitnessGoalProfile"));
    }

    @Test
    void firstInsertRaceLoser_is409_withGenericMessage_andNoInternals() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "could not execute statement [insert into protein_adaptation_profiles ...]",
                        new java.sql.SQLException(
                                "duplicate key value violates unique constraint "
                                        + "\"uq_protein_adaptation_health_profile\"", "23505")))
                .when(adaptationRepository).flush();

        String text = body(mvc.perform(getAs(userA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andReturn());

        assertThat(text).doesNotContain("protein_adaptation", "duplicate key", "insert into",
                "23505", "SQLException", "DataIntegrityViolation");
    }

    @Test
    void nonUniqueIntegrityViolation_staysGeneric500() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "not-null violation", new java.sql.SQLException("null value", "23502")))
                .when(adaptationRepository).flush();

        mvc.perform(getAs(userA)).andExpect(status().isInternalServerError());
    }
}
