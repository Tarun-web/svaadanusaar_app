-- =============================================================================
-- V34 | Add current_protein_grams column to health_profiles
--
-- Purpose:
--   Stores the user's self-reported daily protein intake, mapped by
--   HealthProfile.currentNutritionProfile (@Embedded CurrentNutritionProfile).
--
-- Notes:
--   - Column is nullable: NULL means "intake not supplied / unknown".
--   - NUMERIC(8,2) matches @Column(precision = 8, scale = 2) in JPA.
--   - No default and no backfill. The legacy health_profiles.current_protein_intake
--     column is intentionally left untouched.
--   - This is distinct from protein_adaptation_profiles.current_protein_grams,
--     which is the adaptation baseline captured by ProteinAdaptationService.
-- =============================================================================

ALTER TABLE health_profiles
    ADD COLUMN current_protein_grams NUMERIC(8,2);
