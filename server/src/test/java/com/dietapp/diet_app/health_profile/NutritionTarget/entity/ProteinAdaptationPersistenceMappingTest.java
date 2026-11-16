package com.dietapp.diet_app.health_profile.NutritionTarget.entity;

import com.dietapp.diet_app.common.entity.BaseEntity;
import com.dietapp.diet_app.health_profile.NutritionTarget.repository.ProteinAdaptationProfileRepository;
import com.dietapp.diet_app.health_profile.NutritionTarget.scheduler.EligibleProteinProfile;
import com.dietapp.diet_app.health_profile.entity.HealthProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Query;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence verification that needs <strong>no database</strong>.
 *
 * <p>What this proves: Hibernate accepts the entity mappings for the PostgreSQL
 * dialect, the mapped columns/constraints/versioning are what the migrations
 * describe, and the scheduler's HQL parses and type-checks against the mapping.</p>
 *
 * <p>What this does <strong>not</strong> prove: that any SQL executes on a real
 * PostgreSQL server, that Flyway applies the migrations, or that the existing
 * hybrid Flyway/Hibernate database matches. Those remain unverified.</p>
 */
class ProteinAdaptationPersistenceMappingTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private static Metadata metadata;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void buildMetadata() throws Exception {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting("hibernate.physical_naming_strategy",
                        PhysicalNamingStrategySnakeCaseImpl.class.getName())
                .build();

        MetadataSources sources = new MetadataSources(registry);
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(MappedSuperclass.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Embeddable.class));
        for (var bd : scanner.findCandidateComponents("com.dietapp.diet_app")) {
            sources.addAnnotatedClass(Class.forName(bd.getBeanClassName()));
        }
        metadata = sources.buildMetadata();
        sessionFactory = metadata.buildSessionFactory();
    }

    @AfterAll
    static void close() {
        if (sessionFactory != null) sessionFactory.close();
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static PersistentClass binding(Class<?> c) {
        return metadata.getEntityBinding(c.getName());
    }

    private static Map<String, Column> columns(Class<?> c) {
        Map<String, Column> out = new HashMap<>();
        for (Column col : binding(c).getTable().getColumns()) {
            out.put(col.getName().toLowerCase(Locale.ROOT), col);
        }
        return out;
    }

    private static String read(String prefix) throws IOException {
        try (var files = Files.list(MIGRATIONS)) {
            Path p = files.filter(f -> f.getFileName().toString().startsWith(prefix))
                    .findFirst().orElseThrow();
            // The files contain non-ASCII comment glyphs; ISO-8859-1 never fails to decode.
            return Files.readString(p, StandardCharsets.ISO_8859_1);
        }
    }

    /** Column name -> full definition text, for the body of the CREATE TABLE. */
    private static Map<String, String> createTableColumns(String sql, String table) {
        String noComments = sql.replaceAll("--[^\\r\\n]*", "");
        Matcher m = Pattern.compile(
                "CREATE\\s+TABLE\\s+" + table + "\\s*\\((.*)\\)\\s*;",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(noComments);
        assertThat(m.find()).as("CREATE TABLE " + table).isTrue();

        Map<String, String> out = new HashMap<>();
        for (String part : splitTopLevel(m.group(1))) {
            String def = part.trim();
            String first = def.split("\\s+")[0].toLowerCase(Locale.ROOT);
            if (first.equals("constraint")) continue;
            out.put(first, def);
        }
        return out;
    }

    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (char ch : body.toCharArray()) {
            if (ch == '(') depth++;
            if (ch == ')') depth--;
            if (ch == ',' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        if (!cur.toString().isBlank()) parts.add(cur.toString());
        return parts;
    }

    // ── ProteinAdaptationProfile mapping ───────────────────────────────

    @Test
    void adaptationProfile_mapsToExpectedTableAndColumns() {
        assertThat(binding(ProteinAdaptationProfile.class).getTable().getName())
                .isEqualTo("protein_adaptation_profiles");

        assertThat(columns(ProteinAdaptationProfile.class).keySet()).containsExactlyInAnyOrder(
                "id", "created_at", "updated_at", "version",
                "health_profile_id", "current_protein_grams", "long_term_target_grams",
                "current_target_grams", "adaptation_stage", "days_at_current_target",
                "tolerance_status", "last_target_change_at", "last_evaluated_on");
    }

    @Test
    void adaptationProfile_nullability_matchesIntent() {
        Map<String, Column> cols = columns(ProteinAdaptationProfile.class);

        // Baseline and the two bookkeeping dates are nullable.
        assertThat(cols.get("current_protein_grams").isNullable()).isTrue();
        assertThat(cols.get("last_target_change_at").isNullable()).isTrue();
        assertThat(cols.get("last_evaluated_on").isNullable()).isTrue();

        for (String required : List.of("health_profile_id", "long_term_target_grams",
                "current_target_grams", "adaptation_stage", "days_at_current_target",
                "tolerance_status")) {
            assertThat(cols.get(required).isNullable()).as(required).isFalse();
        }
    }

    @Test
    void adaptationProfile_numericPrecision_is8And2() {
        Map<String, Column> cols = columns(ProteinAdaptationProfile.class);
        for (String c : List.of("current_protein_grams", "long_term_target_grams",
                "current_target_grams")) {
            assertThat(cols.get(c).getPrecision()).as(c + " precision").isEqualTo(8);
            assertThat(cols.get(c).getScale()).as(c + " scale").isEqualTo(2);
        }
    }

    @Test
    void adaptationProfile_hasOptimisticLockVersion() {
        PersistentClass pc = binding(ProteinAdaptationProfile.class);

        assertThat(pc.isVersioned()).isTrue();
        assertThat(pc.getVersion().getName()).isEqualTo("version");
        assertThat(pc.getVersion().getType().getReturnedClass()).isEqualTo(Long.class);
    }

    @Test
    void everyEntityExtendingBaseEntity_isVersioned() {
        for (PersistentClass pc : metadata.getEntityBindings()) {
            if (BaseEntity.class.isAssignableFrom(pc.getMappedClass())) {
                assertThat(pc.isVersioned()).as(pc.getEntityName()).isTrue();
            }
        }
    }

    @Test
    void adaptationProfile_hasUniqueConstraintOnHealthProfile() {
        Table t = binding(ProteinAdaptationProfile.class).getTable();

        Set<String> uniqueColumnSets = new TreeSet<>();
        t.getUniqueKeys().values().forEach(uk -> uniqueColumnSets.add(
                uk.getColumns().stream().map(Column::getName).toList().toString()));
        t.getIndexes().values().forEach(ix -> uniqueColumnSets.add(
                ix.getColumns().stream().map(Column::getName).toList().toString()));

        assertThat(uniqueColumnSets).contains("[health_profile_id]");
    }

    @Test
    void adaptationProfile_toHealthProfile_isLazyRequiredOneToOne() throws Exception {
        OneToOne ann = ProteinAdaptationProfile.class
                .getDeclaredField("healthProfile").getAnnotation(OneToOne.class);

        assertThat(ann.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(ann.optional()).isFalse();
        assertThat(ann.cascade()).as("child must not cascade to the parent").isEmpty();
    }

    @Test
    void healthProfile_doesNotCascadeToAdaptationProfile() {
        // Deletion of the adaptation row is delegated to the database
        // (V31: ON DELETE CASCADE). The entity graph has no inverse side, so
        // an ORM-only environment (ddl-auto) has no cascade for this table.
        for (var f : HealthProfile.class.getDeclaredFields()) {
            assertThat(f.getType()).as(f.getName())
                    .isNotEqualTo(ProteinAdaptationProfile.class)
                    .isNotEqualTo(NutritionTarget.class);
        }
    }

    @Test
    void healthProfile_childProfiles_cascadeAllWithOrphanRemoval() {
        for (var f : HealthProfile.class.getDeclaredFields()) {
            OneToOne ann = f.getAnnotation(OneToOne.class);
            if (ann != null && !ann.mappedBy().isEmpty()) {
                assertThat(ann.cascade()).as(f.getName()).contains(CascadeType.ALL);
                assertThat(ann.orphanRemoval()).as(f.getName()).isTrue();
            }
        }
    }

    // ── NutritionTarget mapping (entity exists; nothing persists it yet) ─

    @Test
    void nutritionTarget_mapsRequiredColumns_andUniqueHealthProfile() {
        Map<String, Column> cols = columns(NutritionTarget.class);

        assertThat(binding(NutritionTarget.class).getTable().getName())
                .isEqualTo("nutrition_targets");
        assertThat(cols.keySet()).contains("health_profile_id", "weight_used_kg", "bmr",
                "tdee", "target_calories", "protein_grams", "fat_grams",
                "carbohydrate_grams", "fiber_grams", "water_ml", "calorie_adjustment",
                "activity_level", "calculation_method");
        assertThat(cols.get("health_profile_id").isNullable()).isFalse();
        assertThat(binding(NutritionTarget.class).isVersioned()).isTrue();
    }

    // ── Latest intake mapping on HealthProfile ─────────────────────────

    @Test
    void latestIntake_isNullableColumnOnHealthProfiles_distinctFromBaselineTable() {
        Column latest = columns(HealthProfile.class).get("current_protein_grams");

        assertThat(latest).isNotNull();
        assertThat(latest.isNullable()).isTrue();
        assertThat(latest.getPrecision()).isEqualTo(8);
        assertThat(latest.getScale()).isEqualTo(2);
        assertThat(binding(HealthProfile.class).getTable().getName())
                .isNotEqualTo(binding(ProteinAdaptationProfile.class).getTable().getName());
    }

    // ── Scheduler HQL ──────────────────────────────────────────────────

    @Test
    void schedulerQuery_parsesAndTypeChecksAgainstTheMapping() throws Exception {
        String hql = ProteinAdaptationProfileRepository.class
                .getMethod("findEligibleForScheduledEvaluation",
                        java.util.Collection.class, java.time.LocalDate.class,
                        java.util.UUID.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class).value();

        try (Session session = sessionFactory.openSession()) {
            // Parsing + semantic analysis happen here; no connection is acquired.
            var query = session.createQuery(hql, EligibleProteinProfile.class);

            assertThat(query.getParameter("stages")).isNotNull();
            assertThat(query.getParameter("today")).isNotNull();
            assertThat(query.getParameter("afterId")).isNotNull();
            assertThat(query.getParameters()).hasSize(3);
        }
    }

    @Test
    void schedulerQuery_filtersInSql_andIsKeysetOrdered() throws Exception {
        String hql = ProteinAdaptationProfileRepository.class
                .getMethod("findEligibleForScheduledEvaluation",
                        java.util.Collection.class, java.time.LocalDate.class,
                        java.util.UUID.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class).value().replaceAll("\\s+", " ");

        assertThat(hql).contains("p.adaptationStage IN :stages");
        assertThat(hql).contains("p.lastEvaluatedOn IS NULL OR p.lastEvaluatedOn < :today");
        assertThat(hql).contains("p.id > :afterId");
        assertThat(hql).contains("ORDER BY p.id");
    }

    // ── Migration text vs entity (static text analysis only) ───────────

    @Test
    void v31_columnsMatchEntity_includingNullability() throws IOException {
        Map<String, String> sqlCols = createTableColumns(
                read("V31__"), "protein_adaptation_profiles");
        Map<String, Column> entityCols = columns(ProteinAdaptationProfile.class);

        // V33 adds last_evaluated_on separately.
        Set<String> expected = new TreeSet<>(entityCols.keySet());
        expected.remove("last_evaluated_on");
        assertThat(sqlCols.keySet()).containsExactlyInAnyOrderElementsOf(expected);

        sqlCols.forEach((name, def) -> {
            boolean sqlNotNull = def.toUpperCase(Locale.ROOT).contains("NOT NULL");
            if (name.equals("version")) {
                // BaseEntity.version has no nullable=false, so Hibernate maps it as
                // nullable; the migration is stricter (NOT NULL DEFAULT 0). Safe.
                assertThat(sqlNotNull).as("NOT NULL on version").isTrue();
                return;
            }
            assertThat(sqlNotNull).as("NOT NULL on " + name)
                    .isEqualTo(!entityCols.get(name).isNullable());
        });
    }

    @Test
    void v31_declaresUniqueFkWithCascadeAndNonNegativeChecks() throws IOException {
        String sql = read("V31__").replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);

        assertThat(sql).contains("FOREIGN KEY (HEALTH_PROFILE_ID) REFERENCES HEALTH_PROFILES (ID) ON DELETE CASCADE");
        assertThat(sql).contains("UNIQUE (HEALTH_PROFILE_ID)");
        assertThat(sql).contains("CHECK (CURRENT_PROTEIN_GRAMS IS NULL OR CURRENT_PROTEIN_GRAMS >= 0)");
        assertThat(sql).contains("CHECK (DAYS_AT_CURRENT_TARGET >= 0)");
    }

    @Test
    void v33_addsNullableDateColumnMatchingEntity() throws IOException {
        String sql = read("V33__").replaceAll("--[^\\r\\n]*", "").replaceAll("\\s+", " ")
                .trim().toUpperCase(Locale.ROOT);

        assertThat(sql).isEqualTo(
                "ALTER TABLE PROTEIN_ADAPTATION_PROFILES ADD COLUMN LAST_EVALUATED_ON DATE;");
        assertThat(columns(ProteinAdaptationProfile.class).get("last_evaluated_on").isNullable())
                .isTrue();
    }

    @Test
    void v34_addsNullableNumeric82ColumnMatchingEmbeddedMapping() throws IOException {
        String sql = read("V34__").replaceAll("--[^\\r\\n]*", "").replaceAll("\\s+", " ")
                .trim().toUpperCase(Locale.ROOT);

        assertThat(sql).isEqualTo(
                "ALTER TABLE HEALTH_PROFILES ADD COLUMN CURRENT_PROTEIN_GRAMS NUMERIC(8,2);");
        assertThat(columns(HealthProfile.class).get("current_protein_grams").isNullable())
                .isTrue();
    }

    @Test
    void migrationVersions_v31ToV34_areContiguousAndUnique() throws IOException {
        try (var files = Files.list(MIGRATIONS)) {
            List<String> names = files.map(f -> f.getFileName().toString())
                    .filter(n -> n.matches("V3[1-4]__.*\\.sql")).sorted().toList();
            assertThat(names).hasSize(4);
            assertThat(names.stream().map(n -> n.substring(0, 3)).toList())
                    .containsExactly("V31", "V32", "V33", "V34");
        }
    }
}
