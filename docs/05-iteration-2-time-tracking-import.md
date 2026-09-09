# Iteration 2 — Scheduled Import from the External Time Tracking System

> TODO — to be planned after iteration 1 is finished.

Topics to cover: a driving adapter for the scheduled background process, a
driven adapter for the fictitious external time tracking source behind its own
outbound port, idempotent import, source precedence between manual entry and
import, change history, and the concurrency guarantees when both paths write the
same employee and period. The inbound port from iteration 1 is reused rather
than duplicated.

