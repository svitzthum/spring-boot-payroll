# Payroll Demo

A Spring Boot 4 demo application for recording monthly working hours in a
payroll system. It is the solution to a coding assignment; the original task
description is stored in [docs/](./docs/Aufgabe_Java_Softwareentwickler_in.pdf).

## Scenario

Managing directors record the hours actually worked by their employees per
calendar month through a REST endpoint. In addition, an external time tracking
system delivers recorded times at regular intervals. Both sources write the same
value, so the application has to guarantee data integrity when the same employee
and period are processed concurrently.

## Tech stack

- Java 26, Spring Boot 4.1
- Spring Web MVC, Spring Data JPA
- PostgreSQL with Flyway migrations
- Gradle, JUnit 5, Testcontainers, ArchUnit

## Prerequisites

- JDK 26
- Docker, for the database and for the Testcontainers based tests

## Architecture

The application is built as a hexagon (ports and adapters): the business rules
live in a framework-free core, the REST endpoint and the scheduled importer are
driving adapters onto the same inbound port, and PostgreSQL and the external
time tracking system sit behind outbound ports. See
[docs/02-architecture.md](./docs/02-architecture.md).

For a single value per employee and month this is more structure than strictly
necessary. It was chosen because the same data is written through two
independent channels and has to stay consistent when they collide — that rule
is implemented once and both adapters share it, and it can be tested without a
database. The trade-off is discussed in
[ADR 0005](./docs/adr/0005-hexagonal-architecture.md).

## Getting started

```shell
docker compose up -d   # PostgreSQL
./gradlew bootRun
```

```shell
./gradlew build        # includes integration tests via Testcontainers
```

## Documentation

The planning documents, the data model and the architecture decision records
live in [docs/](./docs/README.md).

| Document | Content |
| --- | --- |
| [docs/README.md](./docs/README.md) | Index and approach |
| [docs/01-requirements.md](./docs/01-requirements.md) | Requirements and their interpretation |
| [docs/02-architecture.md](./docs/02-architecture.md) | Hexagonal architecture and package structure |
| [docs/03-data-model.md](./docs/03-data-model.md) | Data model and invariants |
| [docs/04-iteration-1-manual-entry.md](./docs/04-iteration-1-manual-entry.md) | Iteration 1: manual entry |
| [docs/05-iteration-2-time-tracking-import.md](./docs/05-iteration-2-time-tracking-import.md) | Iteration 2: time tracking import |
| [docs/adr/](./docs/adr) | Architecture decision records |


