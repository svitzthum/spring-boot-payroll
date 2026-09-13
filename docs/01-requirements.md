# Requirements

## Assignment summary

Source: [Aufgabe_Java_Softwareentwickler_in.pdf](./Aufgabe_Java_Softwareentwickler_in.pdf)

Build a Java application based on Spring Boot 4 for the following scenario:

- Users of the payroll software (typically managing directors) record monthly
  working hours for their own employees through a REST endpoint.
- In addition, an external time tracking system exists in which the employees
  record their own times. Those times are transferred to the payroll software
  at regular intervals.

Required deliverables:

1. A Spring Boot application with a REST endpoint for recording monthly hours.
2. A background process that regularly transfers data from a fictitious external
   time tracking system into the payroll software.
3. Data integrity must be guaranteed when data for the same period and the same
   employee is processed concurrently.
4. No UI is required.

## Interpretation

### The recorded value is actual hours worked

Both channels describe the same fact: the manual entry and the time tracking
import compete for one value, otherwise the required protection against
concurrent processing "for the same period and employee" would be pointless.
That value is the hours actually worked — for hourly paid employees it is what
the wage is calculated from, which is why it has to be correct per month and per
employee rather than merely plausible.

Manual entry is therefore the fallback and correction path for employees without
time tracking, or for correcting incomplete tracked data.

### Derived functional rules

- One authoritative value per employee and calendar month.
- Writing the monthly value is idempotent: the request carries the absolute
  value for the month, not a delta.
- Every write records its source (`MANUAL`, `TIME_TRACKING`).
- Concurrent writes for the same employee and month must never produce a lost
  update or a duplicate row.

## Out of scope

The following is deliberately excluded to keep the demo focused. It is mentioned
here so the boundary is a conscious decision, not an omission:

- Authentication, authorisation and tenant isolation (the data model prepares
  for it via `employer`, but no security layer is implemented).
- Any UI.
- Payroll calculation (gross to net), tax or social security logic.
- Contracted target hours, overtime and absence handling.

