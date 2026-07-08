create table health_profiles (
  user_id uuid primary key references users(id),
  age int,
  gender text,
  height_cm int,
  weight_kg numeric(5,2),
  goal text,
  training_level text,
  activity_level text,
  diet_type text,
  meals_per_day int,
  origin jsonb,
  allergies text[],
  diseases text[],
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table diet_plans (
  id uuid primary key,
  user_id uuid references users(id),
  version int default 1,
  generated_at timestamptz,
  valid_from timestamptz,
  valid_till timestamptz,
  daily_calories int,
  protein_target int,
  diet_json jsonb,
  created_at timestamptz default now()
);
