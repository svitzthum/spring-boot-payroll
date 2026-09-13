# Architecture

The application follows a **hexagonal architecture** (ports and adapters). The
decision and its trade-offs are recorded in
[ADR 0005](./adr/0005-hexagonal-architecture.md).

## Why it fits this assignment

The assignment describes two independent ways to write the same data — a REST
endpoint used by the managing director and a scheduled import from an external
time tracking system — plus one external system to read from. That is exactly
the situation ports and adapters is designed for: the business rule "one
authoritative value per employee and month, written safely under concurrency"
is implemented **once** in the application core and driven from both sides,
instead of being duplicated in a controller and a scheduled job.

```mermaid
flowchart LR
    subgraph driving["Driving adapters"]
        REST["REST controller<br/>(iteration 1)"]
        SCHED["Scheduled importer<br/>(iteration 2)"]
    end

    subgraph core["Application core"]
        IN["Inbound ports<br/>RecordWorkingHours / GetWorkingHours / ImportTimeTracking"]
        APP["Application services"]
        DOM["Domain model<br/>MonthlyWorkingHours, WorkDuration"]
        OUT["Outbound ports<br/>WorkingHoursRepository, EmployeeDirectory,<br/>TimeTrackingSystem, TimeTrackingImportJournal"]
        IN --> APP --> DOM
        APP --> OUT
    end

    subgraph driven["Driven adapters"]
        JPA["JPA / PostgreSQL"]
        EXT["Time tracking client<br/>(iteration 2)"]
    end

    REST --> IN
    SCHED --> IN
    OUT --> JPA
    OUT --> EXT
```

## Package structure

One Gradle module. The hexagon boundary is expressed by packages and enforced by
an ArchUnit test, not by build modules — that keeps the demo readable while the
dependency rule stays verifiable.

```
dev.svitzthum.payroll
├── PayrollApplication
├── workinghours                        ← feature slice
│   ├── domain                          ← no framework dependencies at all
│   │   ├── MonthlyWorkingHours         ← aggregate, holds the invariants
│   │   ├── WorkDuration                ← value object, whole minutes, built from java.time.Duration
│   │   └── WorkingHoursSource          ← MANUAL | TIME_TRACKING
│   ├── application
│   │   ├── port/in                     ← use case interfaces + commands
│   │   ├── port/out                    ← repository / directory interfaces
│   │   └── service                     ← use case implementations, @Transactional
│   └── adapter
│       ├── in/web                      ← controller, DTOs, exception handling
│       ├── in/scheduler                ← scheduled import job (iteration 2)
│       ├── out/persistence             ← JPA entities, Spring Data, mapper
│       └── out/timetracking            ← simulated external system (iteration 2)
└── shared                              ← cross-cutting configuration
```

## Dependency rule

- `domain` depends on nothing but the JDK. No Spring, no JPA, no Jackson.
- `application` depends on `domain` and on its own ports only.
- `adapter` depends on `application` and `domain`; adapters never depend on
  each other.
- Nothing outside `adapter.out.persistence` sees a JPA entity, nothing outside
  `adapter.in.web` sees a DTO.

An ArchUnit test asserts these rules so a violation fails the build instead of
being caught in review.

## Consequences accepted

- **Two models for the monthly value**: a domain aggregate and a JPA entity,
  with an explicit mapper. This costs a mapper class but keeps the persistence
  schema from dictating the domain model — and it makes the optimistic locking
  version an infrastructure concern rather than a business attribute.
- **Interfaces only where a boundary exists**: ports are interfaces because they
  are crossed by different adapters. Internal helper classes are not abstracted
  behind interfaces just for symmetry.
- **Transactions live in the application services**, i.e. at the inbound port
  boundary, so both the REST adapter and the scheduler get identical semantics.

