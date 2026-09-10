create table employer (
    id         uuid        primary key,
    name       text        not null,
    created_at timestamptz not null
);

create table employee (
    id                    uuid    primary key,
    employer_id           uuid    not null references employer (id),
    personnel_number      text    not null,
    first_name            text    not null,
    last_name             text    not null,
    external_employee_ref text,
    active                boolean not null default true,
    constraint uk_employee_personnel_number unique (employer_id, personnel_number),
    constraint uk_employee_external_ref unique (employer_id, external_employee_ref)
);

-- One row per employee and calendar month. The unique constraint is the actual guard
-- of that invariant, independent of the writing code path (ADR 0004).
create table monthly_working_hours (
    id             uuid        primary key,
    employee_id    uuid        not null references employee (id),
    period         date        not null,
    minutes_worked integer     not null,
    last_source    text        not null,
    version        bigint      not null,
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint uk_monthly_working_hours_employee_period unique (employee_id, period),
    constraint ck_monthly_working_hours_period_normalised check (period = date_trunc('month', period)),
    constraint ck_monthly_working_hours_minutes check (minutes_worked between 0 and 44640),
    constraint ck_monthly_working_hours_source check (last_source in ('MANUAL', 'TIME_TRACKING'))
);


