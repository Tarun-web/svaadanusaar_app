create table notification_preferences (
  user_id uuid primary key references users(id),
  push_enabled boolean default true,
  email_enabled boolean default true,
  quiet_from time,
  quiet_to time,
  updated_at timestamptz default now()
);

create table notification_logs (
  id uuid primary key,
  user_id uuid references users(id),
  event_type text,
  channel text,
  status text,
  error_message text,
  created_at timestamptz default now()
);
