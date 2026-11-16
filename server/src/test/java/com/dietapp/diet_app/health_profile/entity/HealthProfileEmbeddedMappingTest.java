package com.dietapp.diet_app.health_profile.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code HealthProfile.currentNutritionProfile} embedded mapping
 * using Hibernate metadata built for the PostgreSQL dialect.
 *
 * <p>No database connection is opened: the whole application entity set is
 * bound (so unrelated mapping errors would fail this test too), and persistence
 * is exercised through Hibernate's own property access for the mapped attributes.</p>
 */
class HealthProfileEmbeddedMappingTest {

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

    private static PersistentClass healthProfileBinding() {
        return metadata.getEntityBinding(HealthProfile.class.getName());
    }

    private static List<String> columnsOf(Property p) {
        return p.getValue().getColumns().stream().map(c -> c.getText()).toList();
    }

    // ── 1. Hibernate recognises the embedded mapping ──────────────────

    @Test
    void currentNutritionProfile_isMappedAsComponentOnHealthProfile() {
        Property p = healthProfileBinding().getProperty("currentNutritionProfile");

        assertThat(p.getValue()).isInstanceOf(Component.class);
        assertThat(columnsOf(p)).containsExactly("current_protein_grams");
        assertThat(((Component) p.getValue()).getTable().getName()).isEqualTo("health_profiles");
    }

    @Test
    void currentNutritionProfile_hasNoOwnEntityOrTable() {
        assertThat(metadata.getEntityBinding(CurrentNutritionProfile.class.getName())).isNull();
        List<String> tables = new ArrayList<>();
        for (var ns : metadata.getDatabase().getNamespaces()) {
            ns.getTables().forEach(t -> tables.add(t.getName()));
        }
        assertThat(tables).doesNotContain("current_nutrition_profiles", "current_nutrition_profile");
    }

    // ── 4. Existing HealthProfile fields remain mapped correctly ──────

    @Test
    void existingHealthProfileColumns_remainMapped() {
        PersistentClass pc = healthProfileBinding();

        assertThat(pc.getTable().getName()).isEqualTo("health_profiles");
        assertThat(columnsOf(pc.getProperty("user"))).containsExactly("user_id");
        assertThat(columnsOf(pc.getProperty("onboardingCompleted"))).containsExactly("onboarding_completed");
        assertThat(columnsOf(pc.getProperty("profileCompletionPercentage")))
                .containsExactly("profile_completion_percentage");
        assertThat(pc.getIdentifierProperty().getName()).isEqualTo("id");
        assertThat(pc.getVersion().getName()).isEqualTo("version");
    }

    @Test
    void noColumnNameCollisionOnHealthProfileTable() {
        List<String> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        healthProfileBinding().getTable().getColumns().forEach(c -> all.add(c.getName()));

        // a collision would collapse two attributes into one column and shrink this set
        assertThat(all).allMatch(seen::add);
        assertThat(all).contains(
                "id", "created_at", "updated_at", "version", "user_id",
                "onboarding_completed", "profile_completion_percentage",
                "current_protein_grams");
    }

    @Test
    void inverseOneToOneAssociations_stillMappedAndUntouched() {
        PersistentClass pc = healthProfileBinding();
        for (String name : List.of("personalProfile", "fitnessGoalProfile", "nutritionPreferenceProfile",
                "workoutProfile", "medicalProfile", "cookingProfile",
                "lifestylePreferenceProfile", "supplementProfile")) {
            assertThat(pc.getProperty(name)).as(name).isNotNull();
            // inverse side owns no column on health_profiles
            assertThat(pc.getTable().getColumns().stream().map(c -> c.getName()))
                    .as(name).doesNotContain(name);
        }
    }

    // ── 2 + 3. Persist / retrieve through Hibernate's attribute access ─

    @Test
    void currentProteinIntake_roundTripsThroughHibernateAttributeAccess() {
        EntityPersister persister = sessionFactory.unwrap(
                org.hibernate.engine.spi.SessionFactoryImplementor.class)
                .getMappingMetamodel().getEntityDescriptor(HealthProfile.class);
        EntityMappingType mapping = persister;
        int idx = persister.getPropertyIndex("currentNutritionProfile");

        HealthProfile source = HealthProfile.builder()
                .currentNutritionProfile(CurrentNutritionProfile.builder()
                        .currentProteinGrams(new BigDecimal("72.50")).build())
                .build();

        Object[] values = persister.getValues(source);
        assertThat(values[idx]).isInstanceOf(CurrentNutritionProfile.class);

        HealthProfile target = new HealthProfile();
        persister.setValues(target, values);

        assertThat(mapping).isNotNull();
        assertThat(target.getCurrentNutritionProfile()).isNotNull();
        assertThat(target.getCurrentNutritionProfile().getCurrentProteinGrams())
                .isEqualByComparingTo("72.50");
    }

    @Test
    void healthProfileWithNullCurrentNutritionProfile_works() {
        EntityPersister persister = sessionFactory.unwrap(
                org.hibernate.engine.spi.SessionFactoryImplementor.class)
                .getMappingMetamodel().getEntityDescriptor(HealthProfile.class);
        int idx = persister.getPropertyIndex("currentNutritionProfile");

        HealthProfile source = HealthProfile.builder().build();
        assertThat(source.getCurrentNutritionProfile()).isNull();

        Object[] values = persister.getValues(source);
        assertThat(values[idx]).isNull();

        HealthProfile target = new HealthProfile();
        persister.setValues(target, values);
        assertThat(target.getCurrentNutritionProfile()).isNull();
    }

    @Test
    void currentNutritionProfileColumn_isNullableSoNullEmbeddedIsPersistable() {
        Property p = healthProfileBinding().getProperty("currentNutritionProfile");
        assertThat(p.getValue().getColumns()).allMatch(c -> c.isNullable());
    }
}

