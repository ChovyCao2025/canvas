# P1-004B - 3000 Hardening Stop Gate Evaluator Spec

Priority: P1
Sequence: 004B
Source: `docs/optimization/archive/3000-concurrency-hardening-checklist.md`
Implementation plan: `../plans/p1-004b-3000-hardening-stop-gate-evaluator-plan.md`

## Implementation Status

Implemented and focused-verified on 2026-06-05. `capacity-report.mjs` now exports `evaluateHardeningGates(samples)` with deterministic gate ordering for Redis, MySQL, MQ, Disruptor overflow, retry backlog, DLQ, and protected lane latency samples.

## Goal

Add a deterministic evaluator that converts 3000 hardening metric samples into `PASS` or `STOP` decisions with named stop gates.

## Current Baseline

- `tools/perf/capacity-report.mjs` estimates capacity bottlenecks.
- `evaluateHardeningGates(samples)` evaluates the 3000 hardening checklist gates for Redis, MySQL, MQ, overflow, retry backlog, DLQ, and protected lane latency.

## In Scope

- `evaluateHardeningGates(samples)` exported from `capacity-report.mjs`.
- Stop gates:
  - `REDIS_REGISTRY_LATENCY_SUSTAINED`
  - `MYSQL_POOL_SATURATION`
  - `MYSQL_SLOW_SQL`
  - `NORMAL_MQ_BACKLOG_STARVED_BY_RETRY`
  - `DISRUPTOR_OVERFLOW_GROWING`
  - `RETRY_BACKLOG_GROWING_AFTER_RECOVERY`
  - `DLQ_GROWING_AFTER_RECOVERY`
  - `PROTECTED_LANE_LATENCY_VIOLATION`
- Tests for healthy sample pass and multi-gate stop output.

## Out Of Scope

- Collecting metrics from live services.
- Writing evidence manifests; split into P1-004.
- Backend metric instrumentation; split into P1-004C.

## Functional Requirements

1. Healthy samples return `{ verdict: "PASS", stopGates: [] }`.
2. Redis p95 above 20 ms or p99 above 50 ms returns `REDIS_REGISTRY_LATENCY_SUSTAINED`.
3. MySQL active connections at or above 85% of pool max returns `MYSQL_POOL_SATURATION`.
4. MySQL slow SQL above 1000 ms returns `MYSQL_SLOW_SQL`.
5. Growing normal MQ backlog, Disruptor overflow, retry backlog, DLQ, and protected-lane p95 each produce named gates.

## Technical Scope

- `tools/perf/capacity-report.mjs`
- `tools/perf/capacity-report.test.mjs`

## Acceptance Criteria

- `node --test tools/perf/capacity-report.test.mjs` passes.
- Tests assert exact gate names and output order for Redis/MySQL violations.
