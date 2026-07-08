create table chat_usage (
  user_id uuid references users(id),
  usage_date date,
  used int,
  updated_at timestamptz default now(),
  primary key(user_id, usage_date)
);

create table chat_messages (
  id uuid primary key,
  user_id uuid references users(id),
  role text,
  message text,
  created_at timestamptz default now()
);
