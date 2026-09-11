# Iteration 2 — Scheduled Import from the External Time Tracking System

## Goal

The second write path required by the assignment: a background process regularly
pulls recorded times from a fictitious external time tracking system and writes
them through the same inbound port the REST adapter uses, without ever producing
a lost update or a duplicate row for the same employee and month.

## In scope

- Driving adapter `adapter.in.scheduler`: a scheduled job that triggers the
  import, with a configurable interval and no business logic of its own.
- Outbound port `TimeTrackingSystem` plus an adapter for the fictitious system,
  so the core does not know how the data arrives.
- Import journal (`time_tracking_import`) with a unique external event id, so a
  repeated delivery cannot be applied twice.
- Source precedence between manual entry and import, implemented in the
  application layer and covered by tests.
- Change history (`working_hours_revision`): which source changed the monthly
  value when and to what.
- Tests: fake time tracking system in the application tests, adapter tests for
  the journal, an integration test in which manual entry and import write the
  same employee and month, and the scheduler wiring itself.

## Out of scope

- A real integration with a real product; the external system stays fictitious.
- Security, authentication and tenant isolation.

## Decisions

- Manual entry wins over the import —
  [ADR 0006](./adr/0006-manual-entry-wins-over-import.md).
- The import is idempotent through its journal, without a scheduler lock —
  [ADR 0007](./adr/0007-idempotent-import-without-scheduler-lock.md).
- The external system is simulated inside its adapter: ordinary production code,
  not a test mock, so the application runs with `docker compose up` alone. A real
  HTTP client would be a second implementation of the same port.

## Steps

1. **Schema** — migration `V3__time_tracking_import.sql` with
   `time_tracking_import` (unique `external_event_id`, status) and
   `working_hours_revision` (append-only), both already sketched in
   [the data model](./03-data-model.md).
2. **Application** — outbound port `TimeTrackingSystem` returning reported times
   per external employee reference and period, inbound port
   `ImportTimeTrackingUseCase`, and the precedence rule in the service.
   `RecordWorkingHoursCommand` already carries the source.
3. **Time tracking adapter** (`adapter.out.timetracking`) — implementation of
   the port that simulates the fictitious system, mapping its employee reference
   to `employee.external_employee_ref`.
4. **Import journal** — persistence adapter for the journal so an event that was
   already applied is skipped; the unique constraint is the guard.
5. **Scheduler adapter** (`adapter.in.scheduler`) — `@Scheduled` job with the
   interval from configuration, error handling and logging, delegating to the
   inbound port.
6. **Tests and guardrails** — application tests for precedence and idempotency
   against a fake time tracking system, an integration test writing the same
   month from both paths, and the ArchUnit rules extended to the new packages.

## Definition of done

- `./gradlew build` passes, including the new integration tests.
- Running the application imports data without a manual trigger.
- A repeated delivery of the same event does not change the stored value twice.
- Manual entry and import writing the same employee and month behave as decided
  in ADR 0006, proven by a test.
