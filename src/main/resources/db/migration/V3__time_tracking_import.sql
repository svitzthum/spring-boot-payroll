-- Journal of the events received from the external time tracking system. The unique
-- external_event_id is what makes the import idempotent: a redelivery, a retry after a
-- failure or a second application instance cannot apply the same event twice (ADR 0007).
-- There is deliberately no foreign key to employee, so events with an unresolvable
-- reference can be recorded as FAILED instead of being lost.
create table time_tracking_import (
    id                    uuid        primary key,
    external_event_id     text        not null,
    external_employee_ref text        not null,
    period                date        not null,
    minutes_worked        integer     not null,
    status                text        not null,
    imported_at           timestamptz not null,
    detail                text,
    constraint uk_time_tracking_import_event unique (external_event_id),
    constraint ck_time_tracking_import_period_normalised check (period = date_trunc('month', period)),
    constraint ck_time_tracking_import_minutes check (minutes_worked between 0 and 44640),
    constraint ck_time_tracking_import_status check (status in ('APPLIED', 'SKIPPED', 'FAILED'))
);

-- Append-only history of the monthly value: which source set which value when.
create table working_hours_revision (
    id               uuid        primary key,
    working_hours_id uuid        not null references monthly_working_hours (id) on delete cascade,
    minutes_worked   integer     not null,
    source           text        not null,
    changed_at       timestamptz not null,
    constraint ck_working_hours_revision_minutes check (minutes_worked between 0 and 44640),
    constraint ck_working_hours_revision_source check (source in ('MANUAL', 'TIME_TRACKING'))
);

create index ix_working_hours_revision_history on working_hours_revision (working_hours_id, changed_at);

