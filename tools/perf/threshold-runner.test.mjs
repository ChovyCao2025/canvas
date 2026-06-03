import test from 'node:test'
import assert from 'node:assert/strict'

import {
  classifyStage,
  parseStages,
  parseThresholdArgs,
  runnerArgs,
  runThresholdPlan,
} from './threshold-runner.mjs'

test('parseStages parses count and concurrency pairs', () => {
  assert.deepEqual(parseStages('1000:10,5000:50,10000:100'), [
    { count: 1000, concurrency: 10 },
    { count: 5000, concurrency: 50 },
    { count: 10000, concurrency: 100 },
  ])
})

test('parseThresholdArgs accepts event secret env', () => {
  const args = parseThresholdArgs([
    '--mode', 'event',
    '--event-secret-env', 'CANVAS_EVENT_REPORT_SECRET',
  ])

  assert.equal(args.eventSecretEnv, 'CANVAS_EVENT_REPORT_SECRET')
})

test('runnerArgs passes event secret env to perf runner for event mode', () => {
  const args = runnerArgs({
    config: {
      mode: 'event',
      baseUrl: 'http://localhost:8080',
      eventCode: 'PERF_ORDER_PAID',
      eventSecretEnv: 'PERF_EVENT_SECRET',
    },
    stage: { count: 100, concurrency: 10 },
    perfRunId: 'perf_20260523_001',
    summaryFile: 'tmp/perf_20260523_001.json',
  })

  assert.deepEqual(args.slice(-2), ['--event-secret-env', 'PERF_EVENT_SECRET'])
})

test('runnerArgs passes event secret env to perf runner for direct mode', () => {
  const args = runnerArgs({
    config: {
      mode: 'direct',
      baseUrl: 'http://localhost:8080',
      canvasId: '42',
      eventSecretEnv: 'PERF_EVENT_SECRET',
    },
    stage: { count: 100, concurrency: 10 },
    perfRunId: 'perf_20260523_001',
    summaryFile: 'tmp/perf_20260523_001.json',
  })

  assert.deepEqual(args.slice(-2), ['--event-secret-env', 'PERF_EVENT_SECRET'])
})

test('parseThresholdArgs rejects explicit event secret flag', () => {
  assert.throws(() => parseThresholdArgs([
    '--mode', 'event',
    '--event-secret', '12345678901234567890123456789012',
  ]), /Unknown flag: --event-secret/)
})

test('classifyStage fails when runner has request failures', () => {
  const result = classifyStage({
    summary: { failed: 1, success: 99, p95Ms: 120 },
    verifier: { verdict: 'PASS' },
    maxFailed: 0,
    maxP95Ms: 500,
  })

  assert.equal(result.ok, false)
  assert.equal(result.reason, 'RUNNER_FAILED')
})

test('classifyStage fails when p95 exceeds threshold', () => {
  const result = classifyStage({
    summary: { failed: 0, success: 100, p95Ms: 900 },
    verifier: { verdict: 'PASS' },
    maxFailed: 0,
    maxP95Ms: 500,
  })

  assert.equal(result.ok, false)
  assert.equal(result.reason, 'P95_EXCEEDED')
})

test('runThresholdPlan stops at first verifier failure and returns last stable stage', async () => {
  const calls = []
  const result = await runThresholdPlan({
    mode: 'event',
    baseUrl: 'http://localhost:8080',
    eventCode: 'PERF_ORDER_PAID',
    stages: [
      { count: 1000, concurrency: 10 },
      { count: 5000, concurrency: 50 },
      { count: 10000, concurrency: 100 },
    ],
    matchedCanvasCount: 1,
    maxFailed: 0,
    maxP95Ms: 500,
    outDir: 'tmp/test-threshold',
    waitAfterRunMs: 0,
    runIdPrefix: 'test_run',
  }, {
    runRunner: async ({ stageIndex }) => {
      calls.push(['runner', stageIndex])
      return { sent: 1000, success: 1000, failed: 0, p95Ms: 120, durationMs: 1000 }
    },
    runVerifier: async ({ stageIndex }) => {
      calls.push(['verifier', stageIndex])
      return stageIndex === 1
        ? { verdict: 'FAIL', unexpectedLoss: 1 }
        : { verdict: 'PASS' }
    },
    now: () => '2026-05-27T00:00:00.000Z',
    wait: async () => {},
  })

  assert.equal(result.verdict, 'THRESHOLD_FOUND')
  assert.equal(result.stableStage.stageIndex, 0)
  assert.equal(result.failedStage.stageIndex, 1)
  assert.equal(result.failedStage.reason, 'VERIFIER_FAIL')
  assert.deepEqual(calls, [
    ['runner', 0],
    ['verifier', 0],
    ['runner', 1],
    ['verifier', 1],
  ])
})
