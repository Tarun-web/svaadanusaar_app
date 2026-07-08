ALTER TABLE weight_logs
ADD COLUMN logged_at timestamptz DEFAULT now();

CREATE INDEX idx_body_weight_logs_user_time
ON weight_logs(user_id, logged_at DESC);
