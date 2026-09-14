# Documentation

This folder documents how the payroll demo application was planned and built.
It is part of the deliverable: it shows the reasoning behind the implementation,
not just the result.

## Original assignment

[Aufgabe_Java_Softwareentwickler_in.pdf](./Aufgabe_Java_Softwareentwickler_in.pdf)

## Approach

The application is built in vertical slices. Each iteration delivers a complete,
working path through all layers (database, service, API, tests) instead of one
horizontal layer at a time. Scope that is deliberately postponed is listed
explicitly per iteration.

| Iteration | Topic | Status |
| --- | --- | --- |
| 1 | Manual monthly working hours entry via REST | done |
| 2 | Scheduled import from external time tracking system | done |

## Core decisions

The application records one authoritative value of hours actually worked per
employee and calendar month, written through two independent paths: a REST
endpoint and a scheduled import. Most decisions below follow from that single
sentence. Each one links to the document or ADR holding the full reasoning,
including the options that were considered.

| Decision | Why | Alternative rejected |
| --- | --- | --- |
| [One authoritative row per employee and month](./03-data-model.md), enforced by `UNIQUE (employee_id, period)` | The payroll-relevant value has to be unambiguous, regardless of which path wrote it | One row per source: leaves open which value applies |
| [Writes are idempotent](./01-requirements.md): they carry an absolute value, not a delta | A repeated `PUT` and a re-run of the import job are both harmless | Delta writes: a retry silently doubles the result |
| [Hexagonal architecture in a single Gradle module](./adr/0005-hexagonal-architecture.md), enforced by ArchUnit | The core owns the interfaces to database and time tracking system, so the fictitious adapter is swappable and the business rules are testable without Spring or a database | Layered: shares the rules just as well, but couples them to JPA and the tracking client |
| [Optimistic locking](./adr/0004-unique-constraint-and-optimistic-locking.md) instead of pessimistic locking | Lost updates are detected, not prevented: REST answers `409 Conflict`, the import retries with fresh state | Pessimistic locking: serialises writers for a rare conflict |
| [Manual entry wins over the import](./adr/0006-manual-entry-wins-over-import.md) | Manual entry is the correction path for incomplete tracked data; a routine job must not undo a correction | Last writer wins: the import would silently revert corrections |
| [Idempotency via a unique `external_event_id`](./adr/0007-idempotent-import-without-scheduler-lock.md), not a scheduler lock | One mechanism covers concurrent runs, retries and redelivery alike | Scheduler lock: an optimisation, but no correctness guarantee |
| [Integer minutes in the database, ISO 8601 in the API](./adr/0003-store-durations-as-minutes.md) | Exact arithmetic when aggregating; `PT152H30M` is self-describing, `7.5` is not | Decimal hours: rounding errors and an ambiguous unit |

## Deliberately out of scope

Authentication and authorization, tenant isolation, a user interface, and the
payroll calculation itself. Each is a topic of its own and none of them changes
the concurrency and multi-source questions this demo is about.

## Documents

| Document | Content |
| --- | --- |
| [01-requirements.md](./01-requirements.md) | Assignment summary and interpretation of the requirements |
| [02-architecture.md](./02-architecture.md) | Hexagonal architecture, package structure, dependency rule |
| [03-data-model.md](./03-data-model.md) | Target data model, constraints and invariants |
| [04-iteration-1-manual-entry.md](./04-iteration-1-manual-entry.md) | Scope and steps of iteration 1 |
| [05-iteration-2-time-tracking-import.md](./05-iteration-2-time-tracking-import.md) | Scope and steps of iteration 2 |

## Conventions

- Documentation, code, commit messages and identifiers are written in English.
- Documentation and the corresponding code are committed together per iteration.
- Decisions are recorded as ADRs in [`adr/`](./adr) in a lightweight [MADR](https://adr.github.io/madr/) style: context, decision, consequences.
- New decisions are added as a new ADR; existing ADRs are superseded, not rewritten. Factual corrections are applied in place.
