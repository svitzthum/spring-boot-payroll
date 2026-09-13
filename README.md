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
time tracking system sit behind outbound ports.

For a single value per employee and month this is more structure than strictly
necessary. It was chosen because the same data is written through two
independent channels and has to stay consistent when they collide — that rule
is implemented once and both adapters share it, and it can be tested without a
database. This and the other trade-offs are listed under
[core decisions](./docs/README.md#core-decisions).

## Getting started

```shell
docker compose up -d   # PostgreSQL
./gradlew bootRun
```

```shell
./gradlew build        # includes integration tests via Testcontainers
```

## Trying it out

The migrations create three demo employees, so the endpoints can be used right
after `bootRun`. `22222222-2222-2222-2222-222222222221` is active,
`22222222-2222-2222-2222-222222222223` has left the company.

Record the hours worked in August 2026, as an ISO 8601 duration:

```shell
curl -X PUT localhost:8080/api/v1/employees/22222222-2222-2222-2222-222222222221/working-hours/2026-08 \
  -H 'Content-Type: application/json' \
  -d '{"workedTime": "PT152H30M"}'
```

```json
{"employeeId":"22222222-2222-2222-2222-222222222221","period":"2026-08","workedTime":"PT152H30M","source":"MANUAL"}
```

Repeating the call with a different value corrects the month instead of adding a
second entry — the value is absolute, so the endpoint is idempotent.

Read a single month, or all months of a year:

```shell
curl localhost:8080/api/v1/employees/22222222-2222-2222-2222-222222222221/working-hours/2026-08
curl 'localhost:8080/api/v1/employees/22222222-2222-2222-2222-222222222221/working-hours?year=2026'
```

Rejected requests are answered as `application/problem+json`, for example when
recording time for an employee who has left:

```shell
curl -X PUT localhost:8080/api/v1/employees/22222222-2222-2222-2222-222222222223/working-hours/2026-08 \
  -H 'Content-Type: application/json' \
  -d '{"workedTime": "PT152H30M"}'
```

```json
{"title": "Employee is not active", "status": 400, "detail": "employee 2222…2223 is not active"}
```

## Documentation

How the application was planned and built, the data model, and the architecture
decision records: [docs/](./docs/README.md).


