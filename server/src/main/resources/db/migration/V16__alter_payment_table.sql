alter table payments
    add column currency varchar(10) default 'INR';

alter table payments
    add column payment_id text;

alter table payments
    add column updated_at timestamptz default now();

alter table payments
    add column metadata jsonb;