# 3000 Concurrency Hardening Checklist

## Purpose

This checklist is the production-readiness gate for the 3000 Canvas execution concurrency target. 3000 means cluster-level active Canvas executions, not HTTP connections, MQ backlog, DAU, or single-instance concurrency.

## Backend Test Baseline

Run before any 3000 hardening code change:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition:

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Stop condition:

- Test compilation fails.
- Any listed test class fails.
- A failure is marked unrelated without a file, test name, and reproducible command.

## 3000 Completion Gate

3000 is complete only when:

- affected backend tests pass on Java 21
- the default mixed profile passes the full observation window
- retry surge passes after downstream recovery
- heavy surge does not degrade LIGHT or STANDARD
- slow downstream is contained by timeout, circuit breaker, and bulkhead behavior
- Redis registry latency or outage fails conservatively
- RocketMQ backlog recovery does not let RETRY starve normal traffic
- retry backlog, DLQ growth, Disruptor overflow, and MQ backlog have stop gates
- rollback and degrade actions have been exercised
- 4000 remains blocked until this checklist passes

## Default Mixed Traffic Profile

The default 3000 profile is:

| Lane | Concurrency | Share | Purpose |
| --- | ---: | ---: | --- |
| LIGHT | 600 | 20% | direct calls, short DAGs, low downstream fanout |
| STANDARD | 1800 | 60% | normal event, MQ, behavior, and API-triggered flows |
| HEAVY | 300 | 10% | audience batch, high fanout DAGs, expensive script or large payload flows |
| RETRY | 300 | 10% | overflow and request retry recovery |
| Global | 3000 | 100% | total cluster active execution budget |

Pass conditions:

- `LIGHT` and `STANDARD` runner p95 stays at or below 1000 ms in `default-mixed-3000`.
- `HEAVY` active execution count never borrows protected `LIGHT` or `STANDARD` budget.
- `RETRY` active execution count never borrows protected `LIGHT` or `STANDARD` budget.
- No unbounded retry backlog, DLQ growth, Disruptor overflow, or MQ backlog growth appears during the observation window.

Stop conditions:

- lane total exceeds global budget
- Redis p95 stays above 20 ms or p99 stays above 50 ms through one observation window
- MySQL active connections stay at or above 85% of pool max, or slow SQL above 1000 ms appears in two consecutive samples
- normal MQ backlog grows while RETRY is draining
- Disruptor overflow grows for two consecutive samples
- DLQ grows after downstream recovery

## Lane Budget Tuning Rules

Default budget:

- `LIGHT`: 600
- `STANDARD`: 1800
- `HEAVY`: 300
- `RETRY`: 300
- `global`: 3000

Guardrails:

- `LIGHT` and `STANDARD` are protected lanes.
- `HEAVY` cannot borrow from `LIGHT` or `STANDARD`.
- `RETRY` cannot borrow from `LIGHT` or `STANDARD`.
- Increase `RETRY` only when downstream health is good and retry backlog is shrinking.
- If `LIGHT` or `STANDARD` latency degrades, reduce `HEAVY` and `RETRY` first.
- If Redis or MySQL latency degrades, do not increase any lane budget.
- If a downstream timeout rises, reduce the lane that calls that dependency instead of raising global concurrency.

Incident tuning order:

1. Reduce `RETRY` budget or lengthen retry backoff.
2. Reduce `HEAVY` budget or pause heavy jobs.
3. Disable low-priority scheduled and replay traffic.
4. Preserve `LIGHT` and `STANDARD` if their dependencies remain healthy.
5. Scale application instances only after Redis, MySQL, RocketMQ, and downstream capacity are confirmed.
