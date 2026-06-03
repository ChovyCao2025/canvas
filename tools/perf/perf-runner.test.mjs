import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildEventPayload,
  buildDirectPayload,
  buildEventSignatureHeaders,
  buildSignedHeaders,
  chunkSeq,
  exitCodeForSummary,
  isCliEntrypoint,
  parseRunnerArgs,
  resolveEventSecret,
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

test('buildEventSignatureHeaders signs timestamp and raw body', () => {
  const headers = buildEventSignatureHeaders({
    secret: '12345678901234567890123456789012',
    timestamp: '1760000000000',
    rawBody: '{"eventCode":"PERF_ORDER_PAID"}',
  })

  assert.equal(headers['X-Canvas-Timestamp'], '1760000000000')
  assert.equal(
    headers['X-Canvas-Signature'],
    'sha256=b459181d5e6609aecf71072654181700c178d19e765700e19ce36fa458be21da',
  )
})

test('resolveEventSecret reads only the configured environment variable', () => {
  assert.deepEqual(resolveEventSecret({
    mode: 'event',
    eventSecret: '12345678901234567890123456789012',
    eventSecretEnv: 'PERF_EVENT_SECRET',
  }, {
    PERF_EVENT_SECRET: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  }), {
    value: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    source: 'env:PERF_EVENT_SECRET',
  })
})

test('resolveEventSecret reads configured environment variable', () => {
  assert.deepEqual(resolveEventSecret({
    mode: 'event',
    eventSecretEnv: 'PERF_EVENT_SECRET',
  }, {
    PERF_EVENT_SECRET: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  }), {
    value: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    source: 'env:PERF_EVENT_SECRET',
  })
})

test('buildSignedHeaders returns JSON headers when no event secret is configured', () => {
  const headers = buildSignedHeaders({
    args: { mode: 'event', eventSecretEnv: 'PERF_EVENT_SECRET' },
    rawBody: '{}',
    nowMs: () => 1760000000000,
    env: {},
  })

  assert.deepEqual(headers, {
    'content-type': 'application/json',
  })
})

test('buildSignedHeaders includes event HMAC headers when secret is configured', () => {
  const headers = buildSignedHeaders({
    args: { mode: 'event', eventSecretEnv: 'PERF_EVENT_SECRET' },
    rawBody: '{"eventCode":"PERF_ORDER_PAID"}',
    nowMs: () => 1760000000000,
    env: { PERF_EVENT_SECRET: '12345678901234567890123456789012' },
  })

  assert.equal(headers['content-type'], 'application/json')
  assert.equal(headers['X-Canvas-Timestamp'], '1760000000000')
  assert.equal(
    headers['X-Canvas-Signature'],
    'sha256=b459181d5e6609aecf71072654181700c178d19e765700e19ce36fa458be21da',
  )
})

test('buildSignedHeaders includes machine HMAC headers for direct mode when secret is configured', () => {
  const headers = buildSignedHeaders({
    args: { mode: 'direct', eventSecretEnv: 'PERF_EVENT_SECRET' },
    rawBody: '{"userId":"perf_user_1"}',
    nowMs: () => 1760000000000,
    env: { PERF_EVENT_SECRET: '12345678901234567890123456789012' },
  })

  assert.equal(headers['content-type'], 'application/json')
  assert.equal(headers['X-Canvas-Timestamp'], '1760000000000')
  assert.equal(
    headers['X-Canvas-Signature'],
    'sha256=f79510582ae819a259be30328a5f2835e25db4c560f0ab1d51a52c436409a5b8',
  )
})

test('parseRunnerArgs accepts event secret env flag without exposing a secret value', () => {
  const args = parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--event-secret-env', 'CANVAS_EVENT_REPORT_SECRET',
  ])

  assert.equal('eventSecret' in args, false)
  assert.equal(args.eventSecretEnv, 'CANVAS_EVENT_REPORT_SECRET')
})

test('parseRunnerArgs rejects event secret flag', () => {
  assert.throws(() => parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--event-secret', '12345678901234567890123456789012',
  ]), /Unknown flag: --event-secret/)
})

test('parseRunnerArgs throws for unsupported mode', () => {
  assert.throws(() => parseRunnerArgs([
    '--mode', 'batch',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
  ]), /--mode must be one of event, direct, audience/)
})

test('parseRunnerArgs throws for zero concurrency', () => {
  assert.throws(() => parseRunnerArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--concurrency', '0',
  ]), /--concurrency must be a positive integer/)
})

test('parseRunnerArgs throws for invalid count', () => {
  assert.throws(() => parseRunnerArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--count', 'abc',
  ]), /--count must be a non-negative integer/)
})

test('parseRunnerArgs throws for direct mode without canvas id', () => {
  assert.throws(() => parseRunnerArgs([
    '--mode', 'direct',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
  ]), /--canvas-id is required for direct mode/)
})

test('parseRunnerArgs throws for audience mode without audience id', () => {
  assert.throws(() => parseRunnerArgs([
    '--mode', 'audience',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
  ]), /--audience-id is required for audience mode/)
})

test('exitCodeForSummary maps failed requests to exit code 2', () => {
  assert.equal(exitCodeForSummary({ failed: 1 }), 2)
  assert.equal(exitCodeForSummary({ failed: 0 }), 0)
})

test('isCliEntrypoint is safe when argv path is missing', () => {
  assert.equal(isCliEntrypoint(import.meta.url, undefined), false)
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

test('run summary records event signature source without leaking secret', async () => {
  const summary = await run(parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
    '--event-secret-env', 'PERF_EVENT_SECRET',
  ]), {
    env: { PERF_EVENT_SECRET: '12345678901234567890123456789012' },
    machineMetadata: () => ({}),
  })

  assert.deepEqual(summary.settings.eventSignature, {
    enabled: true,
    source: 'env:PERF_EVENT_SECRET',
  })
  assert.doesNotMatch(JSON.stringify(summary), /12345678901234567890123456789012/)
})
