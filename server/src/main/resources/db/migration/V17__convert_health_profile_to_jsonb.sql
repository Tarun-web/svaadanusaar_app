-- Migration V17: Convert HealthProfile @ElementCollection to JSONB
-- This migration converts separate join tables to JSONB columns in the health_profiles table

-- Step 1: Add new JSONB columns to health_profiles table
ALTER TABLE health_profiles
    ADD COLUMN medical_conditions JSONB,
    ADD COLUMN allergies JSONB,
    ADD COLUMN medications JSONB,
    ADD COLUMN preferred_cuisines JSONB,
    ADD COLUMN taste_preferences JSONB,
    ADD COLUMN favourite_foods JSONB,
    ADD COLUMN disliked_foods JSONB,
    ADD COLUMN religious_restrictions JSONB,
    ADD COLUMN equipments JSONB,
    ADD COLUMN current_supplements JSONB;

-- Step 2: Drop old @ElementCollection join tables (they are auto-generated and no longer needed)
-- These tables were created by Hibernate from @ElementCollection annotations

-- Drop tables if they exist (using IF EXISTS for safety)
DROP TABLE IF EXISTS health_profile_medical_conditions CASCADE;
DROP TABLE IF EXISTS health_profile_allergies CASCADE;
DROP TABLE IF EXISTS health_profile_medications CASCADE;
DROP TABLE IF EXISTS health_profile_preferred_cuisines CASCADE;
DROP TABLE IF EXISTS health_profile_taste_preferences CASCADE;
DROP TABLE IF EXISTS health_profile_favourite_foods CASCADE;
DROP TABLE IF EXISTS health_profile_disliked_foods CASCADE;
DROP TABLE IF EXISTS health_profile_religious_restrictions CASCADE;
DROP TABLE IF EXISTS cooking_profile_equipments CASCADE;
DROP TABLE IF EXISTS supplement_profile_current_supplements CASCADE;

-- Step 3: Create index for better JSONB query performance (optional, for frequently searched fields)
CREATE INDEX idx_health_profiles_medical_conditions ON health_profiles USING GIN (medical_conditions);
CREATE INDEX idx_health_profiles_allergies ON health_profiles USING GIN (allergies);
CREATE INDEX idx_health_profiles_preferred_cuisines ON health_profiles USING GIN (preferred_cuisines);
CREATE INDEX idx_health_profiles_favourite_foods ON health_profiles USING GIN (favourite_foods);

-- Step 4: Add comment explaining the change
COMMENT ON COLUMN health_profiles.medical_conditions IS 'Medical conditions stored as JSONB array';
COMMENT ON COLUMN health_profiles.allergies IS 'Food allergies stored as JSONB array';
COMMENT ON COLUMN health_profiles.medications IS 'Current medications stored as JSONB array';
COMMENT ON COLUMN health_profiles.preferred_cuisines IS 'Preferred cuisines stored as JSONB array';
COMMENT ON COLUMN health_profiles.taste_preferences IS 'Taste preferences stored as JSONB array';
COMMENT ON COLUMN health_profiles.favourite_foods IS 'Favourite foods stored as JSONB array';
COMMENT ON COLUMN health_profiles.disliked_foods IS 'Disliked foods stored as JSONB array';
COMMENT ON COLUMN health_profiles.religious_restrictions IS 'Religious restrictions stored as JSONB array';
COMMENT ON COLUMN health_profiles.equipments IS 'Cooking equipments stored as JSONB array';
COMMENT ON COLUMN health_profiles.current_supplements IS 'Current supplements stored as JSONB array';

