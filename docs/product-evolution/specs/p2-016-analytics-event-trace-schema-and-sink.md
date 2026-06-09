# P2-016 - Analytics Event Trace Schema And Sink Spec

Priority: P2
Sequence: 016
Source: `docs/optimization/archive/production-design-gaps.md`, `docs/optimization/archive/production-readiness-checklist.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/todo/cdp_gap_analysis.md`
Implementation plan: `../plans/p2-016-analytics-event-trace-schema-and-sink-plan.md`

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Add analytics event/trace storage and route trace/event writes through an OLAP-ready sink abstraction while keeping MySQL compatibility.

## Current Baseline

- `event_log` and `canvas_execution_trace` remain the compatibility tables for existing workflows.
- `V132__analytics_event_trace_schema_and_sink.sql` creates `analytics_event` and `analytics_event_trace` with tenant, session, platform, device, event-time, schema-version, retention, archive, and legal-hold fields.
- `TraceWriteBuffer` writes batches through `TraceEventSink`, keeps the existing MySQL trace write path, and can drop data when full.

## In Scope

- Canonical analytics tables `analytics_event` and `analytics_event_trace` from `V132__analytics_event_trace_schema_and_sink.sql`.
- Data objects and mappers for `AnalyticsEventDO` and `AnalyticsEventTraceDO`.
- `TraceEventSink` and `MySqlTraceEventSink`.
- Metrics for written, failed, dropped, and backlog counts.

## Out Of Scope

- Retention/archive jobs; split into P2-016B.
- Bounded analytics query APIs; split into P2-016C.
- Frontend analytics UI; split into P2-016D.

## Acceptance Criteria

- Migration is additive and backfill-safe through separate analytics tables.
- Existing event/trace reads keep working because `canvas_execution_trace` writes are preserved.
- Sink tests prove tenant-scoped analytics writes, MySQL compatibility, failure isolation, and metrics exposure.

## Verification Evidence

- 2026-06-09: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=TraceSinkTest,TraceWriteBufferTest` passed with 8 tests, 0 failures, 0 errors.
