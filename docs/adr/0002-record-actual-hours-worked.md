# ADR 0002 — The recorded value is actual hours worked

- Status: accepted
- Date: 2026-09-09

## Context

The assignment states that users record "working hours" for their employees on a
monthly basis, and that an external time tracking system delivers times for the
same employees. It does not say whether the manually entered value represents
contracted target hours or hours actually worked.

## Decision

The recorded value is the number of hours actually worked in a calendar month.
Manual entry and the time tracking import are two sources for the same fact.
Manual entry is the fallback and correction path.

## Consequences

- Both write paths target the same column, which is exactly why the required
  protection against concurrent processing for the same employee and period is
  needed.
- Contracted hours are out of scope; if needed later they belong to the
  employment contract as master data, not to a monthly entry.
- A source attribute is required on the monthly value to keep the origin of the
  last write traceable, and a precedence rule between the sources must be
  defined in iteration 2.

