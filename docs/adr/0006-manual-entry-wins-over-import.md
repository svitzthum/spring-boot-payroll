# ADR 0006 — Manual entry wins over the time tracking import

- Status: accepted
- Date: 2026-09-11

## Context

Both write paths set the same value: the managing director through the REST
endpoint, the scheduled job from the external time tracking system. When both
have something to say about the same employee and month, one has to win.

## Options considered

- **Last writer wins** — no rule to implement, but the next import silently
  undoes a manual correction.
- **Import wins** — the external system is treated as the single truth, which
  makes manual entry pointless for tracked employees.
- **Manual entry wins** — the correction survives; the import is recorded but
  does not overwrite.

## Decision

A value recorded manually is not overwritten by the import. The import records
the event as skipped in the import journal, together with the value the external
system reported.

The change history stays untouched in that case: it tracks changes of the
monthly value, and nothing changed.

This follows from what the recorded value means (see
[requirements](../01-requirements.md)): manual entry is the correction path for
missing or incomplete tracked data, so a routine job must not undo it.

## Consequences

- The precedence rule lives in the application layer and is covered by tests.
- Nothing is lost: what the external system reported stays visible in the import
  journal, so the skip can be explained.
- Correcting a month back to the tracked value is a manual act. An explicit
  "release for import again" would be a later feature, not a silent overwrite.

