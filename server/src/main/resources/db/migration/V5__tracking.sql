create table meal_logs (
  id uuid primary key,
  user_id uuid references users(id),
  log_date date,
  meal_type text,
  foods jsonb,
  total_calories int,
  total_protein int,
  created_at timestamptz default now(),
  unique(user_id, log_date, meal_type)
);

create table weight_logs (
  id uuid primary key,
  user_id uuid references users(id),
  log_date date,
  weight_kg numeric(5,2),
  created_at timestamptz default now(),
  unique(user_id, log_date)
);

create table daily_metrics (
  user_id uuid references users(id),
  log_date date,
  target_calories int,
  consumed_calories int,
  target_protein int,
  consumed_protein int,
  calorie_compliance int,
  protein_compliance int,
  updated_at timestamptz default now(),
  primary key(user_id, log_date)
);

create table streaks (
  user_id uuid references users(id),
  type text,
  current_count int,
  longest_count int,
  last_updated date,
  primary key(user_id, type)
);
