import test from 'node:test'
import assert from 'node:assert/strict'
import {
  aggregateWorkerSummaries,
  assertDistributedReportable,
  buildDistributedPlan,
  validateWorkerSummaries,
  workerRunnerArgs,
} from './distributed-report.mjs'
import { latencyBucketsForDurations } from './perf-runner.mjs'

function workerSummary({
  workerId,
  seqStart,
  seqCount,
  durations,
  perfRunId = 'perf_dist_001',
  startedAt = '2026-06-03T00:00:00.000Z',
  finishedAt = '2026-06-03T00:00:10.000Z',
  failed = 0,
}) {
  const success = durations.length - failed
  return {
    perfRunId,
    workerId,
    mode: 'event',
    seqStart,
    seqCount,
    sent: durations.length,
    success,
    failed,
    p95Ms: durations.at(-1) || 0,
    minMs: durations[0] || 0,
    maxMs: durations.at(-1) || 0,
    avgMs: durations.length
      ? durations.reduce((sum, duration) => sum + duration, 0) / durations.length
      : 0,
    latencyBuckets: latencyBucketsForDurations(durations),
    startedAt,
    finishedAt,
    durationMs: 10_000,
    machine: { hostname: workerId },
  }
}

function planFixture() {
  return buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 4,
    totalConcurrency: 2,
    workerIds: ['worker-01', 'worker-02'],
    createdAt: '2026-06-03T00:00:00.000Z',
  })
}

test('buildDistributedPlan splits count and concurrency across workers', () => {
  const plan = buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 10,
    totalConcurrency: 4,
    workerIds: ['worker-01', 'worker-02', 'worker-03'],
    createdAt: '2026-06-03T00:00:00.000Z',
  })

  assert.equal(plan.perfRunId, 'perf_dist_001')
  assert.equal(plan.totalCount, 10)
  assert.equal(plan.totalConcurrency, 4)
  assert.deepEqual(plan.workers.map((worker) => worker.seqStart), [1, 5, 8])
  assert.deepEqual(plan.workers.map((worker) => worker.count), [4, 3, 3])
  assert.deepEqual(plan.workers.map((worker) => worker.concurrency), [2, 1, 1])
})

test('buildDistributedPlan rejects duplicate worker ids', () => {
  assert.throws(() => buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 10,
    totalConcurrency: 4,
    workerIds: ['worker-01', 'worker-01'],
  }), /duplicate worker id worker-01/)
})

test('workerRunnerArgs forwards worker shard settings to perf runner', () => {
  const plan = buildDistributedPlan({
    perfRunId: 'perf_dist_001',
    mode: 'event',
    baseUrl: 'http://lb:8080',
    eventCode: 'PERF_ORDER_PAID',
    totalCount: 10,
    totalConcurrency: 4,
    workerIds: ['worker-01', 'worker-02', 'worker-03'],
  })
  const args = workerRunnerArgs({
    plan,
    workerId: 'worker-02',
    summaryFile: 'tmp/perf-distributed/perf_dist_001/workers/worker-02/runner-summary.json',
  })

  assert.equal(args[args.indexOf('--worker-id') + 1], 'worker-02')
  assert.equal(args[args.indexOf('--seq-start') + 1], '5')
  assert.equal(args[args.indexOf('--count') + 1], '3')
  assert.equal(args[args.indexOf('--concurrency') + 1], '1')
})

test('validateWorkerSummaries rejects missing workers', () => {
  const plan = planFixture()

  assert.throws(() => validateWorkerSummaries(plan, [
    workerSummary({ workerId: 'worker-01', seqStart: 1, seqCount: 2, durations: [100, 200] }),
  ]), /missing worker summary worker-02/)
})

test('validateWorkerSummaries rejects sequence gaps', () => {
  const plan = planFixture()
  plan.workers[1].seqStart = 4

  assert.throws(() => validateWorkerSummaries(plan, [
    workerSummary({ workerId: 'worker-01', seqStart: 1, seqCount: 2, durations: [100, 200] }),
    workerSummary({ workerId: 'worker-02', seqStart: 4, seqCount: 2, durations: [300, 500] }),
  ]), /sequence gap before worker worker-02/)
})

test('aggregateWorkerSummaries computes global counts and percentiles', () => {
  const summary = aggregateWorkerSummaries(planFixture(), [
    workerSummary({
      workerId: 'worker-01',
      seqStart: 1,
      seqCount: 2,
      durations: [100, 200],
      startedAt: '2026-06-03T00:00:00.000Z',
      finishedAt: '2026-06-03T00:00:10.000Z',
    }),
    workerSummary({
      workerId: 'worker-02',
      seqStart: 3,
      seqCount: 2,
      durations: [50, 500],
      startedAt: '2026-06-03T00:00:01.000Z',
      finishedAt: '2026-06-03T00:00:12.000Z',
    }),
  ])

  assert.equal(summary.perfRunId, 'perf_dist_001')
  assert.equal(summary.sent, 4)
  assert.equal(summary.success, 4)
  assert.equal(summary.failed, 0)
  assert.equal(summary.durationMs, 12_000)
  assert.equal(summary.p95Ms, 500)
  assert.equal(summary.p99Ms, 500)
})

test('assertDistributedReportable rejects failed requests and short capacity duration', () => {
  assert.throws(() => assertDistributedReportable({
    perfRunId: 'perf_dist_001',
    sent: 4,
    success: 3,
    failed: 1,
    durationMs: 1_800_000,
    p95Ms: 100,
    p99Ms: 100,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
  }), /distributed runner reported 1 failed request/)

  assert.throws(() => assertDistributedReportable({
    perfRunId: 'perf_dist_001',
    sent: 4,
    success: 4,
    failed: 0,
    durationMs: 60_000,
    p95Ms: 100,
    p99Ms: 100,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
  }), /distributed duration 60000ms is below required 1800000ms/)
})
