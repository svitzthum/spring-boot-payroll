# ADR 0004 — Data integrity via unique constraint and optimistic locking

- Status: accepted
- Date: 2026-09-09

## Context

The assignment explicitly requires data integrity when data for the same period
and the same employee is processed concurrently. Two write paths exist: the REST
endpoint used by the managing director and the scheduled import from the time
tracking system. Both may run at the same time, and the application may run in
multiple instances, so in-process synchronisation is not sufficient.

## Options considered

- **Unique constraint plus optimistic locking (`@Version`) with retry** — lost
  updates are detected by the database, conflicts surface as a `409 Conflict`
  for interactive callers and as a retry for the background job.
- **Pessimistic locking (`SELECT … FOR UPDATE`)** — simple to reason about, but
  serialises writers and holds database locks for the duration of the
  transaction.
- **Blind upsert (`INSERT … ON CONFLICT DO UPDATE`)** — atomic and fast, but
  "last writer wins" silently discards a concurrent change, which conflicts with
  the precedence rules planned for iteration 2.

## Decision

Enforce the invariant "one row per employee and month" with a unique constraint
on `(employee_id, period)` in the database, and detect concurrent modifications
with an optimistic locking `version` column. The REST path reports a conflict to
the caller, the background import retries with fresh state.

## Consequences

- Correctness does not depend on the calling code path or on a single
  application instance.
- Concurrent inserts of the same employee and month result in a constraint
  violation that must be translated into a domain level conflict.
- Retry handling for the background job needs to be implemented and tested with
  a concurrent test that hits the same employee and period from several threads.

