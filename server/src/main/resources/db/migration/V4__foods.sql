create table foods (
  id text primary key,
  name text,
  region text,
  diet_type text,
  nutrition jsonb,
  recipe_url text,
  source_api text,
  is_active boolean default true,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
