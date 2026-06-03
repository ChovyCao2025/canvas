# Distributed Capacity Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build distributed pressure testing support for multiple load workers and multiple backend nodes, with unified evidence, accuracy gates, and Chinese runbook documentation.

**Architecture:** Keep the existing single-machine perf flow intact and add a distributed layer around it. `perf-runner.mjs` becomes shard-aware and emits mergeable latency evidence; a new `distributed-report.mjs` owns plan creation, worker command generation, summary aggregation, global percentile calculation, and evidence validation; `perf-guide.mjs` exposes the distributed commands and reuses existing verifier and side-effect gates.

**Tech Stack:** Node.js ESM CLI scripts, Node built-in test runner, existing MySQL/WireMock verifier scripts, Markdown documentation.

---

## File Structure

- Modify `tools/perf/perf-runner.mjs`: add worker id, global sequence start, mergeable latency buckets, and metadata fields.
- Modify `tools/perf/perf-runner.test.mjs`: add TDD coverage for sharded sequence generation and latency buckets.
- Create `tools/perf/distributed-report.mjs`: pure functions plus CLI helpers for distributed plan, worker execution arguments, summary aggregation, and report validation.
- Create `tools/perf/distributed-report.test.mjs`: TDD coverage for plan sharding, overlap/gap detection, global percentiles, missing evidence, and accuracy report gates.
- Modify `tools/perf/perf-guide.mjs`: add `distributed-plan`, `distributed-worker`, and `distributed-report` subcommands.
- Modify `tools/perf/perf-guide.test.mjs`: add guide command parsing and dispatch tests.
- Create `docs/stressTest/distributed-capacity-runbook.md`: Chinese multi-machine runbook.
- Modify `docs/stressTest/README.md`: link local and distributed runbooks.
- Modify `docs/stressTest/report-template.md`: add distributed evidence fields.
- Modify `tools/perf/README.md`: document supported distributed entry points.

## Task 1: Runner Shards And Latency Buckets

**Files:**
- Modify: `tools/perf/perf-runner.mjs`
- Modify: `tools/perf/perf-runner.test.mjs`

- [ ] **Step 1: Write failing runner tests**

Add tests that demonstrate the new API:

```js
test('chunkSeq supports a global sequence start', () => {
  assert.deepEqual([...chunkSeq(5, 2, 101)], [[101, 102], [103, 104], [105]])
})

test('buildDirectPayload uses global sequence in idempotency key', () => {
  const payload = buildDirectPayload({
    perfRunId: 'perf_dist_001',
    userPrefix: 'user_',
    seq: 101,
    userModulo: 1000,
  })
  assert.equal(payload.idempotencyKey, 'perf_dist_001:direct:101')
  assert.equal(payload.inputParams.perfInputId, 'perf_dist_001:direct:101')
})

test('parseRunnerArgs accepts worker id and sequence start', () => {
  const args = parseRunnerArgs([
    '--perf-run-id', 'perf_dist_001',
    '--count', '10',
    '--concurrency', '2',
    '--worker-id', 'worker-01',
    '--seq-start', '101',
  ])
  assert.equal(args.workerId, 'worker-01')
  assert.equal(args.seqStart, 101)
})

test('run summary includes shard and latency bucket evidence', async () => {
  const summary = await run(parseRunnerArgs([
    '--perf-run-id', 'perf_dist_001',
    '--count', '0',
    '--worker-id', 'worker-01',
    '--seq-start', '101',
  ]), {
    machineMetadata: () => ({ hostname: 'worker-host' }),
  })
  assert.equal(summary.workerId, 'worker-01')
  assert.equal(summary.seqStart, 101)
  assert.equal(summary.seqCount, 0)
  assert.ok(Array.isArray(summary.latencyBuckets))
  assert.equal(summary.minMs, 0)
  assert.equal(summary.maxMs, 0)
  assert.equal(summary.avgMs, 0)
})
```

- [ ] **Step 2: Verify runner tests fail**

Run: `node --test tools/perf/perf-runner.test.mjs`

Expected: FAIL because `--worker-id`, `--seq-start`, three-argument `chunkSeq`, and latency bucket fields do not exist yet.

- [ ] **Step 3: Implement minimal runner support**

Add defaults, flags, integer parsing, `chunkSeq(count, concurrency, seqStart = 1)`, latency bucket helpers, and summary metadata. Keep default behavior byte-compatible for existing callers except for additive summary fields.

- [ ] **Step 4: Verify runner tests pass**

Run: `node --test tools/perf/perf-runner.test.mjs`

Expected: PASS.

## Task 2: Distributed Plan And Summary Aggregation

**Files:**
- Create: `tools/perf/distributed-report.mjs`
- Create: `tools/perf/distributed-report.test.mjs`

- [ ] **Step 1: Write failing distributed unit tests**

Add tests for these behaviors:

```js
test('buildDistributedPlan splits count and concurrency across workers', () => {
  const plan = buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 10,
    totalConcurrency: 4,
    workerIds: ['worker-01', 'worker-02', 'worker-03'],
    startedAt: '2026-06-03T00:00:00.000Z',
  })
  assert.deepEqual(plan.workers.map((worker) => worker.seqStart), [1, 5, 8])
  assert.deepEqual(plan.workers.map((worker) => worker.count), [4, 3, 3])
  assert.deepEqual(plan.workers.map((worker) => worker.concurrency), [2, 1, 1])
})

test('validateWorkerSummaries rejects missing workers and sequence gaps', () => {
  const plan = buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 4,
    totalConcurrency: 2,
    workerIds: ['worker-01', 'worker-02'],
  })
  assert.throws(() => validateWorkerSummaries(plan, []), /missing worker summary/)
})

test('aggregateWorkerSummaries computes global counts and percentiles', () => {
  const summary = aggregateWorkerSummaries(planFixture, workerSummaryFixture)
  assert.equal(summary.sent, 4)
  assert.equal(summary.success, 4)
  assert.equal(summary.failed, 0)
  assert.equal(summary.p95Ms, 500)
  assert.equal(summary.p99Ms, 500)
})
```

- [ ] **Step 2: Verify distributed tests fail**

Run: `node --test tools/perf/distributed-report.test.mjs`

Expected: FAIL because the file and functions do not exist yet.

- [ ] **Step 3: Implement distributed pure functions**

Implement `buildDistributedPlan`, `workerRunnerArgs`, `validateWorkerSummaries`, `aggregateLatencyBuckets`, `percentileFromBuckets`, `aggregateWorkerSummaries`, and `assertDistributedReportable`. Validate duplicate worker IDs, missing workers, wrong `perfRunId`, sequence overlaps, sequence gaps, failed requests, missing latency buckets, and insufficient duration for capacity reports.

- [ ] **Step 4: Verify distributed tests pass**

Run: `node --test tools/perf/distributed-report.test.mjs`

Expected: PASS.

## Task 3: Guide Distributed Commands

**Files:**
- Modify: `tools/perf/perf-guide.mjs`
- Modify: `tools/perf/perf-guide.test.mjs`

- [ ] **Step 1: Write failing guide tests**

Add tests that parse and dispatch:

```js
test('parseGuideArgs accepts distributed commands and worker settings', () => {
  const args = parseGuideArgs([
    'distributed-plan',
    '--perf-run-id', 'perf_dist_001',
    '--worker-ids', 'worker-01,worker-02',
    '--total-count', '100',
    '--total-concurrency', '10',
  ])
  assert.equal(args.command, 'distributed-plan')
  assert.equal(args.workerIds, 'worker-01,worker-02')
  assert.equal(args.totalCount, 100)
  assert.equal(args.totalConcurrency, 10)
})

test('runGuide dispatches distributed-plan command', async () => {
  const result = await runGuide(parseGuideArgs([
    'distributed-plan',
    '--perf-run-id', 'perf_dist_001',
    '--worker-ids', 'worker-01,worker-02',
    '--total-count', '100',
    '--total-concurrency', '10',
  ]))
  assert.equal(result.status, 'PASS')
  assert.equal(result.plan.perfRunId, 'perf_dist_001')
})
```

- [ ] **Step 2: Verify guide tests fail**

Run: `node --test tools/perf/perf-guide.test.mjs`

Expected: FAIL because distributed commands and flags are not registered.

- [ ] **Step 3: Implement guide handlers**

Register `distributed-plan`, `distributed-worker`, and `distributed-report`; add flags `--worker-ids`, `--worker-id`, `--total-count`, `--total-concurrency`, `--plan-file`, `--evidence-dir`, and `--distributed-root`; call the distributed module functions and existing verifier/side-effect verifier commands.

- [ ] **Step 4: Verify guide tests pass**

Run: `node --test tools/perf/perf-guide.test.mjs`

Expected: PASS.

## Task 4: Chinese Distributed Runbook

**Files:**
- Create: `docs/stressTest/distributed-capacity-runbook.md`
- Modify: `docs/stressTest/README.md`
- Modify: `docs/stressTest/report-template.md`
- Modify: `tools/perf/README.md`

- [ ] **Step 1: Add docs after code behavior exists**

Document the full distributed flow in Chinese:

```text
单机 doctor -> fixture -> smoke -> accuracy
distributed-plan
distributed-worker on each load worker
collect worker summaries
distributed-report
distributed cleanup/report evidence
```

Include multi-load-worker and multi-backend-node topology, worker list, backend list, LB requirements, shared MySQL/Redis/RocketMQ/WireMock requirements, time sync, monitor evidence, stop conditions, and accuracy evidence.

- [ ] **Step 2: Verify docs mention all commands**

Run: `rg -n "distributed-plan|distributed-worker|distributed-report|多后端|多压测机" docs/stressTest tools/perf/README.md`

Expected: all new commands and topology sections are present.

## Task 5: Full Verification And Merge Preparation

**Files:**
- All files touched by the previous tasks.

- [ ] **Step 1: Run full perf tests**

Run: `node --test tools/perf/*.test.mjs`

Expected: all tests pass.

- [ ] **Step 2: Run guide doctor**

Run: `node tools/perf/perf-guide.mjs doctor`

Expected: JSON with `"status": "PASS"`.

- [ ] **Step 3: Run whitespace check**

Run: `git diff --check`

Expected: no output and exit code 0.

- [ ] **Step 4: Inspect worktree state**

Run: `git status --short --branch`

Expected: only intended distributed perf files are changed before commit.

- [ ] **Step 5: Commit implementation**

Run:

```bash
git add tools/perf docs/stressTest docs/superpowers/plans docs/superpowers/specs
git commit -m "feat: support distributed perf testing"
```

- [ ] **Step 6: Merge to main**

From `/Users/photonpay/project/canvas`, merge the branch into `main` without reverting unrelated dirty `docs/product-evolution` changes. If main has unrelated dirty changes, verify they do not overlap with this branch and merge only after tests pass on the feature branch.

