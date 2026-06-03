# P2-016 - Analytics Event Trace Schema And Sink Spec

Priority: P2
Sequence: 016
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/production-readiness-checklist.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/todo/cdp_gap_analysis.md`
Implementation plan: `../plans/p2-016-analytics-event-trace-schema-and-sink-plan.md`

## Goal

Add additive event/trace fields and route trace/event writes through an OLAP-ready sink abstraction while keeping MySQL compatibility.

## Current Baseline

- `event_log` and `canvas_execution_trace` exist.
- `TraceWriteBuffer` writes batches to MySQL and can drop data when full.
- Event and trace rows lack enough tenant/session/platform/device/version fields for later analytics.

## In Scope

- Additive migration `V112__analytics_event_trace_schema_and_sink.sql`.
- Data object updates for event and trace fields.
- `TraceEventSink` and `MySqlTraceEventSink`.
- Metrics for written, failed, dropped, and backlog counts.

## Out Of Scope

- Retention/archive jobs; split into P2-016B.
- Bounded analytics query APIs; split into P2-016C.
- Frontend analytics UI; split into P2-016D.

## Acceptance Criteria

- Migration is additive and backfill-safe.
- Existing event/trace reads keep working when new fields are null.
- Sink tests prove tenant-scoped writes, MySQL compatibility, and metrics exposure.
