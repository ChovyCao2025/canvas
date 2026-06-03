# P1-004C - Execution Lane Metrics And Registry Guards Spec

Priority: P1
Sequence: 004C
Source: `docs/optimization/3000-concurrency-hardening-checklist.md`
Implementation plan: `../plans/p1-004c-execution-lane-metrics-and-registry-guards-plan.md`

## Goal

Instrument runtime admission and lane behavior so the 3000 hardening gate can observe registry availability, lane active counts, retry backlog, and protected lane routing.

## Current Baseline

- `InFlightExecutionRegistry` performs Redis ZSET admission and already rejects conservatively on Redis failure.
- `ExecutionLaneResolver` maps work into LIGHT, STANDARD, HEAVY, and RETRY lanes.
- `CanvasExecutionRequestBacklogMetrics` exposes request backlog counts.
- Test coverage is not exhaustive for protected lane routing and registry metrics.

## In Scope

- `CanvasMetrics` counters/timers/gauges for:
  - registry admission result by lane/reason;
  - registry latency;
  - active executions by lane;
  - trace buffer backlog.
- `InFlightExecutionRegistry` metric recording on success, rejection, and Redis failure.
- Retry backlog metric test coverage.
- Exhaustive lane resolver tests for scheduled, replay, Groovy, TAGGER audience, subflow, direct, continuation, overflow retry, and persistent request retry.
- Explicit confirmation of 3000 lane config values.

## Out Of Scope

- Node perf profile schema; split into P1-004.
- Stop-gate evaluator; split into P1-004B.
- Human runbook; split into P1-004D.
- Changing production lane budgets beyond the existing 3000 target.

## Functional Requirements

1. Redis registry failures continue rejecting new admissions conservatively and record `REGISTRY_UNAVAILABLE`.
2. Successful admission records lane and reason `NONE`.
3. Rejected admission records lane and rejection reason.
4. Active lane gauges update after admission decisions.
5. Lane resolver tests prove RETRY rules take precedence over other routing rules.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- `backend/canvas-engine/src/main/resources/application.yml`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`

## Acceptance Criteria

- Focused backend tests pass for registry metrics, backlog metrics, and lane resolver matrix.
- `application.yml` keeps effective 3000 lane budgets as `LIGHT=600`, `STANDARD=1800`, `HEAVY=300`, `RETRY=300`.
