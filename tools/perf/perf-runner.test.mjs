import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildEventPayload,
  buildDirectPayload,
  chunkSeq,
  parseRunnerArgs,
  run,
} from './perf-runner.mjs'

test('buildEventPayload produces deterministic perf input id', () => {
  const payload = buildEventPayload({
    perfRunId: 'perf_20260523_001',
    eventCode: 'PERF_ORDER_PAID',
    userPrefix: 'perf_user_',
    seq: 7,
    userModulo: 3,
  })

  assert.equal(payload.eventCode, 'PERF_ORDER_PAID')
  assert.equal(payload.userId, 'perf_user_1')
  assert.equal(payload.attributes.perfRunId, 'perf_20260523_001')
  assert.equal(payload.attributes.perfInputId, 'perf_20260523_001:event:7')
})

test('buildDirectPayload uses deterministic idempotency key', () => {
  const payload = buildDirectPayload({
    perfRunId: 'perf_20260523_001',
    seq: 9,
    userPrefix: 'perf_user_',
    userModulo: 4,
  })

  assert.equal(payload.userId, 'perf_user_1')
  assert.equal(payload.idempotencyKey, 'perf_20260523_001:direct:9')
  assert.equal(payload.inputParams.perfRunId, 'perf_20260523_001')
})

test('chunkSeq groups sequence numbers by concurrency', () => {
  assert.deepEqual([...chunkSeq(5, 2)], [[1, 2], [3, 4], [5]])
})

test('parseRunnerArgs reads required flags', () => {
  const args = parseRunnerArgs([
    '--mode', 'event',
    '--base-url', 'http://localhost:8080',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '10',
    '--concurrency', '2',
  ])

  assert.equal(args.mode, 'event')
  assert.equal(args.baseUrl, 'http://localhost:8080')
  assert.equal(args.perfRunId, 'perf_20260523_001')
  assert.equal(args.count, 10)
  assert.equal(args.concurrency, 2)
})

test('run includes metadata in zero-count summary', async () => {
  const summary = await run(parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
  ]))

  assert.equal(summary.perfRunId, 'perf_20260523_001')
  assert.equal(summary.mode, 'event')
  assert.equal(summary.sent, 0)
  assert.equal(summary.success, 0)
  assert.equal(summary.failed, 0)
  assert.equal(summary.p95Ms, 0)
})
