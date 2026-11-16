-- =============================================================================
-- Svaadanusaar Nutrition Engine - READ-ONLY PostgreSQL migration preflight
-- =============================================================================
-- PURPOSE   Establish the real state of the existing database before any V100
--           reconcile migration is designed. Every statement is a single
--           SELECT (or WITH ... SELECT). Nothing here changes data, schema,
--           session settings or transactions.
--
-- SAFETY    * No INSERT / UPDATE / DELETE / ALTER / DROP / CREATE statements.
--           * No session or transaction settings are touched.
--           * No user rows are returned: only counts and catalog metadata.
--             The only data values returned are (a) at most 20 distinct
--             UNEXPECTED enum strings in S5.6 (at most 10 per column; enum names,
--             not personal data) and (b) Flyway history rows in S1 (these include
--             installed_by, the database role name that applied each migration).
--           * Row counts use query_to_xml(), which executes a generated
--             SELECT count(*) ...; it cannot modify anything.
--           * No connection strings or credentials are read or printed.
--
-- HOW TO RUN
--           Prefer a READ-ONLY role or a read-only session started by your
--           client. Run the statements ONE AT A TIME, in order, and keep each
--           result. The first column of every result is the section tag.
--           Statements marked [CONDITIONAL] raise an error if the table they
--           read does not exist; that error is itself a finding.
--           The pack assumes the application schema is "public" and that it is on
--           the search_path (S0.1 shows it; regclass-to-text comparisons rely on it).
--
-- STATUS    This file has NOT been executed against any PostgreSQL server by
--           its author. Treat a syntax error as a defect in the pack, not as
--           a finding about the database.
-- =============================================================================


-- #############################################################################
-- S0  ENVIRONMENT, PRIVILEGES AND ROW-LEVEL SECURITY
-- #############################################################################

-- [S0.1] Server and session facts.
select 'S0.1' as section,
       current_database()                             as database_name,
       current_user                                   as connected_role,
       current_schema()                               as current_schema,
       current_setting('search_path')                 as search_path,
       current_setting('server_version')              as server_version,
       current_setting('server_version_num')::int     as server_version_num,
       current_setting('transaction_read_only')       as transaction_read_only,
       current_setting('default_transaction_read_only') as default_transaction_read_only,
       pg_is_in_recovery()                            as is_replica,
       current_setting('TimeZone')                    as session_timezone,
       now()                                          as checked_at;

-- [S0.2] Attributes of the connected role. rolbypassrls = false means row
-- counts in this pack may be FILTERED by row-level security (see S0.4).
select 'S0.2' as section,
       r.rolname, r.rolsuper, r.rolbypassrls, r.rolcreatedb, r.rolcreaterole,
       (select string_agg(m.rolname::text, ',' order by m.rolname)
          from pg_auth_members am
          join pg_roles m on m.oid = am.roleid
         where am.member = r.oid) as member_of
  from pg_roles r
 where r.rolname = current_user;

-- [S0.3] Extensions and gen_random_uuid() (needed by V31's column default).
select 'S0.3' as section, e.extname::text as name, e.extversion::text as value
  from pg_extension e
union all
select 'S0.3', 'function gen_random_uuid',
       case when exists (select 1 from pg_proc where proname = 'gen_random_uuid')
            then 'present' else 'ABSENT' end
 order by 2;

-- [S0.4] Every ordinary table in schema public: owner, RLS flags, SELECT
-- privilege and the planner's row estimate (an estimate only; exact counts are in S4.1 and S5.1).
select 'S0.4' as section,
       c.relname::text                  as table_name,
       pg_get_userbyid(c.relowner)      as owner,
       c.relrowsecurity                 as rls_enabled,
       c.relforcerowsecurity            as rls_forced,
       has_table_privilege(c.oid, 'SELECT') as can_select,
       c.reltuples::bigint              as planner_row_estimate
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where n.nspname = 'public' and c.relkind in ('r', 'p')
 order by c.relname;

-- [S0.5] Non-system schemas and how many tables each holds. Shows whether the
-- Flyway history or application tables live somewhere other than public.
select 'S0.5' as section,
       n.nspname::text as schema_name,
       count(c.oid) filter (where c.relkind in ('r', 'p')) as table_count
  from pg_namespace n
  left join pg_class c on c.relnamespace = n.oid
 where n.nspname not in ('pg_catalog', 'information_schema')
   and n.nspname not like 'pg\_toast%'
   and n.nspname not like 'pg\_temp%'
 group by n.nspname
 order by n.nspname;


-- #############################################################################
-- S1  FLYWAY HISTORY AND THE V17 CONTRADICTION
-- #############################################################################

-- [S1.1] Where is the Flyway history table? Expected: exactly one row.
select 'S1.1' as section, n.nspname::text as schema_name, c.relname::text as table_name
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where c.relname = 'flyway_schema_history' and c.relkind = 'r';

-- [S1.2] [CONDITIONAL on S1.1] Complete recorded history.
select 'S1.2' as section,
       installed_rank, version, description, type, script, checksum,
       installed_by, installed_on, execution_time, success
  from flyway_schema_history
 order by installed_rank;

-- [S1.3] [CONDITIONAL] History summary.
select 'S1.3' as section,
       count(*)                                       as rows_total,
       count(*) filter (where not success)            as failed_rows,
       max(installed_rank)                            as max_rank,
       max(case when version ~ '^[0-9]+$' then version::int end) as max_version,
       count(*) filter (where type <> 'SQL')          as non_sql_rows,
       string_agg(distinct type, ',')                 as row_types,
       min(installed_on)                              as first_install,
       max(installed_on)                              as last_install,
       count(*) filter (where version = '14')         as has_v14,
       count(*) filter (where version = '25')         as has_v25,
       count(*) filter (where version = '31')         as has_v31,
       count(*) filter (where version in ('32','33','34')) as has_v32_to_v34
  from flyway_schema_history;

-- [S1.3b] [CONDITIONAL] Version numbers with no history row (gaps).
select 'S1.3b' as section, g as missing_version
  from generate_series(
         1,
         (select max(case when version ~ '^[0-9]+$' then version::int end)
            from flyway_schema_history)) as g
 where not exists (select 1 from flyway_schema_history h where h.version = g::text)
 order by g;

-- [S1.3c] [CONDITIONAL] Rows applied out of version order.
select 'S1.3c' as section, installed_rank, version, prev_version, installed_on
  from (select h.installed_rank, h.version, h.installed_on,
               lag(case when h.version ~ '^[0-9]+$' then h.version::int end)
                 over (order by h.installed_rank) as prev_version
          from flyway_schema_history h) x
 where prev_version is not null
   and case when version ~ '^[0-9]+$' then version::int end < prev_version;

-- [S1.4] [CONDITIONAL] Recorded checksum versus the checksum of the CURRENT
-- migration file, computed with Flyway 11.14.1's own ChecksumCalculator
-- (org.flywaydb.core.internal.resolver.ChecksumCalculator) over the files in
-- server/src/main/resources/db/migration. See README for how to reproduce.
-- status: MATCH | CHECKSUM_MISMATCH | NOT_APPLIED | APPLIED_NO_LOCAL_FILE |
--         FAILED_IN_HISTORY
with local_files (version, script, local_checksum) as (
  values
  ('1','V1__users_and_auth.sql',1879323602),
  ('2','V2__subscriptions_and_payments.sql',2142666966),
  ('3','V3__health_and_diet.sql',-1012606189),
  ('4','V4__foods.sql',11311096),
  ('5','V5__tracking.sql',-45651504),
  ('6','V6__chat.sql',-205488242),
  ('7','V7__notifications.sql',-175113495),
  ('8','V8__admin.sql',1591318450),
  ('9','V9__seed_subscription_plans.sql',-2077127684),
  ('10','V10__change_origin_to_text.sql',1717824319),
  ('11','V11__add_status_column_to_diet_plans.sql',-1173383535),
  ('12','V12__create_body_weight_logs.sql',-760590705),
  ('13','V13__diet_hardening.sql',1417579068),
  ('14','V14__email_verification_fieldin_user.sql',0),
  ('15','V15__add_email_verification_columns.sql',-1393462706),
  ('16','V16__alter_payment_table.sql',-540935835),
  ('17','V17__convert_health_profile_to_jsonb.sql',1889251182),
  ('18','V18__add_weight_to_personal_profiles.sql',-1586800252),
  ('19','V19__add_activitylevel_to_nutritiontarget.sql',860085525),
  ('20','V20__alter_bmr_in_nutritiontarget.sql',824363197),
  ('21','V21__add_calculation_method_to_nutrition_target.sql',1200401744),
  ('22','V22__add_calorie_adjustment_to_nutritiontarget.sql',-1258553369),
  ('23','V23__target_calories.sql',-1127537240),
  ('24','V24__tdee.sql',1693486542),
  ('26','V26__add_razorpay_subscription_fields.sql',1816112028),
  ('27','V27__map_razorpay_plans.sql',-1525175778),
  ('28','V28__add_plan_and_razorpay_subscription_to_payments.sql',-739057486),
  ('29','V29__create_payment_webhook_events.sql',-1564130584),
  ('30','V30__remove_unique_constraint_from_payment_subscription.sql',440543863),
  ('31','V31__create_protein_adaptation_profiles.sql',-89381357),
  ('32','V32__add_training_status_to_workout_profiles.sql',-1426258696),
  ('33','V33__add_last_evaluated_on_to_protein_adaptation_profiles.sql',1803941477),
  ('34','V34__add_current_protein_grams_to_health_profiles.sql',-1175953462)
),
h as (
  select version, script, checksum, success, installed_rank, installed_on
    from flyway_schema_history
   where version is not null
)
select 'S1.4' as section,
       coalesce(l.version, h.version) as version,
       coalesce(l.script,  h.script)  as script,
       l.local_checksum,
       h.checksum                     as recorded_checksum,
       h.success,
       h.installed_on,
       case when h.version is null                     then 'NOT_APPLIED'
            when l.version is null                     then 'APPLIED_NO_LOCAL_FILE'
            when h.success is not true                 then 'FAILED_IN_HISTORY'
            when h.checksum is distinct from l.local_checksum then 'CHECKSUM_MISMATCH'
            else 'MATCH' end as status
  from local_files l
  full join h on h.version = l.version
 order by case when coalesce(l.version, h.version) ~ '^[0-9]+$'
               then coalesce(l.version, h.version)::int end;

-- [S1.5] [CONDITIONAL] V3 / V17 / V18 side by side. since_previous_row is the
-- time between consecutive rows in this filtered set (V3 -> V17 -> V18).
select 'S1.5' as section,
       version, description, script, checksum, success, installed_by,
       installed_on, execution_time,
       installed_on - lag(installed_on) over (order by installed_rank) as since_previous_row
  from flyway_schema_history
 where version in ('3', '17', '18')
 order by installed_rank;

-- [S1.6] [CONDITIONAL] Rows whose installed_on goes BACKWARDS relative to
-- installed_rank (evidence of manual history edits or restored dumps).
select 'S1.6' as section, installed_rank, version, installed_on, prev_installed_on
  from (select h.installed_rank, h.version, h.installed_on,
               lag(h.installed_on) over (order by h.installed_rank) as prev_installed_on
          from flyway_schema_history h) x
 where installed_on < prev_installed_on;


-- [S1.7] EFFECT PROBES: for each migration whose result can be observed in
-- the catalog, compare "does the effect exist" with "does history say it ran".
-- Verdicts:
--   consistent ...                         history and catalog agree
--   EFFECT PRESENT WITHOUT HISTORY ...     created by Hibernate ddl-auto=update or by hand
--   DRIFT: history says applied ...        a recorded migration whose effect is not there
--   pending ...                            not applied and not present (normal for V31-V34)
-- [CONDITIONAL on flyway_schema_history existing]
with c as (
  select table_name::text as t, column_name::text as col, data_type::text as dt
    from information_schema.columns
   where table_schema = 'public'
),
probe (version, what, expected_present, effect_present) as (
  values
  ('1',  'table users',                                           true,  to_regclass('public.users') is not null),
  ('3',  'table health_profiles',                                 true,  to_regclass('public.health_profiles') is not null),
  ('3',  'health_profiles.age (V3 legacy column)',                true,  exists (select 1 from c where t = 'health_profiles' and col = 'age')),
  ('3',  'table diet_plans',                                      true,  to_regclass('public.diet_plans') is not null),
  ('10', 'health_profiles.origin is text',                        true,  exists (select 1 from c where t = 'health_profiles' and col = 'origin' and dt = 'text')),
  ('11', 'diet_plans.status',                                     true,  exists (select 1 from c where t = 'diet_plans' and col = 'status')),
  ('12', 'weight_logs.logged_at',                                 true,  exists (select 1 from c where t = 'weight_logs' and col = 'logged_at')),
  ('12', 'index idx_body_weight_logs_user_time',                  true,  to_regclass('public.idx_body_weight_logs_user_time') is not null),
  ('13', 'index one_active_diet_per_user',                        true,  to_regclass('public.one_active_diet_per_user') is not null),
  ('13', 'constraint check_calories_positive',                    true,  exists (select 1 from pg_constraint where conname = 'check_calories_positive')),
  ('15', 'users.email_verified',                                  true,  exists (select 1 from c where t = 'users' and col = 'email_verified')),
  ('16', 'payments.currency',                                     true,  exists (select 1 from c where t = 'payments' and col = 'currency')),
  ('17', 'health_profiles.medical_conditions is jsonb',           true,  exists (select 1 from c where t = 'health_profiles' and col = 'medical_conditions' and dt = 'jsonb')),
  ('17', 'health_profiles.allergies is jsonb',                    true,  exists (select 1 from c where t = 'health_profiles' and col = 'allergies' and dt = 'jsonb')),
  ('17', 'health_profiles.allergies is still the V3 text[]',      false, exists (select 1 from c where t = 'health_profiles' and col = 'allergies' and dt = 'ARRAY')),
  ('17', 'index idx_health_profiles_medical_conditions',          true,  to_regclass('public.idx_health_profiles_medical_conditions') is not null),
  ('18', 'personal_profiles.weight_kg',                           true,  exists (select 1 from c where t = 'personal_profiles' and col = 'weight_kg')),
  ('19', 'nutrition_targets.activity_level',                      true,  exists (select 1 from c where t = 'nutrition_targets' and col = 'activity_level')),
  ('21', 'nutrition_targets.calculation_method',                  true,  exists (select 1 from c where t = 'nutrition_targets' and col = 'calculation_method')),
  ('22', 'nutrition_targets.calorie_adjustment',                  true,  exists (select 1 from c where t = 'nutrition_targets' and col = 'calorie_adjustment')),
  ('23', 'nutrition_targets.target_calories',                     true,  exists (select 1 from c where t = 'nutrition_targets' and col = 'target_calories')),
  ('26', 'subscription_plans.razorpay_plan_id',                   true,  exists (select 1 from c where t = 'subscription_plans' and col = 'razorpay_plan_id')),
  ('26', 'user_subscriptions.razorpay_subscription_id',           true,  exists (select 1 from c where t = 'user_subscriptions' and col = 'razorpay_subscription_id')),
  ('28', 'payments.plan_id',                                      true,  exists (select 1 from c where t = 'payments' and col = 'plan_id')),
  ('28', 'payments.razorpay_subscription_id',                     true,  exists (select 1 from c where t = 'payments' and col = 'razorpay_subscription_id')),
  ('29', 'table payment_webhook_events',                          true,  to_regclass('public.payment_webhook_events') is not null),
  ('30', 'index ux_payments_razorpay_subscription_id is GONE',    false, to_regclass('public.ux_payments_razorpay_subscription_id') is not null),
  ('31', 'table protein_adaptation_profiles',                     true,  to_regclass('public.protein_adaptation_profiles') is not null),
  ('32', 'workout_profiles.training_status',                      true,  exists (select 1 from c where t = 'workout_profiles' and col = 'training_status')),
  ('33', 'protein_adaptation_profiles.last_evaluated_on',         true,  exists (select 1 from c where t = 'protein_adaptation_profiles' and col = 'last_evaluated_on')),
  ('34', 'health_profiles.current_protein_grams',                 true,  exists (select 1 from c where t = 'health_profiles' and col = 'current_protein_grams'))
)
select 'S1.7' as section,
       p.version, p.what, p.expected_present, p.effect_present,
       case when h.version is null then 'no history row'
            when h.success        then 'applied'
            else 'FAILED' end as history,
       case when p.effect_present = p.expected_present and h.success is true
                 then 'consistent: applied and effect present'
            when p.effect_present = p.expected_present and h.version is null
                 then 'EFFECT PRESENT WITHOUT HISTORY (Hibernate update or manual change)'
            when p.effect_present <> p.expected_present and h.success is true
                 then 'DRIFT: history says applied, effect missing or unexpected'
            when p.effect_present <> p.expected_present and h.version is null
                 then 'pending: not applied and effect absent'
            else 'REVIEW' end as verdict
  from probe p
  left join flyway_schema_history h on h.version = p.version
 order by p.version::int, p.what;

-- [S1.8] The V3 / V10 / V17 columns of health_profiles as they exist now.
-- allergies is the decisive column: V3 created it as text[], V17 ADDs it as
-- jsonb. Both cannot succeed on the same table.
with expected (col, defined_by, expected_type) as (
  values
  ('age',                    'V3',        'integer'),
  ('gender',                 'V3',        'text'),
  ('height_cm',              'V3',        'integer'),
  ('weight_kg',              'V3',        'numeric'),
  ('goal',                   'V3',        'text'),
  ('training_level',         'V3',        'text'),
  ('activity_level',         'V3',        'text'),
  ('diet_type',              'V3',        'text'),
  ('meals_per_day',          'V3',        'integer'),
  ('origin',                 'V3 + V10',  'text'),
  ('allergies',              'V3 (text[]) AND V17 (jsonb)', 'ARRAY if V3 shape, jsonb if V17 shape'),
  ('diseases',               'V3',        'ARRAY'),
  ('medical_conditions',     'V17',       'jsonb'),
  ('medications',            'V17',       'jsonb'),
  ('preferred_cuisines',     'V17',       'jsonb'),
  ('taste_preferences',      'V17',       'jsonb'),
  ('favourite_foods',        'V17',       'jsonb'),
  ('disliked_foods',         'V17',       'jsonb'),
  ('religious_restrictions', 'V17',       'jsonb'),
  ('equipments',             'V17',       'jsonb'),
  ('current_supplements',    'V17',       'jsonb')
)
select 'S1.8' as section,
       e.col, e.defined_by, e.expected_type,
       c.data_type as actual_data_type, c.udt_name as actual_udt, c.is_nullable,
       case when c.column_name is null then 'ABSENT' else 'present' end as state
  from expected e
  left join information_schema.columns c
    on c.table_schema = 'public' and c.table_name = 'health_profiles' and c.column_name = e.col
 order by e.defined_by, e.col;

-- [S1.9] The ten legacy join tables that V17 drops (IF EXISTS). Existence and
-- exact row count. A table that still exists was either created after V17
-- or V17 never ran against this database.
with names (n) as (
  values ('health_profile_medical_conditions'), ('health_profile_allergies'),
         ('health_profile_medications'), ('health_profile_preferred_cuisines'),
         ('health_profile_taste_preferences'), ('health_profile_favourite_foods'),
         ('health_profile_disliked_foods'), ('health_profile_religious_restrictions'),
         ('cooking_profile_equipments'), ('supplement_profile_current_supplements')
),
ex as (select n from names where to_regclass(format('public.%I', n)) is not null)
select 'S1.9' as section, names.n as legacy_table,
       (ex.n is not null) as exists_now,
       case when ex.n is not null then
         (xpath('/row/c/text()',
                query_to_xml(format('select count(*) as c from public.%I', ex.n), true, true, '')))[1]::text::bigint
       end as row_count,
       case when names.n = 'supplement_profile_current_supplements'
            then 'NOTE: also a CURRENT JPA collection table - see S4'
            end as note
  from names left join ex on ex.n = names.n
 order by names.n;

-- [S1.10] Catalog evidence of churn on health_profiles: dropped columns,
-- highest attribute number versus live columns, and V17's COMMENT ON COLUMN.
select 'S1.10' as section,
       count(*) filter (where a.attnum > 0 and not a.attisdropped) as live_columns,
       count(*) filter (where a.attnum > 0 and a.attisdropped)     as dropped_columns,
       max(a.attnum)                                               as highest_attnum,
       count(*) filter (where a.attnum > 0 and not a.attisdropped
                          and col_description(a.attrelid, a.attnum) is not null) as columns_with_comment,
       string_agg(a.attname::text, ',' order by a.attnum)
         filter (where a.attnum > 0 and not a.attisdropped
                   and col_description(a.attrelid, a.attnum) like '%stored as JSONB array%') as v17_commented_columns
  from pg_attribute a
 where a.attrelid = to_regclass('public.health_profiles');

-- [S1.11] Creation-order proxy: relation OIDs are assigned increasing. Compare
-- where health_profiles sits relative to the Flyway tables (V1-V8) and the
-- Hibernate-only tables. INFORMATIONAL ONLY: OIDs are renumbered by dump/restore.
select 'S1.11' as section, c.oid::bigint as oid, c.relname::text as table_name,
       case when c.relname in ('users','user_auth','subscription_plans','coupons','user_subscriptions','payments',
                               'coupon_redemptions','health_profiles','diet_plans','foods','meal_logs','weight_logs',
                               'daily_metrics','streaks','chat_usage','chat_messages','notification_preferences',
                               'notification_logs','admin_users','admin_audit_logs')
            then 'created by Flyway V1-V8 (by design)'
            else 'not created by any migration' end as expected_origin
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where n.nspname = 'public' and c.relkind = 'r'
 order by c.oid;


-- #############################################################################
-- S2  health_profiles
-- #############################################################################

-- [S2.1] Every column, in physical order.
select 'S2.1' as section,
       c.ordinal_position, c.column_name, c.data_type, c.udt_name, c.is_nullable,
       c.column_default, c.character_maximum_length, c.numeric_precision, c.numeric_scale
  from information_schema.columns c
 where c.table_schema = 'public' and c.table_name = 'health_profiles'
 order by c.ordinal_position;

-- [S2.2] Every constraint (primary key, unique, foreign key, check).
select 'S2.2' as section,
       c.conname::text as constraint_name,
       case c.contype when 'p' then 'PRIMARY KEY' when 'u' then 'UNIQUE'
                      when 'f' then 'FOREIGN KEY'  when 'c' then 'CHECK'
                      when 'x' then 'EXCLUDE'      else c.contype::text end as kind,
       pg_get_constraintdef(c.oid) as definition,
       c.convalidated as validated
  from pg_constraint c
 where c.conrelid = to_regclass('public.health_profiles')
 order by kind, c.conname;

-- [S2.3] Every index, including unique indexes that back no constraint.
select 'S2.3' as section, i.indexname, i.indexdef
  from pg_indexes i
 where i.schemaname = 'public' and i.tablename = 'health_profiles'
 order by i.indexname;

-- [S2.4] THE KEY FACTS (catalog-only, never errors). Answers: does id exist,
-- what is the primary key, is user_id unique, does version exist, can V31's
-- foreign key to health_profiles(id) be created.
with hp as (select to_regclass('public.health_profiles') as oid),
cols as (
  select a.attname::text as col
    from pg_attribute a, hp
   where a.attrelid = hp.oid and a.attnum > 0 and not a.attisdropped
),
pk as (
  select string_agg(a.attname::text, ',' order by k.ord) as cols
    from hp
    join pg_constraint c on c.conrelid = hp.oid and c.contype = 'p'
    cross join lateral unnest(c.conkey) with ordinality as k(attnum, ord)
    join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.attnum
),
u1 as (   -- single-column, non-partial, non-expression unique indexes (includes the PK)
  select a.attname::text as col, i.indisprimary as is_pk
    from hp
    join pg_index i on i.indrelid = hp.oid
    join pg_attribute a on a.attrelid = i.indrelid and a.attnum = i.indkey[0]
   where i.indisunique and i.indnkeyatts = 1 and i.indpred is null and i.indexprs is null
)
select 'S2.4' as section, fact, value, meaning
  from (
    select 1 as ord, 'table health_profiles exists' as fact,
           (select oid is not null from hp)::text as value,
           'false means the table is missing: STOP, every later health_profiles section errors' as meaning
    union all select 2, 'column id exists',
           exists (select 1 from cols where col = 'id')::text,
           'JPA (BaseEntity) maps the key to id. false = V3 shape; HealthProfile cannot be loaded'
    union all select 3, 'column user_id exists',
           exists (select 1 from cols where col = 'user_id')::text,
           'required (nullable=false, unique=true) by HealthProfile.user'
    union all select 4, 'column version exists',
           exists (select 1 from cols where col = 'version')::text,
           'BaseEntity @Version; a missing column breaks every update'
    union all select 5, 'primary key columns',
           coalesce((select cols from pk), '<none>'),
           'expected: id. user_id means V3 shape (BLOCKS V31 FK to health_profiles(id))'
    union all select 6, 'id is primary key or single-column unique',
           exists (select 1 from u1 where col = 'id')::text,
           'must be true before any FK may reference health_profiles(id) (V31, every profile table)'
    union all select 7, 'user_id is primary key or single-column unique',
           exists (select 1 from u1 where col = 'user_id')::text,
           'JPA expects a unique user_id (one profile per user)'
    union all select 8, 'id is the primary key',
           exists (select 1 from u1 where col = 'id' and is_pk)::text,
           'expected true'
    union all select 9, 'live columns',
           (select count(*) from cols)::text,
           'entity maps 8; anything beyond that is legacy (see S5.7)'
    union all select 10, 'columns not mapped by HealthProfile.java',
           coalesce((select string_agg(col, ',' order by col) from cols
                      where col not in ('id','user_id','version','created_at','updated_at',
                                        'onboarding_completed','profile_completion_percentage',
                                        'current_protein_grams')), '<none>'),
           'legacy columns; never dropped by this pack (their data is measured in S5.7)'
  ) f
 order by ord;

-- [S2.5] Actual versus HealthProfile.java (+ BaseEntity + CurrentNutritionProfile).
with expected (col, jpa_type, jpa_not_null) as (
  values ('id','uuid',true), ('user_id','uuid',true), ('version','bigint',false),
         ('created_at','timestamp without time zone',true), ('updated_at','timestamp without time zone',true),
         ('onboarding_completed','boolean',true), ('profile_completion_percentage','integer',true),
         ('current_protein_grams','numeric',false)
),
actual as (
  select column_name::text as col, data_type::text as dt, is_nullable::text as nullable
    from information_schema.columns
   where table_schema = 'public' and table_name = 'health_profiles'
)
select 'S2.5' as section,
       coalesce(e.col, a.col) as column_name,
       e.jpa_type, a.dt as actual_type, e.jpa_not_null, a.nullable as actual_nullable,
       case when e.col is null then 'EXTRA: not mapped by JPA (legacy)'
            when a.col is null then 'MISSING COLUMN'
            when a.dt <> e.jpa_type then 'TYPE MISMATCH'
            when e.jpa_not_null and a.nullable = 'YES' then 'NULLABILITY: JPA requires NOT NULL, DB allows NULL'
            else 'ok' end as status
  from expected e
  full join actual a on a.col = e.col
 order by status, 2;

-- [S2.6] Foreign keys into and out of health_profiles with their delete rule.
select 'S2.6' as section,
       c.conname::text as constraint_name,
       c.conrelid::regclass::text  as from_table,
       (select string_agg(a.attname::text, ',' order by k.ord)
          from unnest(c.conkey) with ordinality k(attnum, ord)
          join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.attnum) as from_columns,
       c.confrelid::regclass::text as to_table,
       (select string_agg(a.attname::text, ',' order by k.ord)
          from unnest(c.confkey) with ordinality k(attnum, ord)
          join pg_attribute a on a.attrelid = c.confrelid and a.attnum = k.attnum) as to_columns,
       case c.confdeltype when 'a' then 'NO ACTION' when 'r' then 'RESTRICT' when 'c' then 'CASCADE'
                          when 'n' then 'SET NULL'  when 'd' then 'SET DEFAULT' end as on_delete,
       c.convalidated as validated
  from pg_constraint c
 where c.contype = 'f'
   and (c.conrelid = to_regclass('public.health_profiles') or c.confrelid = to_regclass('public.health_profiles'))
 order by to_table, from_table, constraint_name;

-- [S2.7a] Always valid: row count and user_id uniqueness.
select 'S2.7a' as section,
       count(*)                  as rows_total,
       count(user_id)            as rows_with_user_id,
       count(distinct user_id)   as distinct_user_ids,
       count(*) - count(distinct user_id) as duplicate_or_null_user_rows
  from public.health_profiles;

-- [S2.7b] [CONDITIONAL: only if S2.4 says id AND version exist] key quality.
select 'S2.7b' as section,
       count(*)                                  as rows_total,
       count(*) filter (where id is null)        as null_id,
       count(*) filter (where id = user_id)      as id_equals_user_id,
       count(*) filter (where id <> user_id)     as id_differs_from_user_id,
       count(distinct id)                        as distinct_ids,
       count(*) filter (where version is null)   as null_version
  from public.health_profiles;


-- #############################################################################
-- S3  V31-V34 READINESS
-- #############################################################################

-- [S3.1] Does each object V31-V34 would create already exist?
-- If yes, running that migration as written FAILS (none of them use IF NOT EXISTS).
with c as (
  select table_name::text as t, column_name::text as col, data_type::text as dt,
         is_nullable::text as nullable, character_maximum_length as cml,
         numeric_precision as np, numeric_scale as ns, column_default as cd
    from information_schema.columns
   where table_schema = 'public'
),
items (mig, kind, t, col) as (
  values
  ('V31', 'creates table',                  'protein_adaptation_profiles', null),
  ('V32', 'prerequisite table',             'workout_profiles',            null),
  ('V32', 'adds column',                    'workout_profiles',            'training_status'),
  ('V33', 'adds column',                    'protein_adaptation_profiles', 'last_evaluated_on'),
  ('V34', 'adds column',                    'health_profiles',             'current_protein_grams'),
  ('V34', 'legacy column left untouched',   'health_profiles',             'current_protein_intake')
)
select 'S3.1' as section, i.mig, i.kind, i.t as table_name, i.col as column_name,
       case when i.col is null then to_regclass(format('public.%I', i.t)) is not null
            else c.col is not null end as exists_now,
       c.dt as data_type, c.nullable, c.cml as char_max_length, c.np as num_precision, c.ns as num_scale, c.cd as column_default,
       case
         when i.kind = 'legacy column left untouched'
              then 'informational: ' || case when c.col is null then 'legacy column absent' else 'legacy column present (never touched)' end
         when i.kind = 'prerequisite table' and to_regclass(format('public.%I', i.t)) is null
              then 'BLOCKS ' || i.mig || ': prerequisite table does not exist'
         when i.kind = 'prerequisite table'
              then 'prerequisite satisfied'
         when i.col is null and to_regclass(format('public.%I', i.t)) is not null
              then 'RUNNING ' || i.mig || ' AS WRITTEN WOULD FAIL: table already exists'
         when i.col is not null and c.col is not null
              then 'RUNNING ' || i.mig || ' AS WRITTEN WOULD FAIL: column already exists'
         else 'object absent: statement would proceed' end as verdict
  from items i
  left join c on c.t = i.t and c.col = i.col
 order by i.mig, i.kind;

-- [S3.2] Columns of protein_adaptation_profiles. V31 defines 13; the entity maps the same 13.
select 'S3.2' as section,
       c.ordinal_position, c.column_name, c.data_type, c.is_nullable, c.column_default,
       c.numeric_precision, c.numeric_scale
  from information_schema.columns c
 where c.table_schema = 'public' and c.table_name = 'protein_adaptation_profiles'
 order by c.ordinal_position;

-- [S3.3] Constraints on protein_adaptation_profiles: key, FK target and DELETE RULE,
-- uniqueness of health_profile_id, and the CHECK constraints V31 defines.
select 'S3.3' as section,
       c.conname::text as constraint_name,
       case c.contype when 'p' then 'PRIMARY KEY' when 'u' then 'UNIQUE'
                      when 'f' then 'FOREIGN KEY'  when 'c' then 'CHECK' else c.contype::text end as kind,
       pg_get_constraintdef(c.oid) as definition,
       case when c.contype = 'f' then c.confrelid::regclass::text end as references_table,
       case when c.contype = 'f' then
         (select string_agg(a.attname::text, ',' order by k.ord)
            from unnest(c.confkey) with ordinality k(attnum, ord)
            join pg_attribute a on a.attrelid = c.confrelid and a.attnum = k.attnum) end as references_columns,
       case when c.contype = 'f' then
         case c.confdeltype when 'a' then 'NO ACTION' when 'r' then 'RESTRICT' when 'c' then 'CASCADE'
                            when 'n' then 'SET NULL'  when 'd' then 'SET DEFAULT' end end as on_delete,
       c.convalidated as validated
  from pg_constraint c
 where c.conrelid = to_regclass('public.protein_adaptation_profiles')
 order by kind, c.conname;

-- [S3.4] Indexes on protein_adaptation_profiles (V31 defines a PK, a UNIQUE constraint AND a duplicate unique index).
select 'S3.4' as section, i.indexname, i.indexdef
  from pg_indexes i
 where i.schemaname = 'public' and i.tablename = 'protein_adaptation_profiles'
 order by i.indexname;

-- [S3.5] The decisive uniqueness fact for the first-insert race fix: is there
-- a single-column unique guarantee on protein_adaptation_profiles.health_profile_id?
select 'S3.5' as section,
       exists (
         select 1
           from pg_index i
           join pg_attribute a on a.attrelid = i.indrelid and a.attnum = i.indkey[0]
          where i.indrelid = to_regclass('public.protein_adaptation_profiles')
            and i.indisunique and i.indnkeyatts = 1 and i.indpred is null and i.indexprs is null
            and a.attname = 'health_profile_id'
       ) as health_profile_id_is_unique;

-- [S3.6] [CONDITIONAL: table must exist] Protein rows: counts only, no user data.
select 'S3.6' as section,
       count(*)                                              as rows_total,
       count(*) filter (where current_protein_grams is null) as baseline_null,
       count(*) filter (where version is null)               as null_version,
       count(*) filter (where adaptation_stage = 'NOT_STARTED')    as not_started,
       count(*) filter (where adaptation_stage = 'STARTING')       as starting,
       count(*) filter (where adaptation_stage = 'ADAPTING')       as adapting,
       count(*) filter (where adaptation_stage = 'TARGET_REACHED') as target_reached,
       count(*) filter (where adaptation_stage is null or adaptation_stage not in
                        ('NOT_STARTED','STARTING','ADAPTING','TARGET_REACHED')) as unexpected_stage
  from public.protein_adaptation_profiles;


-- #############################################################################
-- S4  THE 19 HIBERNATE-ONLY TABLES, nutrition_targets AND FULL JPA COLUMN DRIFT
-- #############################################################################
-- The 19 tables are the ten entity tables (nutrition_targets included) plus
-- the nine element-collection tables that no Flyway migration creates.
-- Row counts use query_to_xml(): a generated "select count(*)", read-only.
-- If S0.2 shows rolbypassrls = false AND S0.4 shows rls_enabled = true for a
-- table, its count may be filtered; treat that count as a LOWER BOUND.

-- [S4.1] One row per table: existence, EXACT row count, and key/constraint/index counts.
with t (table_name, kind) as (
  values
  ('personal_profiles', 'entity'), ('fitness_goal_profiles', 'entity'),
  ('nutrition_preference_profiles', 'entity'), ('workout_profiles', 'entity'),
  ('activity_profiles', 'entity'), ('medical_profiles', 'entity'),
  ('cooking_profiles', 'entity'), ('lifestyle_preference_profiles', 'entity'),
  ('supplement_profiles', 'entity'), ('nutrition_targets', 'entity (unused by code)'),
  ('medical_profile_conditions', 'collection'), ('medical_profile_food_allergies', 'collection'),
  ('medical_profile_medications', 'collection'), ('cooking_profile_equipment', 'collection'),
  ('nutrition_preferred_cuisines', 'collection'), ('nutrition_taste_preferences', 'collection'),
  ('nutrition_preferred_protein_sources', 'collection'), ('nutrition_religious_restrictions', 'collection'),
  ('supplement_profile_current_supplements', 'collection')
)
select 'S4.1' as section, t.table_name, t.kind,
       (to_regclass(format('public.%I', t.table_name)) is not null) as exists_now,
       case when to_regclass(format('public.%I', t.table_name)) is not null then
         (xpath('/row/c/text()',
                query_to_xml(format('select count(*) as c from public.%I', t.table_name), true, true, '')))[1]::text::bigint
       end as row_count,
       (select string_agg(a.attname::text, ',' order by k.ord)
          from pg_constraint c
          cross join lateral unnest(c.conkey) with ordinality k(attnum, ord)
          join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.attnum
         where c.conrelid = to_regclass(format('public.%I', t.table_name)) and c.contype = 'p') as primary_key,
       (select count(*) from pg_constraint c where c.conrelid = to_regclass(format('public.%I', t.table_name)) and c.contype = 'f') as fk_count,
       (select count(*) from pg_constraint c where c.conrelid = to_regclass(format('public.%I', t.table_name)) and c.contype = 'u') as unique_constraints,
       (select count(*) from pg_constraint c where c.conrelid = to_regclass(format('public.%I', t.table_name)) and c.contype = 'c') as check_constraints,
       (select count(*) from pg_indexes i where i.schemaname = 'public' and i.tablename = t.table_name) as index_count
  from t
 order by (to_regclass(format('public.%I', t.table_name)) is null) desc, t.kind, t.table_name;

-- [S4.2] Primary keys: actual versus Hibernate's expected DDL.
-- Expected: the 10 entity tables key on id; user-owned collection tables key on (owner_id, value)
-- for the four NOT-NULL collections and have NO primary key for the five nullable ones.
with expected (table_name, pk_columns) as (
  values
    ('activity_profiles','id'),
    ('cooking_profiles','id'),
    ('fitness_goal_profiles','id'),
    ('health_profiles','id'),
    ('lifestyle_preference_profiles','id'),
    ('medical_profiles','id'),
    ('nutrition_preference_profiles','id'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id,cuisine'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id,protein_source'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id,religious_restriction'),
    ('nutrition_targets','id'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id,taste_preference'),
    ('payment_webhook_events','id'),
    ('payments','id'),
    ('personal_profiles','id'),
    ('protein_adaptation_profiles','id'),
    ('subscription_plans','id'),
    ('supplement_profiles','id'),
    ('user_auth','user_id'),
    ('user_subscriptions','id'),
    ('users','id'),
    ('workout_profiles','id')
),
tables_of_interest (table_name) as (
  values ('personal_profiles'), ('fitness_goal_profiles'), ('nutrition_preference_profiles'),
         ('workout_profiles'), ('activity_profiles'), ('medical_profiles'), ('cooking_profiles'),
         ('lifestyle_preference_profiles'), ('supplement_profiles'), ('nutrition_targets'),
         ('medical_profile_conditions'), ('medical_profile_food_allergies'), ('medical_profile_medications'),
         ('cooking_profile_equipment'), ('nutrition_preferred_cuisines'), ('nutrition_taste_preferences'),
         ('nutrition_preferred_protein_sources'), ('nutrition_religious_restrictions'),
         ('supplement_profile_current_supplements')
),
actual as (
  select c.conrelid::regclass::text as table_name,
         string_agg(a.attname::text, ',' order by k.ord) as pk_columns
    from pg_constraint c
    cross join lateral unnest(c.conkey) with ordinality k(attnum, ord)
    join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.attnum
   where c.contype = 'p' and c.connamespace = 'public'::regnamespace
   group by c.conrelid
)
select 'S4.2' as section, t.table_name,
       to_regclass(format('public.%I', t.table_name)) is not null as exists_now,
       e.pk_columns as expected_pk, a.pk_columns as actual_pk,
       case when to_regclass(format('public.%I', t.table_name)) is null then 'TABLE ABSENT'
            when e.pk_columns is null and a.pk_columns is null then 'ok (no PK expected, none present)'
            when e.pk_columns is null and a.pk_columns is not null then 'info: extra PK present'
            when a.pk_columns is null then 'MISSING PRIMARY KEY'
            when a.pk_columns = e.pk_columns then 'ok'
            else 'PK DIFFERS' end as status
  from tables_of_interest t
  left join expected e on e.table_name = t.table_name
  left join actual   a on a.table_name = t.table_name
 order by status, t.table_name;

-- [S4.3] Foreign keys on every JPA table (the 19 plus health_profiles, protein_adaptation_profiles
-- and the Flyway-created users/payments/etc.): actual versus expected, with DELETE RULE.
-- Hibernate's generated FKs reference the target's primary key and use NO ACTION (no cascade).
-- Only the FIRST column of each FK is compared; none of these FKs is composite.
with expected (child_table, child_column, parent_table, hibernate_name) as (
  values
    ('activity_profiles','workout_profile_id','workout_profiles','FKoe08h21upnnrqymscefcqpspg'),
    ('cooking_profile_equipment','cooking_profile_id','cooking_profiles','FKt6capcee7jfvijh464h27pk2b'),
    ('cooking_profiles','health_profile_id','health_profiles','FKatxq86eecq7po29v3dpt5kj6t'),
    ('fitness_goal_profiles','health_profile_id','health_profiles','FKswsiergey4b3tfe20dfcny8uo'),
    ('health_profiles','user_id','users','FK3tevdso70nrymmg3rgy76jqga'),
    ('lifestyle_preference_profiles','health_profile_id','health_profiles','FKp7qm6cb0juus03065ee37hdm5'),
    ('medical_profile_conditions','medical_profile_id','medical_profiles','FKita44wf1siheut54mu4fdd4kh'),
    ('medical_profile_food_allergies','medical_profile_id','medical_profiles','FK58k4ptdomqa2cg38tao58cwwk'),
    ('medical_profile_medications','medical_profile_id','medical_profiles','FKsne6qk9o7lx2jr45h169quvhe'),
    ('medical_profiles','health_profile_id','health_profiles','FKai2hiaa04u19oefkeq79rdw46'),
    ('nutrition_preference_profiles','health_profile_id','health_profiles','FK2flkraqih8n013xi9ohsottff'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id','nutrition_preference_profiles','FKs9cpieyfsfggy37u5xbcpr198'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id','nutrition_preference_profiles','FKpca5dnd50124u6skpvhcn64pa'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id','nutrition_preference_profiles','FKr49sriujl8k79n2haygc6juls'),
    ('nutrition_targets','health_profile_id','health_profiles','FKesp92d6cyltrkkrabkgigf34y'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id','nutrition_preference_profiles','FK3binc14qbr7m5m727qmnllut7'),
    ('personal_profiles','health_profile_id','health_profiles','FKkgrwmce08f9rij398w1b1n6r9'),
    ('protein_adaptation_profiles','health_profile_id','health_profiles','FK3co2sytmx44jr3rineyr12di9'),
    ('supplement_profile_current_supplements','supplement_profile_id','supplement_profiles','FK4s51mkjypku5enxlq5pxooc95'),
    ('supplement_profiles','health_profile_id','health_profiles','FKk4atusv2nwlc2lvip0klpf6ia'),
    ('user_auth','user_id','users','FK9hog4tmdalcddidanohglx6gl'),
    ('workout_profiles','health_profile_id','health_profiles','FKpjckhoe1l7utdw2tkx6fgsfu')
),
actual as (
  select c.conrelid::regclass::text as child_table,
         (select a.attname::text from pg_attribute a where a.attrelid = c.conrelid and a.attnum = c.conkey[1]) as child_column,
         c.confrelid::regclass::text as parent_table,
         (select string_agg(a.attname::text, ',' order by k.ord)
            from unnest(c.confkey) with ordinality k(attnum, ord)
            join pg_attribute a on a.attrelid = c.confrelid and a.attnum = k.attnum) as parent_columns,
         case c.confdeltype when 'a' then 'NO ACTION' when 'r' then 'RESTRICT' when 'c' then 'CASCADE'
                            when 'n' then 'SET NULL'  when 'd' then 'SET DEFAULT' end as on_delete,
         c.conname::text as actual_name, c.convalidated as validated
    from pg_constraint c
   where c.contype = 'f' and c.connamespace = 'public'::regnamespace
)
select 'S4.3' as section,
       coalesce(e.child_table, a.child_table)   as child_table,
       coalesce(e.child_column, a.child_column) as child_column,
       coalesce(e.parent_table, a.parent_table) as parent_table,
       a.parent_columns, a.on_delete, a.actual_name, a.validated,
       case when e.child_table is null then 'info: FK not in JPA model (e.g. Flyway-created table)'
            when a.child_table is null then 'MISSING FOREIGN KEY'
            when a.parent_columns is distinct from 'id' then 'FK TARGET COLUMN IS NOT id (' || a.parent_columns || ')'
            when a.on_delete is distinct from 'NO ACTION' then 'DELETE RULE DIFFERS (actual ' || a.on_delete || ', Hibernate expects NO ACTION)'
            when a.validated is not true then 'NOT VALIDATED'
            else 'ok' end as status
  from expected e
  full join actual a
    on a.child_table = e.child_table and a.child_column = e.child_column and a.parent_table = e.parent_table
 where coalesce(e.child_table, a.child_table) in (select child_table from expected union select 'protein_adaptation_profiles')
 order by status, child_table, child_column;

-- [S4.4] Unique guarantees: actual single-column uniqueness (constraint OR unique index) versus
-- Hibernate's expected UNIQUE columns.
with expected (table_name, column_name) as (
  values
    ('cooking_profiles','health_profile_id'),
    ('fitness_goal_profiles','health_profile_id'),
    ('health_profiles','user_id'),
    ('lifestyle_preference_profiles','health_profile_id'),
    ('medical_profiles','health_profile_id'),
    ('nutrition_preference_profiles','health_profile_id'),
    ('nutrition_targets','health_profile_id'),
    ('payment_webhook_events','provider_event_id'),
    ('personal_profiles','health_profile_id'),
    ('protein_adaptation_profiles','health_profile_id'),
    ('supplement_profiles','health_profile_id'),
    ('users','email'),
    ('users','phone'),
    ('workout_profiles','health_profile_id')
),
actual as (
  select i.indrelid::regclass::text as table_name, a.attname::text as column_name,
         bool_or(i.indisprimary) as is_pk,
         string_agg(ic.relname::text, ',') as via_indexes
    from pg_index i
    join pg_class ic on ic.oid = i.indexrelid
    join pg_attribute a on a.attrelid = i.indrelid and a.attnum = i.indkey[0]
   where i.indisunique and i.indnkeyatts = 1 and i.indpred is null and i.indexprs is null
     and ic.relnamespace = 'public'::regnamespace
   group by i.indrelid, a.attname
)
select 'S4.4' as section,
       coalesce(e.table_name, a.table_name) as table_name,
       coalesce(e.column_name, a.column_name) as column_name,
       a.is_pk, a.via_indexes,
       case when e.table_name is null then 'info: unique in DB, not unique in JPA model'
            when a.table_name is null    then 'MISSING UNIQUE GUARANTEE'
            else 'ok' end as status
  from expected e
  full join actual a on a.table_name = e.table_name and a.column_name = e.column_name
 where (coalesce(e.table_name, a.table_name) in (select table_name from expected)
    or coalesce(e.table_name, a.table_name) in
       ('protein_adaptation_profiles','health_profiles','nutrition_targets'))
   and (e.table_name is not null or not a.is_pk)
 order by status, table_name, column_name;

-- [S4.5] Every index on the 19 tables + health_profiles + protein_adaptation_profiles
-- (full definitions, to spot redundant duplicates such as V31's UNIQUE constraint + unique index).
select 'S4.5' as section, i.tablename as table_name, i.indexname, i.indexdef
  from pg_indexes i
 where i.schemaname = 'public'
   and i.tablename in ('personal_profiles','fitness_goal_profiles','nutrition_preference_profiles',
        'workout_profiles','activity_profiles','medical_profiles','cooking_profiles',
        'lifestyle_preference_profiles','supplement_profiles','nutrition_targets',
        'medical_profile_conditions','medical_profile_food_allergies','medical_profile_medications',
        'cooking_profile_equipment','nutrition_preferred_cuisines','nutrition_taste_preferences',
        'nutrition_preferred_protein_sources','nutrition_religious_restrictions',
        'supplement_profile_current_supplements','health_profiles','protein_adaptation_profiles')
 order by i.tablename, i.indexname;

-- [S4.6] FULL JPA COLUMN DRIFT across all 27 mapped tables (200 expected columns, generated by
-- Hibernate 7.2.0.Final from the compiled entities with Spring Boot 4.0.1's naming strategy).
-- Statuses:
--   MISSING TABLE / MISSING COLUMN   hibernate validate fails, or inserts fail.
--   TYPE MISMATCH                    hibernate validate fails.
--   JPA NOT NULL, DB NULLABLE        validate passes; V100 must backfill before adding NOT NULL (measured in S5.5).
--   DB NOT NULL, JPA NULLABLE        validate passes; INSERTS FAIL when the entity leaves it null.
--   EXTRA COLUMN                     informational: unmapped column (legacy); never dropped by the pack.
with expected (table_name, column_name, jpa_not_null, jpa_type) as (
  values
    ('activity_profiles','days_per_week',true,'integer'),
    ('activity_profiles','duration_minutes',true,'integer'),
    ('activity_profiles','created_at',true,'timestamp without time zone'),
    ('activity_profiles','updated_at',true,'timestamp without time zone'),
    ('activity_profiles','version',false,'bigint'),
    ('activity_profiles','id',true,'uuid'),
    ('activity_profiles','workout_profile_id',true,'uuid'),
    ('activity_profiles','activity_type',true,'character varying'),
    ('activity_profiles','experience_level',true,'character varying'),
    ('activity_profiles','sport_type',false,'character varying'),
    ('activity_profiles','workout_intensity',true,'character varying'),
    ('cooking_profile_equipment','cooking_profile_id',true,'uuid'),
    ('cooking_profile_equipment','equipment',false,'character varying'),
    ('cooking_profiles','cooking_time_minutes',true,'integer'),
    ('cooking_profiles','meal_prep',true,'boolean'),
    ('cooking_profiles','created_at',true,'timestamp without time zone'),
    ('cooking_profiles','updated_at',true,'timestamp without time zone'),
    ('cooking_profiles','version',false,'bigint'),
    ('cooking_profiles','health_profile_id',true,'uuid'),
    ('cooking_profiles','id',true,'uuid'),
    ('cooking_profiles','budget',true,'character varying'),
    ('cooking_profiles','cooking_skill',true,'character varying'),
    ('fitness_goal_profiles','cheat_meals_per_week',true,'integer'),
    ('fitness_goal_profiles','include_cheat_meals',true,'boolean'),
    ('fitness_goal_profiles','target_date',false,'date'),
    ('fitness_goal_profiles','target_weight_kg',false,'double precision'),
    ('fitness_goal_profiles','weekly_weight_change_kg',false,'double precision'),
    ('fitness_goal_profiles','created_at',true,'timestamp without time zone'),
    ('fitness_goal_profiles','updated_at',true,'timestamp without time zone'),
    ('fitness_goal_profiles','version',false,'bigint'),
    ('fitness_goal_profiles','health_profile_id',true,'uuid'),
    ('fitness_goal_profiles','id',true,'uuid'),
    ('fitness_goal_profiles','diet_flexibility',false,'character varying'),
    ('fitness_goal_profiles','diet_strictness',false,'character varying'),
    ('fitness_goal_profiles','primary_goal',true,'character varying'),
    ('fitness_goal_profiles','secondary_goal',false,'character varying'),
    ('health_profiles','current_protein_grams',false,'numeric'),
    ('health_profiles','onboarding_completed',true,'boolean'),
    ('health_profiles','profile_completion_percentage',true,'integer'),
    ('health_profiles','created_at',true,'timestamp without time zone'),
    ('health_profiles','updated_at',true,'timestamp without time zone'),
    ('health_profiles','version',false,'bigint'),
    ('health_profiles','id',true,'uuid'),
    ('health_profiles','user_id',true,'uuid'),
    ('lifestyle_preference_profiles','food_delivery_available',true,'boolean'),
    ('lifestyle_preference_profiles','hosteller',true,'boolean'),
    ('lifestyle_preference_profiles','office_lunch_available',true,'boolean'),
    ('lifestyle_preference_profiles','travels_frequently',true,'boolean'),
    ('lifestyle_preference_profiles','created_at',true,'timestamp without time zone'),
    ('lifestyle_preference_profiles','updated_at',true,'timestamp without time zone'),
    ('lifestyle_preference_profiles','version',false,'bigint'),
    ('lifestyle_preference_profiles','health_profile_id',true,'uuid'),
    ('lifestyle_preference_profiles','id',true,'uuid'),
    ('medical_profile_conditions','medical_profile_id',true,'uuid'),
    ('medical_profile_conditions','medical_condition',false,'character varying'),
    ('medical_profile_food_allergies','medical_profile_id',true,'uuid'),
    ('medical_profile_food_allergies','food_allergy',false,'character varying'),
    ('medical_profile_medications','medical_profile_id',true,'uuid'),
    ('medical_profile_medications','medication',false,'character varying'),
    ('medical_profiles','created_at',true,'timestamp without time zone'),
    ('medical_profiles','updated_at',true,'timestamp without time zone'),
    ('medical_profiles','version',false,'bigint'),
    ('medical_profiles','health_profile_id',true,'uuid'),
    ('medical_profiles','id',true,'uuid'),
    ('medical_profiles','pregnancy_status',true,'character varying'),
    ('nutrition_preference_profiles','consumes_eggs',true,'boolean'),
    ('nutrition_preference_profiles','food_variety_preference',true,'smallint'),
    ('nutrition_preference_profiles','meals_per_day',true,'integer'),
    ('nutrition_preference_profiles','created_at',true,'timestamp without time zone'),
    ('nutrition_preference_profiles','updated_at',true,'timestamp without time zone'),
    ('nutrition_preference_profiles','version',false,'bigint'),
    ('nutrition_preference_profiles','health_profile_id',true,'uuid'),
    ('nutrition_preference_profiles','id',true,'uuid'),
    ('nutrition_preference_profiles','dairy_preference',true,'character varying'),
    ('nutrition_preference_profiles','diet_type',true,'character varying'),
    ('nutrition_preference_profiles','preferred_meal_size',true,'character varying'),
    ('nutrition_preference_profiles','spice_preference',true,'character varying'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_preferred_cuisines','cuisine',true,'character varying'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_preferred_protein_sources','protein_source',true,'character varying'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_religious_restrictions','religious_restriction',true,'character varying'),
    ('nutrition_targets','bmr',true,'numeric'),
    ('nutrition_targets','calorie_adjustment',true,'numeric'),
    ('nutrition_targets','carbohydrate_grams',true,'numeric'),
    ('nutrition_targets','fat_grams',true,'numeric'),
    ('nutrition_targets','fiber_grams',true,'numeric'),
    ('nutrition_targets','protein_grams',true,'numeric'),
    ('nutrition_targets','target_calories',true,'numeric'),
    ('nutrition_targets','tdee',true,'numeric'),
    ('nutrition_targets','water_ml',true,'numeric'),
    ('nutrition_targets','weight_used_kg',true,'numeric'),
    ('nutrition_targets','created_at',true,'timestamp without time zone'),
    ('nutrition_targets','updated_at',true,'timestamp without time zone'),
    ('nutrition_targets','version',false,'bigint'),
    ('nutrition_targets','health_profile_id',true,'uuid'),
    ('nutrition_targets','id',true,'uuid'),
    ('nutrition_targets','activity_level',true,'character varying'),
    ('nutrition_targets','calculation_method',true,'character varying'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_taste_preferences','taste_preference',true,'character varying'),
    ('payment_webhook_events','received_at',true,'timestamp with time zone'),
    ('payment_webhook_events','id',true,'uuid'),
    ('payment_webhook_events','event_type',true,'character varying'),
    ('payment_webhook_events','provider_event_id',true,'character varying'),
    ('payments','amount',false,'integer'),
    ('payments','created_at',false,'timestamp with time zone'),
    ('payments','updated_at',false,'timestamp with time zone'),
    ('payments','id',true,'uuid'),
    ('payments','subscription_id',false,'uuid'),
    ('payments','user_id',false,'uuid'),
    ('payments','currency',false,'character varying'),
    ('payments','order_id',false,'character varying'),
    ('payments','payment_id',false,'character varying'),
    ('payments','plan_id',false,'character varying'),
    ('payments','provider',false,'character varying'),
    ('payments','provider_event_id',false,'character varying'),
    ('payments','razorpay_subscription_id',false,'character varying'),
    ('payments','status',false,'character varying'),
    ('personal_profiles','breakfast_time',false,'time without time zone'),
    ('personal_profiles','date_of_birth',false,'date'),
    ('personal_profiles','dinner_time',false,'time without time zone'),
    ('personal_profiles','height_cm',true,'double precision'),
    ('personal_profiles','lunch_time',false,'time without time zone'),
    ('personal_profiles','night_shift',true,'boolean'),
    ('personal_profiles','sleep_time',false,'time without time zone'),
    ('personal_profiles','wake_up_time',false,'time without time zone'),
    ('personal_profiles','weight_kg',false,'numeric'),
    ('personal_profiles','created_at',true,'timestamp without time zone'),
    ('personal_profiles','updated_at',true,'timestamp without time zone'),
    ('personal_profiles','version',false,'bigint'),
    ('personal_profiles','health_profile_id',true,'uuid'),
    ('personal_profiles','id',true,'uuid'),
    ('personal_profiles','gender',true,'character varying'),
    ('personal_profiles','living_arrangement',false,'character varying'),
    ('personal_profiles','occupation',false,'character varying'),
    ('personal_profiles','region',false,'character varying'),
    ('personal_profiles','state',false,'character varying'),
    ('protein_adaptation_profiles','current_protein_grams',false,'numeric'),
    ('protein_adaptation_profiles','current_target_grams',true,'numeric'),
    ('protein_adaptation_profiles','days_at_current_target',true,'integer'),
    ('protein_adaptation_profiles','last_evaluated_on',false,'date'),
    ('protein_adaptation_profiles','last_target_change_at',false,'date'),
    ('protein_adaptation_profiles','long_term_target_grams',true,'numeric'),
    ('protein_adaptation_profiles','created_at',true,'timestamp without time zone'),
    ('protein_adaptation_profiles','updated_at',true,'timestamp without time zone'),
    ('protein_adaptation_profiles','version',false,'bigint'),
    ('protein_adaptation_profiles','health_profile_id',true,'uuid'),
    ('protein_adaptation_profiles','id',true,'uuid'),
    ('protein_adaptation_profiles','adaptation_stage',true,'character varying'),
    ('protein_adaptation_profiles','tolerance_status',true,'character varying'),
    ('subscription_plans','chatbot_daily_limit',true,'integer'),
    ('subscription_plans','is_active',true,'boolean'),
    ('subscription_plans','months',true,'integer'),
    ('subscription_plans','price',true,'integer'),
    ('subscription_plans','id',true,'character varying'),
    ('subscription_plans','razorpay_plan_id',false,'character varying'),
    ('supplement_profile_current_supplements','supplement_profile_id',true,'uuid'),
    ('supplement_profile_current_supplements','supplement',false,'character varying'),
    ('supplement_profiles','open_to_supplements',true,'boolean'),
    ('supplement_profiles','created_at',true,'timestamp without time zone'),
    ('supplement_profiles','updated_at',true,'timestamp without time zone'),
    ('supplement_profiles','version',false,'bigint'),
    ('supplement_profiles','health_profile_id',true,'uuid'),
    ('supplement_profiles','id',true,'uuid'),
    ('user_auth','last_login_at',false,'timestamp without time zone'),
    ('user_auth','user_id',true,'uuid'),
    ('user_auth','provider',false,'character varying'),
    ('user_auth','provider_id',false,'character varying'),
    ('user_subscriptions','auto_renew',true,'boolean'),
    ('user_subscriptions','created_at',false,'timestamp with time zone'),
    ('user_subscriptions','ends_at',false,'timestamp with time zone'),
    ('user_subscriptions','starts_at',false,'timestamp with time zone'),
    ('user_subscriptions','updated_at',false,'timestamp with time zone'),
    ('user_subscriptions','id',true,'uuid'),
    ('user_subscriptions','user_id',true,'uuid'),
    ('user_subscriptions','plan_id',true,'character varying'),
    ('user_subscriptions','razorpay_subscription_id',false,'character varying'),
    ('user_subscriptions','status',false,'character varying'),
    ('users','email_verified',true,'boolean'),
    ('users','is_blocked',false,'boolean'),
    ('users','created_at',true,'timestamp without time zone'),
    ('users','email_verification_token_expiry',false,'timestamp with time zone'),
    ('users','updated_at',true,'timestamp without time zone'),
    ('users','version',false,'bigint'),
    ('users','id',true,'uuid'),
    ('users','email',false,'character varying'),
    ('users','email_verification_token',false,'character varying'),
    ('users','name',false,'character varying'),
    ('users','phone',false,'character varying'),
    ('workout_profiles','include_post_workout_meal',true,'boolean'),
    ('workout_profiles','include_pre_workout_meal',true,'boolean'),
    ('workout_profiles','preferred_workout_time',false,'time without time zone'),
    ('workout_profiles','created_at',true,'timestamp without time zone'),
    ('workout_profiles','updated_at',true,'timestamp without time zone'),
    ('workout_profiles','version',false,'bigint'),
    ('workout_profiles','health_profile_id',true,'uuid'),
    ('workout_profiles','id',true,'uuid'),
    ('workout_profiles','training_status',false,'character varying')
),
actual as (
  select table_name::text as table_name, column_name::text as column_name,
         data_type::text as data_type, (is_nullable = 'NO') as db_not_null,
         column_default as column_default
    from information_schema.columns
   where table_schema = 'public'
),
mapped_tables as (select distinct table_name from expected)
select 'S4.6' as section,
       coalesce(e.table_name, a.table_name) as table_name,
       coalesce(e.column_name, a.column_name) as column_name,
       e.jpa_type, a.data_type as db_type, e.jpa_not_null, a.db_not_null,
       a.column_default,
       case when to_regclass(format('public.%I', coalesce(e.table_name, a.table_name))) is null then 'MISSING TABLE'
            when e.table_name is null then 'EXTRA COLUMN (unmapped, informational)'
            when a.table_name is null then 'MISSING COLUMN'
            when a.data_type <> e.jpa_type then 'TYPE MISMATCH'
            when e.jpa_not_null and not a.db_not_null then 'JPA NOT NULL, DB NULLABLE'
            when not e.jpa_not_null and a.db_not_null and a.column_default is null then 'DB NOT NULL (no default), JPA NULLABLE'
            else 'ok' end as status
  from expected e
  full join actual a on a.table_name = e.table_name and a.column_name = e.column_name
 where coalesce(e.table_name, a.table_name) in (select table_name from mapped_tables)
 order by case when e.table_name is null then 2 else 1 end, status, table_name, column_name;

-- [S4.6b] Roll-up of S4.6 by status (read this first, then drill into S4.6).
with expected (table_name, column_name, jpa_not_null, jpa_type) as (
  values
    ('activity_profiles','days_per_week',true,'integer'),
    ('activity_profiles','duration_minutes',true,'integer'),
    ('activity_profiles','created_at',true,'timestamp without time zone'),
    ('activity_profiles','updated_at',true,'timestamp without time zone'),
    ('activity_profiles','version',false,'bigint'),
    ('activity_profiles','id',true,'uuid'),
    ('activity_profiles','workout_profile_id',true,'uuid'),
    ('activity_profiles','activity_type',true,'character varying'),
    ('activity_profiles','experience_level',true,'character varying'),
    ('activity_profiles','sport_type',false,'character varying'),
    ('activity_profiles','workout_intensity',true,'character varying'),
    ('cooking_profile_equipment','cooking_profile_id',true,'uuid'),
    ('cooking_profile_equipment','equipment',false,'character varying'),
    ('cooking_profiles','cooking_time_minutes',true,'integer'),
    ('cooking_profiles','meal_prep',true,'boolean'),
    ('cooking_profiles','created_at',true,'timestamp without time zone'),
    ('cooking_profiles','updated_at',true,'timestamp without time zone'),
    ('cooking_profiles','version',false,'bigint'),
    ('cooking_profiles','health_profile_id',true,'uuid'),
    ('cooking_profiles','id',true,'uuid'),
    ('cooking_profiles','budget',true,'character varying'),
    ('cooking_profiles','cooking_skill',true,'character varying'),
    ('fitness_goal_profiles','cheat_meals_per_week',true,'integer'),
    ('fitness_goal_profiles','include_cheat_meals',true,'boolean'),
    ('fitness_goal_profiles','target_date',false,'date'),
    ('fitness_goal_profiles','target_weight_kg',false,'double precision'),
    ('fitness_goal_profiles','weekly_weight_change_kg',false,'double precision'),
    ('fitness_goal_profiles','created_at',true,'timestamp without time zone'),
    ('fitness_goal_profiles','updated_at',true,'timestamp without time zone'),
    ('fitness_goal_profiles','version',false,'bigint'),
    ('fitness_goal_profiles','health_profile_id',true,'uuid'),
    ('fitness_goal_profiles','id',true,'uuid'),
    ('fitness_goal_profiles','diet_flexibility',false,'character varying'),
    ('fitness_goal_profiles','diet_strictness',false,'character varying'),
    ('fitness_goal_profiles','primary_goal',true,'character varying'),
    ('fitness_goal_profiles','secondary_goal',false,'character varying'),
    ('health_profiles','current_protein_grams',false,'numeric'),
    ('health_profiles','onboarding_completed',true,'boolean'),
    ('health_profiles','profile_completion_percentage',true,'integer'),
    ('health_profiles','created_at',true,'timestamp without time zone'),
    ('health_profiles','updated_at',true,'timestamp without time zone'),
    ('health_profiles','version',false,'bigint'),
    ('health_profiles','id',true,'uuid'),
    ('health_profiles','user_id',true,'uuid'),
    ('lifestyle_preference_profiles','food_delivery_available',true,'boolean'),
    ('lifestyle_preference_profiles','hosteller',true,'boolean'),
    ('lifestyle_preference_profiles','office_lunch_available',true,'boolean'),
    ('lifestyle_preference_profiles','travels_frequently',true,'boolean'),
    ('lifestyle_preference_profiles','created_at',true,'timestamp without time zone'),
    ('lifestyle_preference_profiles','updated_at',true,'timestamp without time zone'),
    ('lifestyle_preference_profiles','version',false,'bigint'),
    ('lifestyle_preference_profiles','health_profile_id',true,'uuid'),
    ('lifestyle_preference_profiles','id',true,'uuid'),
    ('medical_profile_conditions','medical_profile_id',true,'uuid'),
    ('medical_profile_conditions','medical_condition',false,'character varying'),
    ('medical_profile_food_allergies','medical_profile_id',true,'uuid'),
    ('medical_profile_food_allergies','food_allergy',false,'character varying'),
    ('medical_profile_medications','medical_profile_id',true,'uuid'),
    ('medical_profile_medications','medication',false,'character varying'),
    ('medical_profiles','created_at',true,'timestamp without time zone'),
    ('medical_profiles','updated_at',true,'timestamp without time zone'),
    ('medical_profiles','version',false,'bigint'),
    ('medical_profiles','health_profile_id',true,'uuid'),
    ('medical_profiles','id',true,'uuid'),
    ('medical_profiles','pregnancy_status',true,'character varying'),
    ('nutrition_preference_profiles','consumes_eggs',true,'boolean'),
    ('nutrition_preference_profiles','food_variety_preference',true,'smallint'),
    ('nutrition_preference_profiles','meals_per_day',true,'integer'),
    ('nutrition_preference_profiles','created_at',true,'timestamp without time zone'),
    ('nutrition_preference_profiles','updated_at',true,'timestamp without time zone'),
    ('nutrition_preference_profiles','version',false,'bigint'),
    ('nutrition_preference_profiles','health_profile_id',true,'uuid'),
    ('nutrition_preference_profiles','id',true,'uuid'),
    ('nutrition_preference_profiles','dairy_preference',true,'character varying'),
    ('nutrition_preference_profiles','diet_type',true,'character varying'),
    ('nutrition_preference_profiles','preferred_meal_size',true,'character varying'),
    ('nutrition_preference_profiles','spice_preference',true,'character varying'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_preferred_cuisines','cuisine',true,'character varying'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_preferred_protein_sources','protein_source',true,'character varying'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_religious_restrictions','religious_restriction',true,'character varying'),
    ('nutrition_targets','bmr',true,'numeric'),
    ('nutrition_targets','calorie_adjustment',true,'numeric'),
    ('nutrition_targets','carbohydrate_grams',true,'numeric'),
    ('nutrition_targets','fat_grams',true,'numeric'),
    ('nutrition_targets','fiber_grams',true,'numeric'),
    ('nutrition_targets','protein_grams',true,'numeric'),
    ('nutrition_targets','target_calories',true,'numeric'),
    ('nutrition_targets','tdee',true,'numeric'),
    ('nutrition_targets','water_ml',true,'numeric'),
    ('nutrition_targets','weight_used_kg',true,'numeric'),
    ('nutrition_targets','created_at',true,'timestamp without time zone'),
    ('nutrition_targets','updated_at',true,'timestamp without time zone'),
    ('nutrition_targets','version',false,'bigint'),
    ('nutrition_targets','health_profile_id',true,'uuid'),
    ('nutrition_targets','id',true,'uuid'),
    ('nutrition_targets','activity_level',true,'character varying'),
    ('nutrition_targets','calculation_method',true,'character varying'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id',true,'uuid'),
    ('nutrition_taste_preferences','taste_preference',true,'character varying'),
    ('payment_webhook_events','received_at',true,'timestamp with time zone'),
    ('payment_webhook_events','id',true,'uuid'),
    ('payment_webhook_events','event_type',true,'character varying'),
    ('payment_webhook_events','provider_event_id',true,'character varying'),
    ('payments','amount',false,'integer'),
    ('payments','created_at',false,'timestamp with time zone'),
    ('payments','updated_at',false,'timestamp with time zone'),
    ('payments','id',true,'uuid'),
    ('payments','subscription_id',false,'uuid'),
    ('payments','user_id',false,'uuid'),
    ('payments','currency',false,'character varying'),
    ('payments','order_id',false,'character varying'),
    ('payments','payment_id',false,'character varying'),
    ('payments','plan_id',false,'character varying'),
    ('payments','provider',false,'character varying'),
    ('payments','provider_event_id',false,'character varying'),
    ('payments','razorpay_subscription_id',false,'character varying'),
    ('payments','status',false,'character varying'),
    ('personal_profiles','breakfast_time',false,'time without time zone'),
    ('personal_profiles','date_of_birth',false,'date'),
    ('personal_profiles','dinner_time',false,'time without time zone'),
    ('personal_profiles','height_cm',true,'double precision'),
    ('personal_profiles','lunch_time',false,'time without time zone'),
    ('personal_profiles','night_shift',true,'boolean'),
    ('personal_profiles','sleep_time',false,'time without time zone'),
    ('personal_profiles','wake_up_time',false,'time without time zone'),
    ('personal_profiles','weight_kg',false,'numeric'),
    ('personal_profiles','created_at',true,'timestamp without time zone'),
    ('personal_profiles','updated_at',true,'timestamp without time zone'),
    ('personal_profiles','version',false,'bigint'),
    ('personal_profiles','health_profile_id',true,'uuid'),
    ('personal_profiles','id',true,'uuid'),
    ('personal_profiles','gender',true,'character varying'),
    ('personal_profiles','living_arrangement',false,'character varying'),
    ('personal_profiles','occupation',false,'character varying'),
    ('personal_profiles','region',false,'character varying'),
    ('personal_profiles','state',false,'character varying'),
    ('protein_adaptation_profiles','current_protein_grams',false,'numeric'),
    ('protein_adaptation_profiles','current_target_grams',true,'numeric'),
    ('protein_adaptation_profiles','days_at_current_target',true,'integer'),
    ('protein_adaptation_profiles','last_evaluated_on',false,'date'),
    ('protein_adaptation_profiles','last_target_change_at',false,'date'),
    ('protein_adaptation_profiles','long_term_target_grams',true,'numeric'),
    ('protein_adaptation_profiles','created_at',true,'timestamp without time zone'),
    ('protein_adaptation_profiles','updated_at',true,'timestamp without time zone'),
    ('protein_adaptation_profiles','version',false,'bigint'),
    ('protein_adaptation_profiles','health_profile_id',true,'uuid'),
    ('protein_adaptation_profiles','id',true,'uuid'),
    ('protein_adaptation_profiles','adaptation_stage',true,'character varying'),
    ('protein_adaptation_profiles','tolerance_status',true,'character varying'),
    ('subscription_plans','chatbot_daily_limit',true,'integer'),
    ('subscription_plans','is_active',true,'boolean'),
    ('subscription_plans','months',true,'integer'),
    ('subscription_plans','price',true,'integer'),
    ('subscription_plans','id',true,'character varying'),
    ('subscription_plans','razorpay_plan_id',false,'character varying'),
    ('supplement_profile_current_supplements','supplement_profile_id',true,'uuid'),
    ('supplement_profile_current_supplements','supplement',false,'character varying'),
    ('supplement_profiles','open_to_supplements',true,'boolean'),
    ('supplement_profiles','created_at',true,'timestamp without time zone'),
    ('supplement_profiles','updated_at',true,'timestamp without time zone'),
    ('supplement_profiles','version',false,'bigint'),
    ('supplement_profiles','health_profile_id',true,'uuid'),
    ('supplement_profiles','id',true,'uuid'),
    ('user_auth','last_login_at',false,'timestamp without time zone'),
    ('user_auth','user_id',true,'uuid'),
    ('user_auth','provider',false,'character varying'),
    ('user_auth','provider_id',false,'character varying'),
    ('user_subscriptions','auto_renew',true,'boolean'),
    ('user_subscriptions','created_at',false,'timestamp with time zone'),
    ('user_subscriptions','ends_at',false,'timestamp with time zone'),
    ('user_subscriptions','starts_at',false,'timestamp with time zone'),
    ('user_subscriptions','updated_at',false,'timestamp with time zone'),
    ('user_subscriptions','id',true,'uuid'),
    ('user_subscriptions','user_id',true,'uuid'),
    ('user_subscriptions','plan_id',true,'character varying'),
    ('user_subscriptions','razorpay_subscription_id',false,'character varying'),
    ('user_subscriptions','status',false,'character varying'),
    ('users','email_verified',true,'boolean'),
    ('users','is_blocked',false,'boolean'),
    ('users','created_at',true,'timestamp without time zone'),
    ('users','email_verification_token_expiry',false,'timestamp with time zone'),
    ('users','updated_at',true,'timestamp without time zone'),
    ('users','version',false,'bigint'),
    ('users','id',true,'uuid'),
    ('users','email',false,'character varying'),
    ('users','email_verification_token',false,'character varying'),
    ('users','name',false,'character varying'),
    ('users','phone',false,'character varying'),
    ('workout_profiles','include_post_workout_meal',true,'boolean'),
    ('workout_profiles','include_pre_workout_meal',true,'boolean'),
    ('workout_profiles','preferred_workout_time',false,'time without time zone'),
    ('workout_profiles','created_at',true,'timestamp without time zone'),
    ('workout_profiles','updated_at',true,'timestamp without time zone'),
    ('workout_profiles','version',false,'bigint'),
    ('workout_profiles','health_profile_id',true,'uuid'),
    ('workout_profiles','id',true,'uuid'),
    ('workout_profiles','training_status',false,'character varying')
),
actual as (
  select table_name::text as table_name, column_name::text as column_name,
         data_type::text as data_type, (is_nullable = 'NO') as db_not_null, column_default
    from information_schema.columns where table_schema = 'public'
),
mapped_tables as (select distinct table_name from expected),
r as (
  select coalesce(e.table_name, a.table_name) as table_name,
         case when to_regclass(format('public.%I', coalesce(e.table_name, a.table_name))) is null then 'MISSING TABLE'
              when e.table_name is null then 'EXTRA COLUMN (unmapped, informational)'
              when a.table_name is null then 'MISSING COLUMN'
              when a.data_type <> e.jpa_type then 'TYPE MISMATCH'
              when e.jpa_not_null and not a.db_not_null then 'JPA NOT NULL, DB NULLABLE'
              when not e.jpa_not_null and a.db_not_null and a.column_default is null then 'DB NOT NULL (no default), JPA NULLABLE'
              else 'ok' end as status
    from expected e
    full join actual a on a.table_name = e.table_name and a.column_name = e.column_name
   where coalesce(e.table_name, a.table_name) in (select table_name from mapped_tables)
)
select 'S4.6b' as section, status, count(*) as columns,
       count(distinct table_name) as tables,
       string_agg(distinct table_name, ', ') filter (where status <> 'ok') as affected_tables
  from r
 group by status
 order by status;

-- [S4.7] ENUM CHECK CONSTRAINTS. Hibernate 6.2+ generates a CHECK listing the enum constants for
-- every @Enumerated(STRING) column. A stale CHECK (an older, shorter list) makes inserting a newer
-- constant fail at runtime. This compares each column's actual CHECK literal set with the current Java enum.
with expected (table_name, column_name, enum_values) as (
  values
    ('activity_profiles','activity_type',array['SEDENTARY','WALKING','GYM','RUNNING','CYCLING','SWIMMING','YOGA','PILATES','HOME_WORKOUT','SPORTS','MARTIAL_ARTS','PHYSICALLY_DEMANDING_JOB']),
    ('activity_profiles','experience_level',array['LESS_THAN_A_YEAR','ONE_TO_TWO_YEARS','TWO_TO_FIVE_YEARS','FIVE_PLUS_YEARS']),
    ('activity_profiles','sport_type',array['CRICKET','FOOTBALL','BADMINTON','TENNIS','BASKETBALL','VOLLEYBALL','HOCKEY','KABADDI','ATHLETICS','OTHER']),
    ('activity_profiles','workout_intensity',array['LOW','MODERATE','HIGH','ATHLETE']),
    ('cooking_profile_equipment','equipment',array['GAS_STOVE','INDUCTION','MICROWAVE','AIR_FRYER','PRESSURE_COOKER','RICE_COOKER','ELECTRIC_KETTLE','MIXER_GRINDER','OVEN']),
    ('cooking_profiles','budget',array['LOW','MEDIUM','HIGH','PREMIUM']),
    ('cooking_profiles','cooking_skill',array['NONE','BEGINNER','INTERMEDIATE','ADVANCED']),
    ('fitness_goal_profiles','diet_flexibility',array['NEVER','OCCASIONALLY','WEEKLY','FREQUENTLY']),
    ('fitness_goal_profiles','diet_strictness',array['STRICT','BALANCED','FLEXIBLE']),
    ('fitness_goal_profiles','primary_goal',array['FAT_LOSS','MUSCLE_GAIN','BODY_RECOMPOSITION','WEIGHT_GAIN','MAINTENANCE','GENERAL_HEALTH','SPORTS_PERFORMANCE']),
    ('fitness_goal_profiles','secondary_goal',array['FAT_LOSS','MUSCLE_GAIN','BODY_RECOMPOSITION','WEIGHT_GAIN','MAINTENANCE','GENERAL_HEALTH','SPORTS_PERFORMANCE']),
    ('medical_profile_conditions','medical_condition',array['DIABETES','PREDIABETES','HYPERTENSION','HIGH_CHOLESTEROL','FATTY_LIVER','HYPOTHYROID','HYPERTHYROID','PCOS','IBS','GERD','CELIAC','GOUT','KIDNEY_DISEASE','LIVER_DISEASE']),
    ('medical_profile_food_allergies','food_allergy',array['MILK','LACTOSE','PEANUT','TREE_NUT','SOY','EGG','GLUTEN','FISH','SHELLFISH','SESAME','MUSTARD']),
    ('medical_profiles','pregnancy_status',array['NOT_APPLICABLE']),
    ('nutrition_preference_profiles','dairy_preference',array['YES','NO','LIMITED']),
    ('nutrition_preference_profiles','diet_type',array['VEG','EGGETARIAN','NON_VEG','VEGAN','JAIN','SATVIK']),
    ('nutrition_preference_profiles','preferred_meal_size',array['SMALL','MEDIUM','LARGE']),
    ('nutrition_preference_profiles','spice_preference',array['MILD','MEDIUM','SPICY']),
    ('nutrition_preferred_cuisines','cuisine',array['NORTH_INDIAN','SOUTH_INDIAN','PUNJABI','GUJARATI','RAJASTHANI','MAHARASHTRIAN','BENGALI','KASHMIRI','BIHARI','ODISHA','GOAN','HYDERABADI','ASSAMESE','NORTH_EAST','MIXED']),
    ('nutrition_preferred_protein_sources','protein_source',array['EGGS','CHICKEN','FISH','MUTTON','PANEER','MILK','CURD','WHEY','SOY','TOFU','DAL','CHICKPEAS','RAJMA','LENTILS','PEANUTS','NUTS']),
    ('nutrition_religious_restrictions','religious_restriction',array['NO_BEEF','NO_PORK','NO_ONION_GARLIC','NAVRATRI_FAST','EKADASHI_FAST']),
    ('nutrition_targets','activity_level',array['SEDENTARY','LIGHTLY_ACTIVE','MODERATELY_ACTIVE','VERY_ACTIVE','EXTREMELY_ACTIVE']),
    ('nutrition_targets','calculation_method',array['MIFFLIN_ST_JEOR','HARRIS_BENEDICT']),
    ('nutrition_taste_preferences','taste_preference',array['SPICY','MILD','SWEET','SAVORY','SOUR','BITTER']),
    ('personal_profiles','gender',array['MALE','FEMALE']),
    ('personal_profiles','living_arrangement',array['FAMILY','ALONE','HOSTEL','PG','SHARED_FLAT']),
    ('personal_profiles','occupation',array['OFFICE','WFH','HOSTEL_STUDENT','COLLEGE_STUDENT','HOME_MAKER','RETIRED','SHIFT_WORKER','MANUAL_LABOUR']),
    ('personal_profiles','region',array['NORTH','SOUTH','EAST','WEST','CENTRAL','NORTH_EAST']),
    ('personal_profiles','state',array['ANDHRA_PRADESH','ARUNACHAL_PRADESH','ASSAM','BIHAR','CHHATTISGARH','GOA','GUJARAT','HARYANA','HIMACHAL_PRADESH','JHARKHAND','KARNATAKA','KERALA','MADHYA_PRADESH','MAHARASHTRA','MANIPUR','MEGHALAYA','MIZORAM','NAGALAND','ODISHA','PUNJAB','RAJASTHAN','SIKKIM','TAMIL_NADU','TELANGANA','TRIPURA','UTTAR_PRADESH','UTTARAKHAND','WEST_BENGAL','DELHI','JAMMU_KASHMIR','LADAKH','CHANDIGARH','PUDUCHERRY','DADRA_NAGAR_HAVELI','DAMAN_DIU','LAKSHADWEEP','ANDAMAN_NICOBAR']),
    ('protein_adaptation_profiles','adaptation_stage',array['NOT_STARTED','STARTING','ADAPTING','TARGET_REACHED']),
    ('protein_adaptation_profiles','tolerance_status',array['UNKNOWN','GOOD','MILD_ISSUES','SIGNIFICANT_ISSUES']),
    ('supplement_profile_current_supplements','supplement',array['WHEY','CASEIN','CREATINE','MULTIVITAMIN','FISH_OIL','VITAMIN_D','VITAMIN_B12','MAGNESIUM','ELECTROLYTES','PROBIOTIC']),
    ('workout_profiles','training_status',array['BEGINNER','INTERMEDIATE','ADVANCED'])
),
checks as (
  select c.conrelid::regclass::text as table_name, c.conname::text as constraint_name,
         pg_get_constraintdef(c.oid) as def
    from pg_constraint c
   where c.contype = 'c' and c.connamespace = 'public'::regnamespace
),
joined as (
  select e.table_name, e.column_name, e.enum_values, k.constraint_name, k.def,
         (select coalesce(array_agg(distinct g[1] order by g[1]), array[]::text[])
            from regexp_matches(k.def, '''([A-Z0-9_]+)''', 'g') as m(g)) as actual_values
    from expected e
    left join checks k
      on k.table_name = e.table_name and k.def ~ ('(^|[^a-z_])' || e.column_name || '([^a-z_]|$)')
)
select 'S4.7' as section, table_name, column_name, constraint_name,
       case when constraint_name is null then 'no CHECK constraint on this column'
            when actual_values @> enum_values and actual_values <@ enum_values then 'ok: matches current enum'
            when actual_values @> enum_values then 'CHECK allows extra values (harmless)'
            else 'STALE CHECK: missing ' || (select string_agg(v, ',')
                                              from unnest(enum_values) v
                                             where not (v = any(actual_values))) end as status
  from joined
 where to_regclass(format('public.%I', table_name)) is not null
 order by status, table_name, column_name;

-- [S4.8] nutrition_targets in detail. Product decision: calculated on demand, never persisted
-- (no production code writes or reads it). Any row here is stale data from an earlier experiment.
select 'S4.8' as section,
       to_regclass('public.nutrition_targets') is not null as table_exists,
       case when to_regclass('public.nutrition_targets') is not null then
         (xpath('/row/c/text()', query_to_xml('select count(*) as c from public.nutrition_targets', true, true, '')))[1]::text::bigint
       end as row_count,
       (select string_agg(column_name::text, ',' order by ordinal_position)
          from information_schema.columns
         where table_schema = 'public' and table_name = 'nutrition_targets') as columns_present,
       (select count(*) from pg_constraint c
         where c.confrelid = to_regclass('public.nutrition_targets')) as foreign_keys_pointing_at_it;


-- #############################################################################
-- S5  DATA INTEGRITY
-- #############################################################################
-- All counts are produced by query_to_xml(<generated SELECT count(*)>). Every generated
-- statement is chosen by a CASE on catalog facts so that an absent table or column yields
-- NULL ("not measurable") instead of an error. NULL is therefore a FINDING, not a pass.

-- [S5.1] Exact row count for EVERY table in schema public, classified. This is the
-- "which tables could lose data during reconciliation" answer: any table with
-- rows > 0 whose class is not 'JPA-mapped' has NO entity, and must never be
-- dropped, retyped or rewritten by a reconcile migration.
with mapped(t) as (
  values ('activity_profiles'),('cooking_profile_equipment'),('cooking_profiles'),('fitness_goal_profiles'),
         ('health_profiles'),('lifestyle_preference_profiles'),('medical_profile_conditions'),
         ('medical_profile_food_allergies'),('medical_profile_medications'),('medical_profiles'),
         ('nutrition_preference_profiles'),('nutrition_preferred_cuisines'),('nutrition_preferred_protein_sources'),
         ('nutrition_religious_restrictions'),('nutrition_targets'),('nutrition_taste_preferences'),
         ('payment_webhook_events'),('payments'),('personal_profiles'),('protein_adaptation_profiles'),
         ('subscription_plans'),('supplement_profile_current_supplements'),('supplement_profiles'),
         ('user_auth'),('user_subscriptions'),('users'),('workout_profiles')
),
flyway_only(t) as (
  values ('coupons'),('coupon_redemptions'),('foods'),('diet_plans'),('meal_logs'),('weight_logs'),
         ('daily_metrics'),('streaks'),('chat_usage'),('chat_messages'),('notification_preferences'),
         ('notification_logs'),('admin_users'),('admin_audit_logs')
),
legacy(t) as (
  values ('health_profile_medical_conditions'),('health_profile_allergies'),('health_profile_medications'),
         ('health_profile_preferred_cuisines'),('health_profile_taste_preferences'),('health_profile_favourite_foods'),
         ('health_profile_disliked_foods'),('health_profile_religious_restrictions'),
         ('cooking_profile_equipments')
),
counted as (
  select c.relname::text as t,
         (xpath('/row/c/text()',
                query_to_xml(format('select count(*) as c from public.%I', c.relname), true, true, '')))[1]::text::bigint as n
    from pg_class c
   where c.relnamespace = 'public'::regnamespace and c.relkind in ('r', 'p')
)
select 'S5.1' as section, k.t as table_name,
       case when k.t in (select t from mapped)      then 'JPA-mapped'
            when k.t in (select t from flyway_only) then 'Flyway-only (no entity)'
            when k.t in (select t from legacy)      then 'legacy V17 drop target'
            when k.t = 'flyway_schema_history'      then 'Flyway history'
            else 'UNKNOWN (in neither migrations nor JPA)' end as class,
       k.n as row_count,
       case when k.n > 0 and k.t not in (select t from mapped) and k.t <> 'flyway_schema_history'
                 then 'HOLDS DATA WITH NO ENTITY: reconcile must not drop, retype or rewrite'
            when k.n > 0 and k.t in (select t from mapped)
                 then 'holds data: additive and validated changes only'
            else null end as risk_note
  from counted k
 order by (k.t in (select t from mapped)), k.n desc nulls last, k.t;

-- [S5.2] Duplicate or NULL health_profile_id in each one-to-one profile table.
-- The unique constraint should make duplicated = 0. A table created by Hibernate update
-- normally has it; a hand-made copy might not. duplicated_ids > 0 BLOCKS adding a UNIQUE
-- constraint (and the first-insert race fix relies on one existing for protein_adaptation_profiles).
with t(table_name) as (
  values ('personal_profiles'),('fitness_goal_profiles'),('nutrition_preference_profiles'),
         ('workout_profiles'),('medical_profiles'),('cooking_profiles'),
         ('lifestyle_preference_profiles'),('supplement_profiles'),('nutrition_targets'),
         ('protein_adaptation_profiles')
),
g as (
  select t.table_name,
         exists (select 1 from information_schema.columns c
                  where c.table_schema = 'public' and c.table_name = t.table_name
                    and c.column_name = 'health_profile_id') as has_col
    from t
)
select 'S5.2' as section, g.table_name, g.has_col as column_present,
       (xpath('/row/c/text()', query_to_xml(case when g.has_col then format('select count(*) as c from public.%I', g.table_name) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as rows_total,
       (xpath('/row/c/text()', query_to_xml(case when g.has_col then format('select count(*) as c from (select health_profile_id from public.%I where health_profile_id is not null group by health_profile_id having count(*) > 1) d', g.table_name) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as duplicated_ids,
       (xpath('/row/c/text()', query_to_xml(case when g.has_col then format('select count(*) as c from public.%I where health_profile_id is null', g.table_name) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as null_health_profile_id
  from g
 order by g.table_name;

-- [S5.3] ORPHAN rows: for every foreign key Hibernate expects, child rows whose parent
-- row does not exist. Measured whether or not the FK exists in the database. NULL means
-- the child column or the parent's id column is absent (not measurable).
-- orphan_rows > 0 BLOCKS adding or validating that FK.
with fk(child_table, child_column, parent_table, hibernate_name) as (
  values
    ('activity_profiles','workout_profile_id','workout_profiles','FKoe08h21upnnrqymscefcqpspg'),
    ('cooking_profile_equipment','cooking_profile_id','cooking_profiles','FKt6capcee7jfvijh464h27pk2b'),
    ('cooking_profiles','health_profile_id','health_profiles','FKatxq86eecq7po29v3dpt5kj6t'),
    ('fitness_goal_profiles','health_profile_id','health_profiles','FKswsiergey4b3tfe20dfcny8uo'),
    ('health_profiles','user_id','users','FK3tevdso70nrymmg3rgy76jqga'),
    ('lifestyle_preference_profiles','health_profile_id','health_profiles','FKp7qm6cb0juus03065ee37hdm5'),
    ('medical_profile_conditions','medical_profile_id','medical_profiles','FKita44wf1siheut54mu4fdd4kh'),
    ('medical_profile_food_allergies','medical_profile_id','medical_profiles','FK58k4ptdomqa2cg38tao58cwwk'),
    ('medical_profile_medications','medical_profile_id','medical_profiles','FKsne6qk9o7lx2jr45h169quvhe'),
    ('medical_profiles','health_profile_id','health_profiles','FKai2hiaa04u19oefkeq79rdw46'),
    ('nutrition_preference_profiles','health_profile_id','health_profiles','FK2flkraqih8n013xi9ohsottff'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id','nutrition_preference_profiles','FKs9cpieyfsfggy37u5xbcpr198'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id','nutrition_preference_profiles','FKpca5dnd50124u6skpvhcn64pa'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id','nutrition_preference_profiles','FKr49sriujl8k79n2haygc6juls'),
    ('nutrition_targets','health_profile_id','health_profiles','FKesp92d6cyltrkkrabkgigf34y'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id','nutrition_preference_profiles','FK3binc14qbr7m5m727qmnllut7'),
    ('personal_profiles','health_profile_id','health_profiles','FKkgrwmce08f9rij398w1b1n6r9'),
    ('protein_adaptation_profiles','health_profile_id','health_profiles','FK3co2sytmx44jr3rineyr12di9'),
    ('supplement_profile_current_supplements','supplement_profile_id','supplement_profiles','FK4s51mkjypku5enxlq5pxooc95'),
    ('supplement_profiles','health_profile_id','health_profiles','FKk4atusv2nwlc2lvip0klpf6ia'),
    ('user_auth','user_id','users','FK9hog4tmdalcddidanohglx6gl'),
    ('workout_profiles','health_profile_id','health_profiles','FKpjckhoe1l7utdw2tkx6fgsfu')
),
cols as (
  select table_name::text as t, column_name::text as c
    from information_schema.columns where table_schema = 'public'
),
g as (
  select fk.*,
         exists (select 1 from cols where t = fk.child_table  and c = fk.child_column) as child_ok,
         exists (select 1 from cols where t = fk.parent_table and c = 'id')            as parent_ok
    from fk
)
select 'S5.3' as section, g.child_table, g.child_column, g.parent_table,
       g.child_ok as child_column_present, g.parent_ok as parent_id_present,
       (xpath('/row/c/text()', query_to_xml(
          case when g.child_ok and g.parent_ok
               then format('select count(*) as c from public.%I ch where ch.%I is not null and not exists (select 1 from public.%I p where p.id = ch.%I)',
                           g.child_table, g.child_column, g.parent_table, g.child_column)
               else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as orphan_rows
  from g
 order by orphan_rows desc nulls first, g.child_table, g.child_column;

-- [S5.4] NULL version values, per table that BaseEntity maps. A NULL @Version makes Spring Data
-- treat a loaded row as "new" or fail the optimistic-lock update; rows with NULL version
-- must be backfilled (to 0) BEFORE the application relies on them.
with t(table_name) as (
  values ('activity_profiles'),('cooking_profiles'),('fitness_goal_profiles'),('health_profiles'),
         ('lifestyle_preference_profiles'),('medical_profiles'),('nutrition_preference_profiles'),
         ('nutrition_targets'),('personal_profiles'),('protein_adaptation_profiles'),
         ('supplement_profiles'),('users'),('workout_profiles')
),
g as (
  select t.table_name,
         exists (select 1 from information_schema.columns c
                  where c.table_schema = 'public' and c.table_name = t.table_name and c.column_name = 'version') as has_version
    from t
)
select 'S5.4' as section, g.table_name, g.has_version as version_column_present,
       (xpath('/row/c/text()', query_to_xml(format('select count(*) as c from public.%I', g.table_name), true, true, '')))[1]::text::bigint as rows_total,
       (xpath('/row/c/text()', query_to_xml(case when g.has_version then format('select count(*) as c from public.%I where version is null', g.table_name) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as null_version_rows
  from g
 where to_regclass(format('public.%I', g.table_name)) is not null
 order by null_version_rows desc nulls first, g.table_name;

-- [S5.5] NULLs in columns the JPA mapping declares NOT NULL. null_rows > 0 means existing
-- rows would violate the entity contract (loading may still work; saving or a NOT NULL
-- constraint will fail). Backfill is a data decision; it BLOCKS adding NOT NULL.
with req(table_name, column_name) as (
  values
    ('activity_profiles','days_per_week'),
    ('activity_profiles','duration_minutes'),
    ('activity_profiles','created_at'),
    ('activity_profiles','updated_at'),
    ('activity_profiles','id'),
    ('activity_profiles','workout_profile_id'),
    ('activity_profiles','activity_type'),
    ('activity_profiles','experience_level'),
    ('activity_profiles','workout_intensity'),
    ('cooking_profile_equipment','cooking_profile_id'),
    ('cooking_profiles','cooking_time_minutes'),
    ('cooking_profiles','meal_prep'),
    ('cooking_profiles','created_at'),
    ('cooking_profiles','updated_at'),
    ('cooking_profiles','health_profile_id'),
    ('cooking_profiles','id'),
    ('cooking_profiles','budget'),
    ('cooking_profiles','cooking_skill'),
    ('fitness_goal_profiles','cheat_meals_per_week'),
    ('fitness_goal_profiles','include_cheat_meals'),
    ('fitness_goal_profiles','created_at'),
    ('fitness_goal_profiles','updated_at'),
    ('fitness_goal_profiles','health_profile_id'),
    ('fitness_goal_profiles','id'),
    ('fitness_goal_profiles','primary_goal'),
    ('health_profiles','onboarding_completed'),
    ('health_profiles','profile_completion_percentage'),
    ('health_profiles','created_at'),
    ('health_profiles','updated_at'),
    ('health_profiles','id'),
    ('health_profiles','user_id'),
    ('lifestyle_preference_profiles','food_delivery_available'),
    ('lifestyle_preference_profiles','hosteller'),
    ('lifestyle_preference_profiles','office_lunch_available'),
    ('lifestyle_preference_profiles','travels_frequently'),
    ('lifestyle_preference_profiles','created_at'),
    ('lifestyle_preference_profiles','updated_at'),
    ('lifestyle_preference_profiles','health_profile_id'),
    ('lifestyle_preference_profiles','id'),
    ('medical_profile_conditions','medical_profile_id'),
    ('medical_profile_food_allergies','medical_profile_id'),
    ('medical_profile_medications','medical_profile_id'),
    ('medical_profiles','created_at'),
    ('medical_profiles','updated_at'),
    ('medical_profiles','health_profile_id'),
    ('medical_profiles','id'),
    ('medical_profiles','pregnancy_status'),
    ('nutrition_preference_profiles','consumes_eggs'),
    ('nutrition_preference_profiles','food_variety_preference'),
    ('nutrition_preference_profiles','meals_per_day'),
    ('nutrition_preference_profiles','created_at'),
    ('nutrition_preference_profiles','updated_at'),
    ('nutrition_preference_profiles','health_profile_id'),
    ('nutrition_preference_profiles','id'),
    ('nutrition_preference_profiles','dairy_preference'),
    ('nutrition_preference_profiles','diet_type'),
    ('nutrition_preference_profiles','preferred_meal_size'),
    ('nutrition_preference_profiles','spice_preference'),
    ('nutrition_preferred_cuisines','nutrition_preference_profile_id'),
    ('nutrition_preferred_cuisines','cuisine'),
    ('nutrition_preferred_protein_sources','nutrition_preference_profile_id'),
    ('nutrition_preferred_protein_sources','protein_source'),
    ('nutrition_religious_restrictions','nutrition_preference_profile_id'),
    ('nutrition_religious_restrictions','religious_restriction'),
    ('nutrition_targets','bmr'),
    ('nutrition_targets','calorie_adjustment'),
    ('nutrition_targets','carbohydrate_grams'),
    ('nutrition_targets','fat_grams'),
    ('nutrition_targets','fiber_grams'),
    ('nutrition_targets','protein_grams'),
    ('nutrition_targets','target_calories'),
    ('nutrition_targets','tdee'),
    ('nutrition_targets','water_ml'),
    ('nutrition_targets','weight_used_kg'),
    ('nutrition_targets','created_at'),
    ('nutrition_targets','updated_at'),
    ('nutrition_targets','health_profile_id'),
    ('nutrition_targets','id'),
    ('nutrition_targets','activity_level'),
    ('nutrition_targets','calculation_method'),
    ('nutrition_taste_preferences','nutrition_preference_profile_id'),
    ('nutrition_taste_preferences','taste_preference'),
    ('payment_webhook_events','received_at'),
    ('payment_webhook_events','id'),
    ('payment_webhook_events','event_type'),
    ('payment_webhook_events','provider_event_id'),
    ('payments','id'),
    ('personal_profiles','height_cm'),
    ('personal_profiles','night_shift'),
    ('personal_profiles','created_at'),
    ('personal_profiles','updated_at'),
    ('personal_profiles','health_profile_id'),
    ('personal_profiles','id'),
    ('personal_profiles','gender'),
    ('protein_adaptation_profiles','current_target_grams'),
    ('protein_adaptation_profiles','days_at_current_target'),
    ('protein_adaptation_profiles','long_term_target_grams'),
    ('protein_adaptation_profiles','created_at'),
    ('protein_adaptation_profiles','updated_at'),
    ('protein_adaptation_profiles','health_profile_id'),
    ('protein_adaptation_profiles','id'),
    ('protein_adaptation_profiles','adaptation_stage'),
    ('protein_adaptation_profiles','tolerance_status'),
    ('subscription_plans','chatbot_daily_limit'),
    ('subscription_plans','is_active'),
    ('subscription_plans','months'),
    ('subscription_plans','price'),
    ('subscription_plans','id'),
    ('supplement_profile_current_supplements','supplement_profile_id'),
    ('supplement_profiles','open_to_supplements'),
    ('supplement_profiles','created_at'),
    ('supplement_profiles','updated_at'),
    ('supplement_profiles','health_profile_id'),
    ('supplement_profiles','id'),
    ('user_auth','user_id'),
    ('user_subscriptions','auto_renew'),
    ('user_subscriptions','id'),
    ('user_subscriptions','user_id'),
    ('user_subscriptions','plan_id'),
    ('users','email_verified'),
    ('users','created_at'),
    ('users','updated_at'),
    ('users','id'),
    ('workout_profiles','include_post_workout_meal'),
    ('workout_profiles','include_pre_workout_meal'),
    ('workout_profiles','created_at'),
    ('workout_profiles','updated_at'),
    ('workout_profiles','health_profile_id'),
    ('workout_profiles','id')
),
g as (
  select r.*,
         exists (select 1 from information_schema.columns c
                  where c.table_schema = 'public' and c.table_name = r.table_name
                    and c.column_name = r.column_name) as col_ok
    from req r
   where to_regclass(format('public.%I', r.table_name)) is not null
)
select 'S5.5' as section, g.table_name, g.column_name, g.col_ok as column_present,
       (xpath('/row/c/text()', query_to_xml(case when g.col_ok then format('select count(*) as c from public.%I where %I is null', g.table_name, g.column_name) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as null_rows
  from g
 order by null_rows desc nulls first, g.table_name, g.column_name;

-- [S5.6] ENUM VALUES stored, versus the current Java enums. unexpected_rows > 0 means a stored
-- string is not a constant of the enum: Hibernate throws on read. The distinct unexpected
-- strings (at most 10 per column) are shown; they are enum names, never personal data.
with ex(table_name, column_name, enum_values) as (
  values
    ('activity_profiles','activity_type',array['SEDENTARY','WALKING','GYM','RUNNING','CYCLING','SWIMMING','YOGA','PILATES','HOME_WORKOUT','SPORTS','MARTIAL_ARTS','PHYSICALLY_DEMANDING_JOB']),
    ('activity_profiles','experience_level',array['LESS_THAN_A_YEAR','ONE_TO_TWO_YEARS','TWO_TO_FIVE_YEARS','FIVE_PLUS_YEARS']),
    ('activity_profiles','sport_type',array['CRICKET','FOOTBALL','BADMINTON','TENNIS','BASKETBALL','VOLLEYBALL','HOCKEY','KABADDI','ATHLETICS','OTHER']),
    ('activity_profiles','workout_intensity',array['LOW','MODERATE','HIGH','ATHLETE']),
    ('cooking_profile_equipment','equipment',array['GAS_STOVE','INDUCTION','MICROWAVE','AIR_FRYER','PRESSURE_COOKER','RICE_COOKER','ELECTRIC_KETTLE','MIXER_GRINDER','OVEN']),
    ('cooking_profiles','budget',array['LOW','MEDIUM','HIGH','PREMIUM']),
    ('cooking_profiles','cooking_skill',array['NONE','BEGINNER','INTERMEDIATE','ADVANCED']),
    ('fitness_goal_profiles','diet_flexibility',array['NEVER','OCCASIONALLY','WEEKLY','FREQUENTLY']),
    ('fitness_goal_profiles','diet_strictness',array['STRICT','BALANCED','FLEXIBLE']),
    ('fitness_goal_profiles','primary_goal',array['FAT_LOSS','MUSCLE_GAIN','BODY_RECOMPOSITION','WEIGHT_GAIN','MAINTENANCE','GENERAL_HEALTH','SPORTS_PERFORMANCE']),
    ('fitness_goal_profiles','secondary_goal',array['FAT_LOSS','MUSCLE_GAIN','BODY_RECOMPOSITION','WEIGHT_GAIN','MAINTENANCE','GENERAL_HEALTH','SPORTS_PERFORMANCE']),
    ('medical_profile_conditions','medical_condition',array['DIABETES','PREDIABETES','HYPERTENSION','HIGH_CHOLESTEROL','FATTY_LIVER','HYPOTHYROID','HYPERTHYROID','PCOS','IBS','GERD','CELIAC','GOUT','KIDNEY_DISEASE','LIVER_DISEASE']),
    ('medical_profile_food_allergies','food_allergy',array['MILK','LACTOSE','PEANUT','TREE_NUT','SOY','EGG','GLUTEN','FISH','SHELLFISH','SESAME','MUSTARD']),
    ('medical_profiles','pregnancy_status',array['NOT_APPLICABLE']),
    ('nutrition_preference_profiles','dairy_preference',array['YES','NO','LIMITED']),
    ('nutrition_preference_profiles','diet_type',array['VEG','EGGETARIAN','NON_VEG','VEGAN','JAIN','SATVIK']),
    ('nutrition_preference_profiles','preferred_meal_size',array['SMALL','MEDIUM','LARGE']),
    ('nutrition_preference_profiles','spice_preference',array['MILD','MEDIUM','SPICY']),
    ('nutrition_preferred_cuisines','cuisine',array['NORTH_INDIAN','SOUTH_INDIAN','PUNJABI','GUJARATI','RAJASTHANI','MAHARASHTRIAN','BENGALI','KASHMIRI','BIHARI','ODISHA','GOAN','HYDERABADI','ASSAMESE','NORTH_EAST','MIXED']),
    ('nutrition_preferred_protein_sources','protein_source',array['EGGS','CHICKEN','FISH','MUTTON','PANEER','MILK','CURD','WHEY','SOY','TOFU','DAL','CHICKPEAS','RAJMA','LENTILS','PEANUTS','NUTS']),
    ('nutrition_religious_restrictions','religious_restriction',array['NO_BEEF','NO_PORK','NO_ONION_GARLIC','NAVRATRI_FAST','EKADASHI_FAST']),
    ('nutrition_targets','activity_level',array['SEDENTARY','LIGHTLY_ACTIVE','MODERATELY_ACTIVE','VERY_ACTIVE','EXTREMELY_ACTIVE']),
    ('nutrition_targets','calculation_method',array['MIFFLIN_ST_JEOR','HARRIS_BENEDICT']),
    ('nutrition_taste_preferences','taste_preference',array['SPICY','MILD','SWEET','SAVORY','SOUR','BITTER']),
    ('personal_profiles','gender',array['MALE','FEMALE']),
    ('personal_profiles','living_arrangement',array['FAMILY','ALONE','HOSTEL','PG','SHARED_FLAT']),
    ('personal_profiles','occupation',array['OFFICE','WFH','HOSTEL_STUDENT','COLLEGE_STUDENT','HOME_MAKER','RETIRED','SHIFT_WORKER','MANUAL_LABOUR']),
    ('personal_profiles','region',array['NORTH','SOUTH','EAST','WEST','CENTRAL','NORTH_EAST']),
    ('personal_profiles','state',array['ANDHRA_PRADESH','ARUNACHAL_PRADESH','ASSAM','BIHAR','CHHATTISGARH','GOA','GUJARAT','HARYANA','HIMACHAL_PRADESH','JHARKHAND','KARNATAKA','KERALA','MADHYA_PRADESH','MAHARASHTRA','MANIPUR','MEGHALAYA','MIZORAM','NAGALAND','ODISHA','PUNJAB','RAJASTHAN','SIKKIM','TAMIL_NADU','TELANGANA','TRIPURA','UTTAR_PRADESH','UTTARAKHAND','WEST_BENGAL','DELHI','JAMMU_KASHMIR','LADAKH','CHANDIGARH','PUDUCHERRY','DADRA_NAGAR_HAVELI','DAMAN_DIU','LAKSHADWEEP','ANDAMAN_NICOBAR']),
    ('protein_adaptation_profiles','adaptation_stage',array['NOT_STARTED','STARTING','ADAPTING','TARGET_REACHED']),
    ('protein_adaptation_profiles','tolerance_status',array['UNKNOWN','GOOD','MILD_ISSUES','SIGNIFICANT_ISSUES']),
    ('supplement_profile_current_supplements','supplement',array['WHEY','CASEIN','CREATINE','MULTIVITAMIN','FISH_OIL','VITAMIN_D','VITAMIN_B12','MAGNESIUM','ELECTROLYTES','PROBIOTIC']),
    ('workout_profiles','training_status',array['BEGINNER','INTERMEDIATE','ADVANCED'])
),
g as (
  select ex.*,
         exists (select 1 from information_schema.columns c
                  where c.table_schema = 'public' and c.table_name = ex.table_name
                    and c.column_name = ex.column_name) as col_ok
    from ex
   where to_regclass(format('public.%I', ex.table_name)) is not null
)
select 'S5.6' as section, g.table_name, g.column_name, g.col_ok as column_present,
       (xpath('/row/c/text()', query_to_xml(case when g.col_ok then format('select count(*) as c from public.%I where %I is not null and %I::text <> all (%L::text[])', g.table_name, g.column_name, g.column_name, g.enum_values) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as unexpected_rows,
       (xpath('/row/c/text()', query_to_xml(case when g.col_ok then format('select string_agg(v, '','' order by v) as c from (select distinct %I::text as v from public.%I where %I is not null and %I::text <> all (%L::text[]) limit 10) d', g.column_name, g.table_name, g.column_name, g.column_name, g.enum_values) else 'select null::text as c' end, true, true, '')))[1]::text as unexpected_values
  from g
 order by unexpected_rows desc nulls first, g.table_name, g.column_name;

-- [S5.6b] nutrition_preference_profiles.food_variety_preference. The entity omits
-- @Enumerated(STRING), so Hibernate stores and reads the ORDINAL (smallint 0..2), unlike every
-- other enum column. If the DB column is text (or holds 'LOW'/'MEDIUM'/'HIGH'), reads fail.
select 'S5.6b' as section,
       c.data_type as actual_column_type,
       'smallint' as type_hibernate_expects,
       (xpath('/row/c/text()', query_to_xml(
          case when c.column_name is not null
               then 'select string_agg(v || '':'' || n, '', '' order by v) as c from (select food_variety_preference::text as v, count(*) as n from public.nutrition_preference_profiles group by 1) d'
               else 'select null::text as c' end, true, true, '')))[1]::text as value_distribution,
       case when c.column_name is null then 'column absent'
            when c.data_type = 'smallint' then 'type ok'
            else 'TYPE MISMATCH with ordinal mapping' end as verdict
  from (select 1) one
  left join information_schema.columns c
    on c.table_schema = 'public' and c.table_name = 'nutrition_preference_profiles'
   and c.column_name = 'food_variety_preference';

-- [S5.7] Legacy (unmapped) columns of health_profiles that still hold data. These come from V3, V17
-- and an earlier experiment. rows_with_value > 0 means a reconcile migration must keep the column.
with legacy(col) as (
  values ('age'),('gender'),('height_cm'),('weight_kg'),('goal'),('training_level'),('activity_level'),
         ('diet_type'),('meals_per_day'),('origin'),('allergies'),('diseases'),('medical_conditions'),
         ('medications'),('preferred_cuisines'),('taste_preferences'),('favourite_foods'),
         ('disliked_foods'),('religious_restrictions'),('equipments'),('current_supplements'),
         ('current_protein_intake')
),
g as (
  select l.col,
         exists (select 1 from information_schema.columns c
                  where c.table_schema = 'public' and c.table_name = 'health_profiles' and c.column_name = l.col) as col_ok
    from legacy l
)
select 'S5.7' as section, g.col as legacy_column, g.col_ok as column_present,
       (xpath('/row/c/text()', query_to_xml(case when g.col_ok then format('select count(%I) as c from public.health_profiles', g.col) else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as rows_with_value
  from g
 order by g.col_ok desc, rows_with_value desc nulls last, g.col;


-- #############################################################################
-- S6  GATES (catalog-only, never errors)
-- #############################################################################

-- [S6.1] One row per pre-condition. passed = false on a BLOCKER row means: do not write V100 yet.
-- Data gates (duplicates, orphans, NULLs, enum values) are in S5; see the README for the mapping.
with hp as (select to_regclass('public.health_profiles') as oid),
col as (
  select table_name::text as t, column_name::text as c, data_type::text as dt
    from information_schema.columns where table_schema = 'public'
),
uniq as (
  select i.indrelid as rel, a.attname::text as c, i.indisprimary as is_pk
    from pg_index i
    join pg_attribute a on a.attrelid = i.indrelid and a.attnum = i.indkey[0]
    join pg_class ic on ic.oid = i.indexrelid
   where i.indisunique and i.indnkeyatts = 1 and i.indpred is null and i.indexprs is null
     and ic.relnamespace = 'public'::regnamespace
),
mapped(t) as (
  values ('activity_profiles'),('cooking_profile_equipment'),('cooking_profiles'),('fitness_goal_profiles'),
         ('health_profiles'),('lifestyle_preference_profiles'),('medical_profile_conditions'),
         ('medical_profile_food_allergies'),('medical_profile_medications'),('medical_profiles'),
         ('nutrition_preference_profiles'),('nutrition_preferred_cuisines'),('nutrition_preferred_protein_sources'),
         ('nutrition_religious_restrictions'),('nutrition_targets'),('nutrition_taste_preferences'),
         ('payment_webhook_events'),('payments'),('personal_profiles'),('protein_adaptation_profiles'),
         ('subscription_plans'),('supplement_profile_current_supplements'),('supplement_profiles'),
         ('user_auth'),('user_subscriptions'),('users'),('workout_profiles')
),
fh as (select to_regclass('public.flyway_schema_history') is not null as present),
failed as (
  select (xpath('/row/c/text()', query_to_xml(
           case when (select present from fh)
                then 'select count(*) as c from public.flyway_schema_history where not success'
                else 'select null::bigint as c' end, true, true, '')))[1]::text::bigint as n
),
missing_tables as (
  select count(*) as n, string_agg(m.t, ',' order by m.t) as names
    from mapped m where to_regclass(format('public.%I', m.t)) is null
),
unvalidated as (
  select count(*) as n from pg_constraint
   where connamespace = 'public'::regnamespace and not convalidated
)
select 'S6.1' as section, g.gate, g.severity, g.passed, g.observed, g.if_failed
  from (values
    ('G01 Flyway history table exists', 'BLOCKER',
       (select present from fh), (select present from fh)::text,
       'database is not Flyway-managed: the whole V100 strategy must be re-planned'),
    ('G02 no failed rows in Flyway history', 'BLOCKER',
       coalesce((select n from failed) = 0, false), coalesce((select n from failed)::text, 'history table absent'),
       'a failed migration row makes Flyway refuse to start until repaired'),
    ('G03 health_profiles exists', 'BLOCKER',
       (select oid is not null from hp), (select oid is not null from hp)::text,
       'nothing else in this pack is meaningful'),
    ('G04 health_profiles.id column exists', 'BLOCKER',
       exists (select 1 from col where t = 'health_profiles' and c = 'id'),
       exists (select 1 from col where t = 'health_profiles' and c = 'id')::text,
       'V3 shape: HealthProfile cannot load; V31 FK to health_profiles(id) cannot be created'),
    ('G05 health_profiles.id is PRIMARY KEY or UNIQUE', 'BLOCKER',
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'id'),
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'id')::text,
       'every FK to health_profiles(id) will be rejected by PostgreSQL'),
    ('G06 health_profiles.id is the primary key', 'WARNING',
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'id' and uniq.is_pk),
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'id' and uniq.is_pk)::text,
       'unique but not the PK: works for FKs, but differs from the JPA/Hibernate model'),
    ('G07 health_profiles.version exists', 'BLOCKER',
       exists (select 1 from col where t = 'health_profiles' and c = 'version'),
       exists (select 1 from col where t = 'health_profiles' and c = 'version')::text,
       'every health_profiles UPDATE fails'),
    ('G08 users.version exists', 'BLOCKER',
       exists (select 1 from col where t = 'users' and c = 'version'),
       exists (select 1 from col where t = 'users' and c = 'version')::text,
       'every users UPDATE fails (V1 never created it)'),
    ('G09 health_profiles.user_id is UNIQUE', 'BLOCKER',
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'user_id'),
       exists (select 1 from uniq, hp where uniq.rel = hp.oid and uniq.c = 'user_id')::text,
       'one-profile-per-user is not enforced; duplicates possible (see S2.7a)'),
    ('G10 health_profiles.allergies type (shape indicator)', 'INFO',
       true,
       coalesce((select dt from col where t = 'health_profiles' and c = 'allergies'), '<column absent>'),
       'ARRAY = V3 shape (V17 could not have run); jsonb = V17 shape; absent = neither'),
    ('G11 every JPA-mapped table exists', 'BLOCKER',
       (select n from missing_tables) = 0,
       coalesce((select names from missing_tables), 'none missing'),
       'ddl-auto=validate would fail; V100 must create these (fresh) or the app must not need them'),
    ('G12 workout_profiles exists (V32 prerequisite)', 'BLOCKER',
       to_regclass('public.workout_profiles') is not null, (to_regclass('public.workout_profiles') is not null)::text,
       'V32 as written would fail'),
    ('G13 protein_adaptation_profiles exists', 'INFO',
       true, (to_regclass('public.protein_adaptation_profiles') is not null)::text,
       'true: V31 as written would fail (CREATE TABLE without IF NOT EXISTS); false: V31 is still needed'),
    ('G14 protein_adaptation_profiles.health_profile_id is UNIQUE', 'BLOCKER',
       exists (select 1 from uniq where uniq.rel = to_regclass('public.protein_adaptation_profiles') and uniq.c = 'health_profile_id'),
       exists (select 1 from uniq where uniq.rel = to_regclass('public.protein_adaptation_profiles') and uniq.c = 'health_profile_id')::text,
       'the first-insert race fix (409 on duplicate) relies on this; without it duplicates become possible'),
    ('G15 gen_random_uuid() available', 'WARNING',
       exists (select 1 from pg_proc where proname = 'gen_random_uuid'),
       exists (select 1 from pg_proc where proname = 'gen_random_uuid')::text,
       'V31 uses it as a column default'),
    ('G16 server_version_num >= 130000', 'WARNING',
       current_setting('server_version_num')::int >= 130000, current_setting('server_version'),
       'gen_random_uuid() is only built in from PostgreSQL 13'),
    ('G17 no NOT VALID constraints in schema public', 'WARNING',
       (select n from unvalidated) = 0, (select n from unvalidated)::text,
       'earlier NOT VALID constraints were never validated; list them in S2.2, S3.3, S4.3'),
    ('G18 connected role bypasses RLS or tables have RLS off', 'INFO',
       (select rolbypassrls or rolsuper from pg_roles where rolname = current_user),
       (select (rolbypassrls or rolsuper)::text from pg_roles where rolname = current_user),
       'false: row counts in S4/S5 may be FILTERED by row-level security; treat them as lower bounds'),
    ('G19 session is read-only', 'INFO',
       current_setting('transaction_read_only') = 'on', current_setting('transaction_read_only'),
       'informational: this pack never writes, but a read-only session is the safest way to run it')
  ) as g(gate, severity, passed, observed, if_failed)
 order by case g.severity when 'BLOCKER' then 1 when 'WARNING' then 2 else 3 end, g.gate;

-- =============================================================================
-- END OF PACK. Nothing above modifies the database.
-- =============================================================================
