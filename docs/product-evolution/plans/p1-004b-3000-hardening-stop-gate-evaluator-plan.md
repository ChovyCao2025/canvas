# 3000 Hardening Stop Gate Evaluator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a deterministic evaluator that converts 3000 hardening metric samples into `PASS` or `STOP` decisions with named stop gates.

**Architecture:** Extend the existing Node.js capacity report module with a pure `evaluateHardeningGates(samples)` function and focused tests. This slice does not collect live metrics or write evidence manifests.

**Tech Stack:** Node.js built-in test runner, ES modules, `tools/perf/capacity-report.mjs`.

**Implementation Status:** Implemented and focused-verified on 2026-06-05. `capacity-report.mjs` now exports `evaluateHardeningGates(samples)` with deterministic gate ordering for Redis, MySQL, MQ, Disruptor overflow, retry backlog, DLQ, and protected lane latency samples.

---

## Spec Reference

- `docs/product-evolution/specs/p1-004b-3000-hardening-stop-gate-evaluator.md`
- Source: `docs/optimization/archive/3000-concurrency-hardening-checklist.md`

## File Structure

**Stop gate evaluator**
- Modify: `tools/perf/capacity-report.mjs` - exports `evaluateHardeningGates(samples)`.
- Modify: `tools/perf/capacity-report.test.mjs` - covers healthy and multi-stop samples.

### Task 1: Stop Gate Evaluator

**Files:**
- Modify: `tools/perf/capacity-report.mjs`
- Modify: `tools/perf/capacity-report.test.mjs`

- [x] **Step 1: Add stop gate evaluator tests**

Modify `tools/perf/capacity-report.test.mjs` import:

```js
import { estimateCapacity, evaluateHardeningGates, parseCapacityArgs } from './capacity-report.mjs'
```

Add these tests:

```js
test('evaluateHardeningGates passes healthy hardening samples', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 8,
    redisP99Ms: 20,
    mysqlActiveConnections: 40,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'PASS')
  assert.deepEqual(result.stopGates, [])
})

test('evaluateHardeningGates reports redis and mysql stop gates in stable order', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 25,
    redisP99Ms: 55,
    mysqlActiveConnections: 90,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 1200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'STOP')
  assert.deepEqual(result.stopGates, [
    'REDIS_REGISTRY_LATENCY_SUSTAINED',
    'MYSQL_POOL_SATURATION',
    'MYSQL_SLOW_SQL',
  ])
})

test('evaluateHardeningGates reports queue, retry, dlq, and protected lane stop gates', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 8,
    redisP99Ms: 20,
    mysqlActiveConnections: 40,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 200,
    normalMqBacklogGrowing: true,
    disruptorOverflowConsecutiveSamples: 2,
    retryBacklogGrowingAfterRecovery: true,
    dlqGrowingAfterRecovery: true,
    lightP95Ms: 1100,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'STOP')
  assert.deepEqual(result.stopGates, [
    'NORMAL_MQ_BACKLOG_STARVED_BY_RETRY',
    'DISRUPTOR_OVERFLOW_GROWING',
    'RETRY_BACKLOG_GROWING_AFTER_RECOVERY',
    'DLQ_GROWING_AFTER_RECOVERY',
    'PROTECTED_LANE_LATENCY_VIOLATION',
  ])
})
```

- [x] **Step 2: Run capacity tests and confirm red state**

Run:

```bash
node --test tools/perf/capacity-report.test.mjs
```

Expected: FAIL because `evaluateHardeningGates` is not exported.

Observed: FAIL on 2026-06-05 because `capacity-report.mjs` did not export `evaluateHardeningGates`.

- [x] **Step 3: Add hardening gate evaluator**

Modify `tools/perf/capacity-report.mjs`:

```js
export function evaluateHardeningGates(samples) {
  const stopGates = []
  if (samples.redisP95Ms > 20 || samples.redisP99Ms > 50) {
    stopGates.push('REDIS_REGISTRY_LATENCY_SUSTAINED')
  }
  if ((samples.mysqlActiveConnections / samples.mysqlMaxConnections) >= 0.85) {
    stopGates.push('MYSQL_POOL_SATURATION')
  }
  if (samples.mysqlSlowSqlMs > 1000) {
    stopGates.push('MYSQL_SLOW_SQL')
  }
  if (samples.normalMqBacklogGrowing) {
    stopGates.push('NORMAL_MQ_BACKLOG_STARVED_BY_RETRY')
  }
  if (samples.disruptorOverflowConsecutiveSamples >= 2) {
    stopGates.push('DISRUPTOR_OVERFLOW_GROWING')
  }
  if (samples.retryBacklogGrowingAfterRecovery) {
    stopGates.push('RETRY_BACKLOG_GROWING_AFTER_RECOVERY')
  }
  if (samples.dlqGrowingAfterRecovery) {
    stopGates.push('DLQ_GROWING_AFTER_RECOVERY')
  }
  if (samples.lightP95Ms > 1000 || samples.standardP95Ms > 1000) {
    stopGates.push('PROTECTED_LANE_LATENCY_VIOLATION')
  }
  return {
    verdict: stopGates.length === 0 ? 'PASS' : 'STOP',
    stopGates,
  }
}
```

- [x] **Step 4: Run capacity tests**

Run:

```bash
node --test tools/perf/capacity-report.test.mjs
```

Expected: PASS with existing capacity tests and new stop gate tests green.

Observed: PASS on 2026-06-05.

### Task 2: Verification And Commit

**Files:**
- Modify: `tools/perf/capacity-report.mjs`
- Modify: `tools/perf/capacity-report.test.mjs`
- Modify: `docs/product-evolution/specs/p1-004b-3000-hardening-stop-gate-evaluator.md`
- Modify: `docs/product-evolution/plans/p1-004b-3000-hardening-stop-gate-evaluator-plan.md`

- [x] **Step 1: Run focused Node tests**

Run:

```bash
node --test tools/perf/capacity-report.test.mjs
```

Expected: PASS.

Observed: PASS on 2026-06-05, 10 tests passed.

- [x] **Step 2: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add \
  tools/perf/capacity-report.mjs \
  tools/perf/capacity-report.test.mjs \
  docs/product-evolution/specs/p1-004b-3000-hardening-stop-gate-evaluator.md \
  docs/product-evolution/plans/p1-004b-3000-hardening-stop-gate-evaluator-plan.md
git commit -m "test: add 3000 hardening stop gates"
```

Expected: commit contains only stop-gate evaluator files and related docs.
