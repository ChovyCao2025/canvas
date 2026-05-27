# 3000 Concurrency Hardening Design Spec

## 1. Purpose

The current 3000 concurrency design defines the target architecture: global budget, lane budget, Redis ZSET admission, retry isolation, heavy-task isolation, and rollout gates. This hardening spec closes the gap between "the architecture supports 3000" and "the platform is ready to carry 3000 in production."

The 3000 target remains the formal production target. 4000 stays a later readiness phase and must not distract from making 3000 measurable, testable, and operable.

---

## 2. Completion Definition

3000 is considered complete only when all of these are true:

1. The affected backend test baseline compiles and passes.
2. `LIGHT`, `STANDARD`, `HEAVY`, and `RETRY` lanes have a documented mixed traffic profile.
3. Lane budgets have default values, tuning rules, and guardrails.
4. Redis registry failure and latency spikes have conservative behavior.
5. Retry backlog, DLQ growth, Disruptor overflow, and MQ backlog have stop gates.
6. Downstream partial failures cannot consume the main execution lanes indefinitely.
7. The 3000 rollout checklist has explicit pass, stop, rollback, and degrade actions.
8. 4000 readiness is blocked until the full 3000 gate passes.

---

## 3. Current Gaps

### 3.1 Test Baseline Gap

Current backend tests can fail at test compilation before feature work begins because some test fixtures lag behind production constructors. Examples observed in the current branch include:

- `CanvasExecutionServiceTest`
- `CanvasExecutionServiceTriggerNodeTest`
- `InFlightExecutionRegistryTest`
- `CanvasServicePublishTest`
- `CanvasServiceExampleFilterTest`
- `CanvasOpsServiceExampleCloneTest`

This must be fixed before claiming any 3000 implementation is verified. A 3000 capacity change that cannot run impacted tests has no reliable safety net.

### 3.2 Traffic Model Gap

The design has lane budgets but does not yet define the mixed workload that proves those budgets. A pure 3000 standard-flow test is not enough because production traffic includes retry, heavy, slow downstream, and burst behavior.

### 3.3 Tuning Rule Gap

The initial lane budget is clear, but the rules for changing it under pressure need to be explicit. Without tuning rules, operators may increase the wrong lane and amplify an incident.

### 3.4 Failure Mode Gap

The system needs documented behavior for infrastructure and dependency degradation:

- Redis registry unavailable or slow.
- MySQL connection pool close to saturation.
- RocketMQ backlog growing.
- Downstream p95 / p99 increasing.
- Retry backlog growing faster than recovery.
- Heavy jobs starving main traffic.

---

## 4. 3000 Mixed Traffic Model

### 4.1 Default Mixed Profile

The default 3000 profile should be:

- `LIGHT`: 20% = 600 concurrent executions.
- `STANDARD`: 60% = 1800 concurrent executions.
- `HEAVY`: 10% = 300 concurrent executions.
- `RETRY`: 10% = 300 concurrent executions.

This matches the initial lane budget and should be the first full 3000 acceptance scenario.

### 4.2 Required Stress Profiles

The default mixed profile is not sufficient by itself. The rollout must also run these focused profiles:

1. `RETRY` surge
   - Retry lane rises to its full budget.
   - `LIGHT` and `STANDARD` latency must stay within target.
   - Retry backlog must stop growing after the downstream condition recovers.

2. `HEAVY` surge
   - Heavy lane reaches its full budget.
   - Heavy work must not consume main-lane budget.
   - Heavy queue growth must be bounded or explicitly degraded.

3. Slow downstream
   - One dependency becomes slow or times out.
   - Its circuit breaker / timeout / bulkhead must protect unrelated dependencies.
   - Execution slots must not remain occupied until global timeout in large numbers.

4. Redis registry latency spike
   - Registry latency increases or becomes temporarily unavailable.
   - New execution admission must fail conservatively rather than over-admit.
   - Existing executions must complete and release local resources.

5. RocketMQ backlog recovery
   - Normal, heavy, and retry topic backlog are observed separately.
   - Retry backlog must not starve normal backlog recovery.

---

## 5. Lane Budget Tuning Rules

### 5.1 Initial Budget

The initial 3000 budget remains:

- `LIGHT`: 600
- `STANDARD`: 1800
- `HEAVY`: 300
- `RETRY`: 300
- `global`: 3000

The sum of lane budgets must never exceed the global budget.

### 5.2 Guardrails

Budget tuning must follow these guardrails:

- `LIGHT` and `STANDARD` are protected lanes.
- `HEAVY` cannot borrow from `LIGHT` or `STANDARD`.
- `RETRY` cannot borrow from `LIGHT` or `STANDARD`.
- Increasing `RETRY` is allowed only when downstream health is good and retry backlog is shrinking.
- When `LIGHT` or `STANDARD` latency degrades, reduce `HEAVY` and `RETRY` first.
- When Redis or MySQL latency degrades, do not increase any lane budget.
- When downstream timeout increases, reduce the lane that calls that dependency instead of raising global concurrency.

### 5.3 Tuning Order

Recommended tuning order during incidents:

1. Reduce `RETRY` budget or lengthen retry backoff.
2. Reduce `HEAVY` budget or pause heavy jobs.
3. Disable low-priority scheduled and replay traffic.
4. Preserve `LIGHT` and `STANDARD` if their downstream dependencies remain healthy.
5. Scale application instances only after Redis, MySQL, RocketMQ, and downstream capacity are confirmed.

---

## 6. Failure Mode Strategy

### 6.1 Redis Registry Unavailable

Behavior:

- Reject new execution admission conservatively.
- Return a typed rejection reason such as `REGISTRY_UNAVAILABLE`.
- Keep existing executions running.
- Rely on ZSET TTL self-healing for stale slots.

Reason:

Over-admitting during registry failure can exceed the 3000 global budget and cause wider failure.

### 6.2 Redis Latency Spike

Behavior:

- Keep `LIGHT` and `STANDARD` as protected lanes.
- Reduce or pause `HEAVY` and `RETRY`.
- Record registry latency and rejection metrics.
- Stop promotion to 3000 if Redis p95 / p99 remains above threshold through the observation window.

### 6.3 MySQL Saturation

Behavior:

- Do not increase execution concurrency.
- Degrade weak-online writes when supported.
- Batch or buffer trace, audit, stats, and console-view updates when supported.
- Stop promotion if active connections or slow SQL remain above threshold.

### 6.4 RocketMQ Backlog Growth

Behavior:

- Inspect normal, retry, and heavy backlog independently.
- Do not allow retry backlog recovery to starve normal traffic.
- Pause low-priority scheduled, replay, or heavy jobs before increasing consumer pressure.
- Stop promotion if backlog grows for the full observation window.

### 6.5 Downstream Partial Failure

Behavior:

- Apply dependency-level timeout, circuit breaker, and bulkhead.
- Reduce the lane that calls the degraded dependency.
- Keep unrelated lanes running if their dependencies are healthy.
- Do not raise global concurrency to compensate for slow downstream.

### 6.6 Retry Backlog Explosion

Behavior:

- Lengthen retry backoff.
- Lower `RETRY` lane budget.
- Move entries to DLQ after max attempts.
- Stop promotion if retry backlog growth outpaces recovery after the downstream dependency recovers.

---

## 7. 3000 Rollout Gate

### 7.1 Pre-Implementation Gate

Before implementing new 3000 code changes:

- Current affected tests compile.
- Current affected tests pass or known unrelated failures are documented.
- JDK 21 is used for backend verification.
- Work happens in an isolated worktree when the main workspace is dirty.

### 7.2 Pre-Load-Test Gate

Before 3000 load testing:

- Focused lane tests pass.
- Impacted execution tests pass.
- Metrics for lane active, global active, admission reject, retry backlog, Disruptor overflow, and MQ backlog are visible.
- Rollback commands and configuration toggles are documented.

### 7.3 Production Gate

3000 production readiness requires:

- Default mixed profile passes the full observation window.
- Retry surge profile passes.
- Heavy surge profile passes.
- Slow downstream profile passes.
- Redis registry latency profile passes.
- RocketMQ backlog recovery profile passes.
- No unbounded backlog growth.
- No sustained Redis, MySQL, RocketMQ, or downstream saturation.
- Rollback and degrade playbook has been exercised.

---

## 8. Relationship To 4000

4000 remains a later readiness target. The platform must not enter 4000 planning as a production objective until:

- 3000 production gate passes.
- 3000 metrics show clear headroom.
- 3000 incident playbooks are validated.
- 3000 lane tuning rules have been exercised at least once in staging or performance testing.

The current 4000 readiness notes remain valid as future direction:

- per-lane worker pool or per-lane ring buffer.
- async writer for trace / audit / stats / console-view state.
- physical Redis separation or equivalent isolation.
- adaptive retry controller.
- DAG cost profile.
- downstream bulkhead per dependency.

These are not required to declare 3000 complete unless 3000 testing proves they are needed earlier.

---

## 9. Non-Goals

This hardening spec does not require:

- Moving the official target from 3000 to 4000.
- Implementing per-lane ring buffers for the first 3000 release.
- Physically splitting Redis before measurement proves it is necessary.
- Solving downstream service capacity with Canvas code alone.
- Running production load tests from this document.

---

## 10. Outputs

This hardening effort should produce:

1. A test-baseline repair plan.
2. A 3000 mixed traffic profile.
3. Lane tuning and guardrail documentation.
4. Failure-mode handling rules.
5. A concrete 3000 rollout gate.
6. A clear decision that 4000 remains blocked until 3000 is complete.
