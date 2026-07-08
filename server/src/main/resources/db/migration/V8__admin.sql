create table admin_users (
  id uuid primary key,
  email text unique,
  password_hash text,
  role text,
  is_active boolean default true,
  created_at timestamptz default now(),
  last_login_at timestamptz
);

create table admin_audit_logs (
  id uuid primary key,
  admin_id uuid references admin_users(id),
  action text,
  entity_type text,
  entity_id text,
  before_state jsonb,
  after_state jsonb,
  created_at timestamptz default now()
);
