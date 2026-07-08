-- Fast lookup for active diet
CREATE INDEX IF NOT EXISTS idx_diet_user_status
ON diet_plans(user_id, status);

-- Fast version lookup
CREATE INDEX IF NOT EXISTS idx_diet_user_version
ON diet_plans(user_id, version DESC);

-- Prevent multiple ACTIVE diets
CREATE UNIQUE INDEX IF NOT EXISTS one_active_diet_per_user
ON diet_plans(user_id)
WHERE status = 'ACTIVE';

-- Safety checks
ALTER TABLE diet_plans
ADD CONSTRAINT check_calories_positive CHECK (daily_calories > 900);

ALTER TABLE diet_plans
ADD CONSTRAINT check_protein_positive CHECK (protein_target > 0);
