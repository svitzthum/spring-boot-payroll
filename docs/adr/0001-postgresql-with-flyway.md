# ADR 0001 — PostgreSQL with Flyway migrations

- Status: accepted
- Date: 2026-09-09

## Context

The application needs a relational database. The central requirement of the
assignment — data integrity for concurrent writes on the same employee and
period — is best enforced by database constraints. That only works if the schema
is explicit, reviewable and versioned.

## Decision

Use PostgreSQL and manage the schema with Flyway migrations. Hibernate runs with
`ddl-auto: validate` and never generates or modifies the schema. The same
database engine is used in tests via Testcontainers.

## Consequences

- Constraints (unique indexes, check constraints) are visible in SQL and part of
  the reviewed source code.
- Tests run against the real engine, so PostgreSQL specific behaviour such as
  constraint violation handling is covered instead of being masked by an
  in-memory database.
- Schema changes require writing a migration, which is slightly more effort but
  keeps the schema history explicit.

