# P2-015 - 4000 Concurrency Readiness And Lane Isolation Spec

Priority: P2
Sequence: 015
Source: `docs/optimization/archive/4000-concurrency-readiness-checklist.md`
Implementation plan: `../plans/p2-015-4000-concurrency-readiness-and-lane-isolation-plan.md`

Status: Current readiness implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Prepare the platform for a 4000 active execution target without allowing a configuration-only concurrency increase.

## User And Business Value

The team can discuss 4000 only after the runtime proves lane isolation, async write resilience, Redis role separation, adaptive retry, and downstream bulkheads.

## Evidence From Optimization

- 4000 checklist explicitly blocks production rollout until 3000 passes and additional architecture exists.
- Current lane resolver and Redis ZSET admission exist, but per-lane worker isolation, async writer separation, adaptive retry, cost profile, and downstream bulkhead readiness are not complete release gates.

## In Scope

- Add 4000 profile validation and readiness runbook.
- Add per-lane execution worker or queue isolation design and first implementation slice.
- Separate async writer pressure metrics for trace, audit, stats, and console-view state.
- Add Redis role separation plan and configuration guard.
- Add adaptive retry backoff based on backlog, downstream timeout, DLQ growth, and main-lane latency.
- Compute DAG cost profile at publish time and store it for lane selection.
- Add downstream bulkhead policy per external dependency.

## Out Of Scope

- Enabling 4000 in production before 3000 evidence is accepted.
- Full service split or WebFlux to MVC migration.
- Replacing RocketMQ or Redis.

## Functional Requirements

1. 4000 profile totals must validate to exactly 4000 before any run.
2. HEAVY and RETRY saturation must not degrade LIGHT or STANDARD p99 beyond the documented gate.
3. Async writer backlog must have visible stop gates.
4. Execution-state Redis latency must be isolated from route-cache and bitmap traffic in readiness environments.
5. Adaptive retry must reduce retry pressure when main-lane latency or downstream errors increase.
6. Publish-time DAG cost must influence lane selection and be visible to operators.

## Technical Scope

### Backend And Tooling Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneWorkerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/DagCostProfiler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicy.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- `backend/canvas-engine/src/main/resources/application.yml`
- `tools/perf/4000-readiness-profiles.json`
- `tools/perf/hardening-profile.mjs`

### Docs Touchpoints

- `docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/DagCostProfilerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/LaneWorkerIsolationTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicyTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistryTest.java`
- `tools/perf/4000-readiness-profile.test.mjs`

## Dependencies

- Requires P1-004 3000 hardening gate to pass.
- Requires observability from P0-005 and delivery/outbox pressure metrics from P0-003.

## Risks And Controls

- Premature promotion risk: keep config and runbook explicit that 4000 is blocked until readiness gates pass.
- Measurement blind spot risk: readiness fails when any required metric is missing.
- Overfitting risk: run mixed, heavy surge, retry surge, and downstream failure profiles.

## Acceptance Criteria

- 4000 profile validation command prints total `4000`.
- Readiness runbook lists entry requirements, stop gates, rollback, and evidence storage.
- Cost profiling influences at least one test DAG's lane selection.
- LIGHT and STANDARD execution requests use independent worker or queue capacity from HEAVY and RETRY in readiness mode.
- Adaptive retry backs off when downstream timeout or DLQ growth is injected.
- LIGHT/STANDARD protection is verified in a focused lane-isolation test.

## Implementation Notes

- `tools/perf/4000-readiness-profiles.json` validates exact 4000 lane totals and keeps `blockedUntil: p1-004-accepted`.
- `DagCostProfiler` computes cost signals and returns a recommended lane for scheduler, script, fanout, loop-risk, and large-recipient DAGs.
- `ExecutionLaneWorkerRegistry` adds readiness-mode lane worker capacity; `CanvasExecutionRequestDispatcher` uses it only when `canvas.execution-request.lane-isolation.enabled=true`.
- `AdaptiveRetryBackoffPolicy` preserves base exponential retry under healthy pressure and extends delay when lane p99, downstream errors, or DLQ growth exceed gates.
- `DownstreamBulkheadRegistry` tracks provider-specific CLOSED/OPEN/HALF_OPEN decisions and fails closed when registry state is unavailable.
- `RedisRoleConfiguration` rejects single logical Redis role wiring only when `canvas.redis.roles.readiness-enabled=true`.
- `TraceWriteBuffer` exposes async writer pressure metrics for written, failed, dropped, pending, and last flush duration. This file overlaps P2-016 sink work and must be manually merged with the P2-016 `TraceEventSink` boundary.

## Verification Evidence

- 2026-06-09: `node --test tools/perf/4000-readiness-profile.test.mjs` passed with 6 tests, 0 failures.
- 2026-06-09: `node --test tools/perf/hardening-profile.test.mjs` passed with 9 tests, 0 failures.
- 2026-06-09: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=DagCostProfilerTest,ExecutionLaneResolverTest,LaneWorkerIsolationTest,AdaptiveRetryBackoffPolicyTest,DownstreamBulkheadRegistryTest,CanvasExecutionRequestExecutorTest,RedisRoleConfigurationTest,TraceWriteBufferTest` passed with 38 tests, 0 failures, 0 errors.
