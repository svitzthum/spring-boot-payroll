# ADR 0005 — Hexagonal architecture (ports and adapters)

- Status: accepted
- Date: 2026-09-09

## Context

The assignment defines two independent write paths for the same data: a REST
endpoint used by the managing director and a scheduled import from an external
time tracking system. It also requires data integrity when both process the same
employee and period concurrently. A shared service in a conventional
controller–service–repository layering would serve both paths just as well. What
differs is what the business rules depend on: in that design they depend on JPA
and on the client of the external system, which lets the persistence mapping
shape the model. The external system is fictitious on top of that — it has to be
simulated for the demo to run at all.

The pattern was proposed first, and working with it deliberately was part of the
motivation. An application of this size would run without it, so the forces
above had to carry the decision afterwards.

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

- **Every write enters through an inbound port** — no adapter can reach the
  store on its own and skip the rules the use case applies; the ArchUnit test
  fails the build if one tries.
- **The business rules can be exercised without Spring or a database** — no
  framework types in `domain` and `application`, ports narrow enough to
  implement in memory.
- **The domain model is not shaped by the ORM** — the aggregate stays final and
  factory-built, the locking version an infrastructure concern.
- Domain model and JPA entity are therefore separate types, which requires an
  explicit mapper — accepted so the persistence schema does not dictate the
  domain model.
- More classes than a layered design, justified by two driving and two driven
  adapters and explicitly not extended to a multi-module build. At this size the
  pattern earns less than it would in a larger system.
