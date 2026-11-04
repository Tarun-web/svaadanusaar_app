-- =============================================================================
-- V32 | Add training_status column to workout_profiles
--
-- Purpose:
--   Stores the user's resistance-training experience level so that
--   ProteinCalculator (and future calculators) can contextualise targets.
--
-- Notes:
--   - Column is nullable: existing rows and new profiles where the user has
--     not provided this information will remain NULL.
--   - Stored as VARCHAR(32) to match @Enumerated(EnumType.STRING) in JPA.
--   - Valid values: BEGINNER | INTERMEDIATE | ADVANCED  (TrainingStatus enum).
--   - No default is applied — NULL means "not yet provided".
-- =============================================================================

ALTER TABLE workout_profiles
    ADD COLUMN training_status VARCHAR(32);
