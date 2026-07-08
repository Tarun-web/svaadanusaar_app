create table subscription_plans (
  id text primary key,
  months int not null,
  price int not null,
  chatbot_daily_limit int default 10,
  is_active boolean default true,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table coupons (
  id uuid primary key,
  code text unique,
  discount_percent int,
  max_uses int,
  expires_at timestamptz,
  is_active boolean default true,
  created_at timestamptz default now()
);

create table user_subscriptions (
  id uuid primary key,
  user_id uuid references users(id),
  plan_id text references subscription_plans(id),
  starts_at timestamptz,
  ends_at timestamptz,
  status text,
  auto_renew boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table payments (
  id uuid primary key,
  user_id uuid references users(id),
  subscription_id uuid references user_subscriptions(id),
  provider text,
  order_id text,
  provider_event_id text unique,
  amount int,
  status text,
  created_at timestamptz default now()
);

create table coupon_redemptions (
  user_id uuid references users(id),
  coupon_id uuid references coupons(id),
  redeemed_at timestamptz default now(),
  primary key(user_id, coupon_id)
);
