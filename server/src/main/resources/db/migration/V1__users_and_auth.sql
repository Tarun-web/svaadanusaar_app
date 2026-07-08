create table users (
  id uuid primary key,
  name text,
  email text unique,
  phone text unique,
  is_blocked boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table user_auth (
  user_id uuid primary key references users(id),
  provider text,
  provider_id text,
  last_login_at timestamptz
);
