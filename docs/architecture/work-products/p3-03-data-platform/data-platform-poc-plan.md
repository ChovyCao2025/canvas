# Data Platform Proof-Of-Concept Plan

Date: 2026-06-05

Status: P3-03 proof-of-concept plan. Full data platform is deferred.

## Selected Slice

Selected vertical slice: audience compute history.

This slice answers one measurable operator question:

> For each audience, when did compute runs happen, which perf run or input triggered them, what status did they end in, how large was the result, and how long did the data take to become available?

The slice is intentionally thin. It does not select Flink, ClickHouse, lakehouse storage, BI semantic layers, or a separate data service.

## Slice Definition

| Area | Decision |
|---|---|
| Input sources | `audience_compute_run`, `audience_definition`, `audience_stat`; optional `event_log` and `canvas_execution` filtered by `perf_run_id`. |
| Transform | Build a tenant-aware audience compute history model with audience metadata, compute status, estimated size, bitmap size, perf run ID, input ID, created/updated timestamps, and error class. |
| Storage | First proof uses a MySQL read model or materialized table inside the monolith. A columnar store requires a later ADR after evidence. |
| Serving API | Internal read API for audience compute history; no frontend route change until payload is contract-tested. |
| SLA | freshness target T+5 minutes from `audience_compute_run.updated_at` to serving model availability. |
| Retention | Keep compute history according to capacity policy; raw `event_log` remains 30 days online and should be archived or summarized before deletion. |
| PII | Do not store raw user identifiers, phone, email, open ID, event attributes, trace input/output, or message payloads in the POC serving model. |
| Cost | Initial cost is one scheduled incremental query plus one small serving table. Stop before adding streaming infrastructure unless freshness or volume evidence requires it. |
| Rollback | Disable the scheduled build, drop or ignore the serving model, and keep OLTP source tables untouched. |
| Success metric | 99 percent of completed `audience_compute_run` rows visible in the serving model within 5 minutes, with zero raw PII fields and zero impact on online audience membership checks. |

## Transform Steps

1. Read `audience_compute_run` rows changed since the last watermark.
2. Join `audience_definition` for stable audience name, source type, and evaluation strategy.
3. Join `audience_stat` for latest aggregate status where needed.
4. Optionally group `event_log` and `canvas_execution` by `perf_run_id` for aggregate input and execution counts.
5. Write or update the serving model using idempotent key `(audience_id, compute_run_id)`.
6. Record build watermark, row count, dropped records, error count, and duration in evidence.

## Contract Tests

Required contract test coverage before implementation:

- source schema: `PerfRunTrackingSchemaTest` and `AudienceComputeRunTrackingSchemaTest`;
- event envelope: example `AudienceComputeRunCompleted` event validates tenant, event ID, schema version, occurred-at, idempotency key, and trace context;
- replay idempotency: rebuilding the same watermark produces the same serving row count and no duplicate rows;
- deletion propagation: user-scoped source deletion does not leave raw user identifiers in the serving model;
- serving query shape: query by audience ID, status, perf run ID, and time range returns stable DTO fields.

## Operating Evidence

Capture the following for every POC run:

- freshness p50, p95, p99;
- dropped records count and reason;
- backfill duration and processed row count;
- query latency p50, p95, p99;
- storage growth by day;
- cost estimate for scheduler runtime, storage, and any future component;
- source watermark and target watermark;
- deletion-propagation check result.

## Stop Criteria

Reject or pause the selected platform component if any condition is true:

- online execution, audience membership checks, or audience computation slows beyond agreed P0/P1 thresholds;
- PII appears in the serving model;
- replay is not idempotent;
- freshness target is missed for two consecutive evidence runs without a capacity explanation;
- deletion propagation cannot be proven;
- operational cost is higher than the value of the single slice;
- the implementation requires broad service extraction or a new data platform before the slice proves value.

## Why Full Data Platform Is Deferred

The full data platform is deferred until this slice proves source ownership, ingestion safety, replay, deletion propagation, freshness, query value, storage growth, and operating cost. Choosing Flink, ClickHouse, lakehouse layers, or BI semantic governance before that evidence would create platform coupling without a measured business slice.
