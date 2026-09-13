# ADR 0003 — ISO 8601 durations in the API, integer minutes in the database

- Status: accepted
- Date: 2026-09-09

## Context

Payroll requires minute-based working times: totals must add up exactly across
employees and periods. Two questions follow from that — how the value is stored,
and how a client expresses it.

Storing a duration as a floating point number of hours introduces representation
errors (`7.5` has no exact `double` representation) and accumulates drift when
aggregated.

Accepting decimal hours in the API is worse than it looks. `7.51` hours is
`450.6` minutes and therefore not representable on a minute grid, which leaves
only two options: silently round, or reject. Silent rounding is unacceptable for
payroll, and rejecting is confusing because `7.51` looks like a perfectly normal
value to a caller — in German payroll practice, hundredths of an hour
("Industriestunden") are a common notation.

## Options considered

- **Decimal hours in the API, minutes in the database** — convenient for
  callers, but every request has to be checked against the minute grid, and
  ordinary looking values on a hundredth-of-an-hour grid have to be rejected.
- **Plain integer `minutesWorked` in the API** — unambiguous, but the unit lives
  only in the field name and callers have to convert themselves.
- **ISO 8601 durations in the API, minutes in the database** — `PT152H30M` is
  self-describing, standardised, and maps to `java.time.Duration` without any
  arithmetic.

## Decision

Persist durations as `integer` minutes in
`monthly_working_hours.minutes_worked`. Express them in the REST API as ISO 8601
duration strings (`"workedTime": "PT152H30M"`), mapped to `java.time.Duration`.

Conversion is exact in both directions:
`Duration.parse("PT152H30M").toMinutes()` yields `9150`, and
`Duration.ofMinutes(9150).toString()` yields `"PT152H30M"`.

Input is validated, never rounded. A request is rejected with `400 Bad Request`
if the duration

- carries a sub-minute component (`PT7H30M45S`),
- is negative (`PT-5H`),
- or exceeds the plausibility bound of the period.

The domain wraps the value in a `WorkDuration` value object holding minutes, so
the rest of the application never handles a raw number again.

## Consequences

- No rounding takes place anywhere: a value is either exactly representable in
  minutes or rejected with an explicit error.
- The unit is part of the wire format rather than a naming convention, so
  `152.5` versus `152:30` cannot be misread.
- `Duration.parse` rejects month and year designators itself, which removes the
  ambiguity of calendar-based durations for free.
- Jackson must be configured to serialise durations as ISO strings rather than
  numeric timestamps. The whole-minute and range rules are enforced by
  `WorkDuration` alone, whose rejection the exception handler answers with
  `400 Bad Request` — the API does not repeat them as validation annotations.
- Callers who think in decimal hours have to convert. Accepted: the conversion is
  trivial on their side, and it forces them to state what they mean instead of
  leaving the server to guess.
- If a future source delivers hundredths of an hour, minutes are the wrong grid
  (`0.01 h` = `36 s`). Seconds would represent both grids exactly and are the
  documented fallback; that change would be confined to the persistence adapter
  and the `WorkDuration` value object.

