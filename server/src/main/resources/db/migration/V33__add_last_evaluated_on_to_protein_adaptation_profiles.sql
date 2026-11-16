-- =============================================================================
-- V33 | Add last_evaluated_on column to protein_adaptation_profiles
--
-- Purpose:
--   Records the calendar date on which the scheduled protein-adaptation
--   process last counted a day for this profile. ProteinAdaptationService
--   uses it to guarantee that a profile is evaluated at most once per day,
--   even if the scheduled job is retried, restarted, or run by two instances.
--
-- Notes:
--   - Column is nullable: existing rows remain NULL, which means "never
--     evaluated by the scheduled process".
--   - No default is applied and no existing data is updated.
--   - DATE (no time zone) to match last_target_change_at.
-- =============================================================================

ALTER TABLE protein_adaptation_profiles
    ADD COLUMN last_evaluated_on DATE;
