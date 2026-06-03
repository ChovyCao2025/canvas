# 4000 Concurrency Readiness And Lane Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a guarded 4000-concurrency readiness path without allowing a configuration-only production increase.

**Architecture:** Extend the existing lane resolver and hardening tooling with a 4000 readiness profile, publish-time DAG cost profiling, lane worker isolation, adaptive retry pressure control, async writer pressure metrics, and downstream bulkhead decisions. Keep 4000 blocked by explicit entry gates until P1-004 evidence and this readiness runbook pass.

**Tech Stack:** Java 21, Spring Boot, Redis, RocketMQ, MyBatis, Micrometer, Node.js perf tooling, JUnit 5.

---

## Spec Reference

- `docs/product-evolution/specs/p2-015-4000-concurrency-readiness-and-lane-isolation.md`
- Optimization sources: `docs/optimization/4000-concurrency-readiness-checklist.md`, `docs/optimization/3000-concurrency-hardening-checklist.md`, `docs/optimization/production-design-gaps.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/DagCostProfiler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneWorkerRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicy.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Tooling And Docs**
- Modify: `tools/perf/4000-readiness-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Create: `docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/DagCostProfilerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/LaneWorkerIsolationTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistryTest.java`
- Create: `tools/perf/4000-readiness-profile.test.mjs`

### Task 1: 4000 Profile Validation

**Files:**
- Modify: `tools/perf/4000-readiness-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Create: `tools/perf/4000-readiness-profile.test.mjs`

- [ ] **Step 1: Write profile validation tests**

Create `4000-readiness-profile.test.mjs` tests named `requiresExact4000TotalConcurrency`, `requiresLightStandardHeavyRetryLaneBudgets`, `requiresNonZeroRetryBudget`, `requiresMixedHeavyAndRetrySurgeScenarios`, and `requiresStopGatesForRedisMysqlRocketMqDownstreamAndRetryBacklog`.

- [ ] **Step 2: Run the profile test and confirm red state**

Run:

```bash
node --test tools/perf/4000-readiness-profile.test.mjs
```

Expected: FAIL because the 4000 readiness profile and validator hooks do not exist.

- [ ] **Step 3: Add the 4000 readiness profile**

Add a profile with `totalConcurrency: 4000`, lane budgets, mixed-heavy and retry-surge scenarios, required stop gates, and an explicit `blockedUntil: "p1-004-accepted"` field.

- [ ] **Step 4: Extend the hardening profile validator**

Teach `tools/perf/hardening-profile.mjs` to load the 4000 readiness file, verify exact totals, reject missing stop gates, and print a machine-readable summary containing `profile`, `totalConcurrency`, `laneBudgets`, `stopGates`, and `blockedUntil`.

- [ ] **Step 5: Run profile validation**

Run:

```bash
node --test tools/perf/4000-readiness-profile.test.mjs
node tools/perf/hardening-profile.mjs --profile 4000-readiness --validate-only
```

Expected: PASS, and the command output contains `"totalConcurrency":4000`.

### Task 2: DAG Cost Profiling, Lane Selection, And Worker Isolation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/DagCostProfiler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneWorkerRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/DagCostProfilerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/LaneWorkerIsolationTest.java`

- [ ] **Step 1: Write cost profile tests**

Create `DagCostProfilerTest` methods named `classifiesLightDirectCallDagAsLight`, `classifiesScheduledAudienceDagAsHeavy`, `classifiesSideEffectHeavyDagAsStandardOrHeavy`, `classifiesGroovyScriptDagAsHeavy`, `classifiesLargeFanoutDagAsHeavy`, `addsLoopRiskForGotoDag`, and `usesStandardLaneForMissingGraph`.

- [ ] **Step 2: Write lane worker isolation tests**

Create `LaneWorkerIsolationTest` methods named `createsReadinessWorkersForLightStandardHeavyAndRetry`, `protectsLightAndStandardCapacity`, `heavySaturationDoesNotConsumeLightWorkers`, `retrySaturationDoesNotConsumeStandardWorkers`, and `disabledIsolationUsesExistingDispatcherPath`.

- [ ] **Step 3: Run cost and isolation tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagCostProfilerTest,LaneWorkerIsolationTest
```

Expected: FAIL because `DagCostProfiler` and lane worker isolation do not exist.

- [ ] **Step 4: Implement `DagCostProfiler`**

Return a value object with `nodeCount`, `sideEffectNodeCount`, `waitNodeCount`, `fanoutScore`, `scriptNodeCount`, `loopRiskScore`, `estimatedRecipientCount`, and `recommendedLane`. Classify high-recipient, script, offline tagger, subflow, and high-fanout DAGs as HEAVY unless trigger type forces RETRY.

- [ ] **Step 5: Implement lane worker isolation**

Add `ExecutionLaneWorkerRegistry` with per-lane readiness-mode worker or queue capacity. Route `CanvasExecutionRequestDispatcher` submissions through the lane registry when readiness isolation is enabled. Keep the existing dispatcher path when readiness isolation is disabled.

- [ ] **Step 6: Integrate cost profile into lane resolution**

Add an overload to `ExecutionLaneResolver` that accepts the DAG cost profile and uses it after existing RETRY and LIGHT trigger checks. Keep current method behavior unchanged for callers that do not have a profile.

- [ ] **Step 7: Run lane tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagCostProfilerTest,ExecutionLaneResolverTest,LaneWorkerIsolationTest
```

Expected: PASS.

### Task 3: Adaptive Retry And Downstream Bulkheads

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicy.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistryTest.java`

- [ ] **Step 1: Write retry and bulkhead tests**

Create `AdaptiveRetryBackoffPolicyTest` methods named `usesDefaultExponentialRetry`, `increasesDelayWhenMainLaneP99IsAboveGate`, `increasesDelayWhenDlqGrowthIsAboveGate`, and `stopsAfterMaxAttempts`. Create `DownstreamBulkheadRegistryTest` methods named `opensProviderSpecificBulkhead`, `allowsHalfOpenRecovery`, and `failsClosedWhenRegistryStateIsUnavailable`.

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AdaptiveRetryBackoffPolicyTest,DownstreamBulkheadRegistryTest
```

Expected: FAIL because adaptive retry and downstream bulkhead services do not exist.

- [ ] **Step 3: Implement adaptive retry policy**

Accept attempt count, base delay, lane pressure snapshot, downstream error snapshot, and DLQ growth snapshot. Return `delayMs`, `reason`, `pressureMultiplier`, and `maxAttemptsExceeded`.

- [ ] **Step 4: Implement downstream bulkhead registry**

Track per-tenant and per-provider state `CLOSED`, `OPEN`, and `HALF_OPEN`. Return a decision with provider key, dependency kind, permit result, retry-after time, and operator-readable reason.

- [ ] **Step 5: Integrate retry policy**

Replace `CanvasExecutionRequestExecutor.calculateRetryDelayMs` internals with `AdaptiveRetryBackoffPolicy` while preserving the existing public execution flow and max-attempt behavior.

- [ ] **Step 6: Run policy tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AdaptiveRetryBackoffPolicyTest,DownstreamBulkheadRegistryTest,CanvasExecutionRequestExecutorTest
```

Expected: PASS.

### Task 4: Async Writer And Redis Role Readiness Metrics

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md`

- [ ] **Step 1: Add async writer counters**

Expose pending, written, dropped, failed, and flush-duration metrics for trace writes. Mirror the metric naming pattern used by existing Micrometer counters.

- [ ] **Step 2: Add Redis role configuration guards**

Add config keys for execution state Redis, route cache Redis, bitmap Redis, and rate-limit Redis. Startup must fail in readiness mode when all roles point to the same logical connection.

- [ ] **Step 3: Add readiness runbook**

Document entry requirements, 4000 command, expected metrics, stop gates, rollback, degraded-mode actions, and evidence storage path. Include that 4000 remains blocked until P1-004 3000 evidence is accepted.

- [ ] **Step 4: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceWriteBufferTest,RedisRoleConfigurationTest
node tools/perf/hardening-profile.mjs --profile 4000-readiness --validate-only
```

Expected: PASS.

### Task 5: Final Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-015-4000-concurrency-readiness-and-lane-isolation.md`
- Modify: `docs/product-evolution/plans/p2-015-4000-concurrency-readiness-and-lane-isolation-plan.md`

- [ ] **Step 1: Run all readiness tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagCostProfilerTest,ExecutionLaneResolverTest,LaneWorkerIsolationTest,AdaptiveRetryBackoffPolicyTest,DownstreamBulkheadRegistryTest,TraceWriteBufferTest,RedisRoleConfigurationTest
node --test tools/perf/4000-readiness-profile.test.mjs
```

Expected: PASS.

- [ ] **Step 2: Commit implementation slice**

Run:

```bash
git add backend/canvas-engine/src tools/perf docs/product-evolution/specs docs/product-evolution/plans docs/product-evolution/runbooks
git commit -m "feat: add 4000 concurrency readiness gates"
```

Expected: commit contains only 4000 readiness, lane cost, retry, bulkhead, metrics, runbook, spec, and plan files.
