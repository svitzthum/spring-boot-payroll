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
| 1 | Manual monthly working hours entry via REST | planned |
| 2 | Scheduled import from external time tracking system | not started |

## Documents

| Document | Content |
| --- | --- |
| [01-requirements.md](./01-requirements.md) | Assignment summary and interpretation of the requirements |
| [02-architecture.md](./02-architecture.md) | Hexagonal architecture, package structure, dependency rule |
| [03-data-model.md](./03-data-model.md) | Target data model, constraints and invariants |
| [04-iteration-1-manual-entry.md](./04-iteration-1-manual-entry.md) | Scope and steps of iteration 1 |
| [05-iteration-2-time-tracking-import.md](./05-iteration-2-time-tracking-import.md) | Scope of iteration 2 (placeholder) |

## Architecture Decision Records

Short records in a lightweight [MADR](https://adr.github.io/madr/) style:
context, decision, consequences.

| ADR | Decision |
| --- | --- |
| [0001](./adr/0001-postgresql-with-flyway.md) | PostgreSQL with Flyway migrations instead of Hibernate schema generation |
| [0002](./adr/0002-record-actual-hours-worked.md) | The recorded value is actual hours worked, not contracted hours |
| [0003](./adr/0003-store-durations-as-minutes.md) | ISO 8601 durations in the API, integer minutes in the database |
| [0004](./adr/0004-unique-constraint-and-optimistic-locking.md) | Data integrity via unique constraint and optimistic locking |
| [0005](./adr/0005-hexagonal-architecture.md) | Hexagonal architecture in a single Gradle module |

## Conventions

- Documentation, code, commit messages and identifiers are written in English.
- Documentation and the corresponding code are committed together per iteration.
- New decisions are added as a new ADR; existing ADRs are superseded, not rewritten.



