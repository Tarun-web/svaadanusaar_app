-- =============================================================================
-- V31 | Create protein_adaptation_profiles table
--
-- Purpose:
--   Stores the ongoing state of each user's protein adaptation journey.
--   Managed exclusively by ProteinAdaptationService — never written by users.
--
-- Relationship:
--   health_profiles  1 ─── 1  protein_adaptation_profiles
--
-- Key field semantics:
--   current_protein_grams   = what the user currently eats (nullable = unknown)
--   current_target_grams    = what Svaadanusaar currently recommends
--   long_term_target_grams  = eventual physiological target from ProteinCalculator
-- =============================================================================

CREATE TABLE protein_adaptation_profiles
(
    -- ── Identity (matches BaseEntity) ────────────────────────────────────────
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    version                 BIGINT       NOT NULL DEFAULT 0,

    -- ── Parent reference ─────────────────────────────────────────────────────
    health_profile_id       UUID         NOT NULL,

    -- ── Protein intake / targets (grams, 2 decimal places) ───────────────────

    -- Nullable: null = current intake is unknown
    current_protein_grams   NUMERIC(8,2),

    -- The long-term physiological target produced by ProteinCalculator
    long_term_target_grams  NUMERIC(8,2) NOT NULL,

    -- What Svaadanusaar currently recommends (written to NutritionTarget.protein_grams)
    current_target_grams    NUMERIC(8,2) NOT NULL,

    -- ── Adaptation state ─────────────────────────────────────────────────────
    adaptation_stage        VARCHAR(32)  NOT NULL,   -- ProteinAdaptationStage enum
    days_at_current_target  INT          NOT NULL DEFAULT 0,
    tolerance_status        VARCHAR(32)  NOT NULL,   -- ProteinToleranceStatus enum
    last_target_change_at   DATE,

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT pk_protein_adaptation_profiles
        PRIMARY KEY (id),

    CONSTRAINT fk_protein_adaptation_health_profile
        FOREIGN KEY (health_profile_id)
        REFERENCES health_profiles (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_protein_adaptation_health_profile
        UNIQUE (health_profile_id),

    CONSTRAINT chk_protein_current_grams_non_negative
        CHECK (current_protein_grams IS NULL OR current_protein_grams >= 0),

    CONSTRAINT chk_protein_long_term_target_positive
        CHECK (long_term_target_grams > 0),

    CONSTRAINT chk_protein_current_target_positive
        CHECK (current_target_grams > 0),

    CONSTRAINT chk_protein_days_non_negative
        CHECK (days_at_current_target >= 0)
);

-- ── Index on health_profile_id for fast single-user lookups ──────────────────
CREATE UNIQUE INDEX idx_protein_adaptation_health_profile
    ON protein_adaptation_profiles (health_profile_id);
