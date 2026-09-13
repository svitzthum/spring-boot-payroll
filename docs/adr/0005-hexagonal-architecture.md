# ADR 0005 — Hexagonal architecture (ports and adapters)

- Status: accepted
- Date: 2026-09-09

## Context

The assignment defines two independent write paths for the same data: a REST
endpoint used by the managing director and a scheduled import from an external
time tracking system. It also requires data integrity when both process the same
employee and period concurrently. A shared service in a conventional
controller–service–repository layering would serve both paths just as well. The
difference is on the driven side: there the business rules depend on JPA and on
the client of the external system, which makes the fictitious time tracking
system awkward to substitute and the concurrency behaviour harder to test in
isolation.

## Options considered

- **Layered architecture (controller, service, repository)** — least code, most
  familiar, and sufficient for sharing the use case between both paths; but the
  core depends on JPA and on the time tracking client instead of owning those
  interfaces.
- **Hexagonal architecture in a single Gradle module, boundaries expressed by
  packages** — one implementation of the use case driven by both adapters, the
  external time tracking system hidden behind an outbound port.
- **Hexagonal architecture with one Gradle module per ring** — strongest
  enforcement, but disproportionate build complexity for a demo of this size.

## Decision

Use hexagonal architecture within a single Gradle module. Inbound ports express
the use cases, outbound ports express persistence and the external time tracking
system. The domain package has no framework dependencies. The dependency rule is
enforced by an ArchUnit test rather than by convention alone.

## Consequences

- The rule "one authoritative value per employee and month" is implemented once
  and shared by the REST adapter and the scheduled importer.
- The fictitious time tracking system is a port implementation, so it can be
  faked in tests and swapped for a real HTTP client without touching the core.
- Domain model and JPA entity are separate types, which requires an explicit
  mapper — accepted so the persistence schema does not dictate the domain model.
- Concurrency tests can drive the inbound port directly, without going through
  HTTP.
- Slightly more classes than a layered design; justified here by two driving and
  two driven adapters, and explicitly not extended to a multi-module build.

