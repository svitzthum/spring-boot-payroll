# ADR 0007 — Idempotent import instead of a scheduler lock

- Status: accepted
- Date: 2026-09-11

## Context

The scheduled import runs on every application instance. It may therefore be
executed twice at the same time, and it may be repeated after a failure or
because the external system delivers an event again.

## Options considered

- **Scheduler lock** (ShedLock, or a PostgreSQL advisory lock) — prevents the
  duplicate run, but does not make a retry or a redelivery safe.
- **Import journal with a unique external event id** — a second attempt to apply
  the same event is recognised and skipped, whatever caused it.

## Decision

Make the import idempotent through the `time_tracking_import` journal, with a
unique constraint on `external_event_id`, and run without a scheduler lock.

## Consequences

- Duplicate execution is harmless, no matter whether it comes from a second
  instance, a retry or a redelivery.
- On several instances the same work is done more than once. Adding a scheduler
  lock later would be an optimisation and would not change the guarantees.
- The journal also documents what arrived and how it was handled.

