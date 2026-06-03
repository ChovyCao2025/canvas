import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'

import {
  assertCapacityReportable,
  assertRunnerReportable,
  assertRunnerSummaryComplete,
  assertVerifierEvidenceComplete,
  commandForCleanup,
  defaultRunScenario,
  parseGuideArgs,
  runGuide,
  writeJson,
} from './perf-guide.mjs'

function completeVerifierEvidence(overrides = {}) {
  const evidence = {
    verdict: 'PASS',
    perfRunId: 'perf_report_001',
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 100,
    success: 100,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    intentionalDuplicates: 0,
    expectedFailedWithRecord: 0,
    expectedRejectedWithRecord: 0,
    expectedDlq: 0,
    unexpectedFailedWithRecord: 0,
    unexpectedRejectedWithRecord: 0,
    unexpectedDlq: 0,
    deduplicated: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
    unexpectedLoss: 0,
    wrongAudienceCount: 0,
    traceEnabled: false,
    traceCounts: [],
    traceMismatch: 0,
    traceMismatches: [],
    traceFailed: 0,
    traceDuplicateSuccess: 0,
    traceBufferPending: 0,
    traceVerdict: 'PASS',
    ...overrides,
  }
  return {
    ...evidence,
    ledgerCounts: {
      eventLog: evidence.accepted,
      requestRows: 0,
      requestSources: 0,
      executions: evidence.actualExecutions,
      success: evidence.success,
      failed: evidence.failedWithRecord,
      dlq: evidence.dlq,
      retryPending: evidence.retryPending,
      rejectedWithRecord: evidence.rejectedWithRecord,
      duplicateInput: 0,
      audienceRuns: 0,
      ...(overrides.ledgerCounts || {}),
    },
  }
}

test('parseGuideArgs parses subcommand and common flags', () => {
  const config = parseGuideArgs([
    'doctor',
    '--base-url', 'http://localhost:8080',
    '--run-root', 'tmp/perf-runs',
  ])

  assert.equal(config.command, 'doctor')
  assert.equal(config.baseUrl, 'http://localhost:8080')
  assert.equal(config.runRoot, 'tmp/perf-runs')
})

test('parseGuideArgs rejects unknown subcommand', () => {
  assert.throws(() => parseGuideArgs(['unknown']), /command must be one of/)
})

test('assertCapacityReportable accepts PASS verifier for capacity report', () => {
  assert.doesNotThrow(() => assertCapacityReportable({
    verdict: 'PASS',
  }, {
    reportType: 'capacity',
  }))
})

test('assertCapacityReportable rejects FAIL verifier', () => {
  assert.throws(() => assertCapacityReportable({
    verdict: 'FAIL',
  }, {
    reportType: 'capacity',
  }), /verifier verdict FAIL cannot be used for capacity reporting/)
})

test('assertCapacityReportable rejects expected failures for capacity report', () => {
  assert.throws(() => assertCapacityReportable({
    verdict: 'PASS_WITH_EXPECTED_FAILURES',
  }, {
    reportType: 'capacity',
  }), /PASS_WITH_EXPECTED_FAILURES is only allowed for fault reports/)
})

test('assertCapacityReportable accepts expected failures for fault report', () => {
  assert.doesNotThrow(() => assertCapacityReportable({
    verdict: 'PASS_WITH_EXPECTED_FAILURES',
  }, {
    reportType: 'fault',
  }))
})

test('assertVerifierEvidenceComplete rejects truncated verifier evidence', () => {
  assert.throws(() => assertVerifierEvidenceComplete({
    verdict: 'PASS',
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }, {
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }), /verifier evidence field planned must be a non-negative integer/)
})

test('assertVerifierEvidenceComplete accepts complete verifier evidence', () => {
  assert.doesNotThrow(() => assertVerifierEvidenceComplete(completeVerifierEvidence({
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }), {
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }))
})

test('assertVerifierEvidenceComplete rejects missing trace evidence fields', () => {
  const evidence = completeVerifierEvidence({
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  })
  delete evidence.traceMismatch

  assert.throws(() => assertVerifierEvidenceComplete(evidence, {
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }), /traceMismatch/)
})

test('assertVerifierEvidenceComplete rejects internally inconsistent trace evidence', () => {
  assert.throws(() => assertVerifierEvidenceComplete(completeVerifierEvidence({
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
    verdict: 'PASS',
    traceEnabled: true,
    traceCounts: [
      { nodeId: 'event', status: 1, statusName: 'success', expected: 100, actual: 99 },
    ],
    traceMismatch: 0,
    traceMismatches: [],
    traceVerdict: 'PASS',
  }), {
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }), /traceMismatch/)
})

test('assertVerifierEvidenceComplete rejects internally inconsistent verifier evidence', () => {
  assert.throws(() => assertVerifierEvidenceComplete(completeVerifierEvidence({
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
    actualExecutions: 99,
    success: 99,
  }), {
    perfRunId: 'perf_report_001',
    sentSuccess: 100,
  }), /unexpectedLoss/)
})

test('assertRunnerReportable rejects failed runner requests', () => {
  assert.throws(() => assertRunnerReportable({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 99,
    failed: 1,
    durationMs: 1_800_000,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
    perfRunId: 'perf_report_001',
  }), /failed request/)
})

test('assertRunnerReportable rejects short capacity runs', () => {
  assert.throws(() => assertRunnerReportable({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 100,
    failed: 0,
    durationMs: 60_000,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
    perfRunId: 'perf_report_001',
  }), /duration/)
})

test('assertRunnerReportable rejects missing numeric runner evidence', () => {
  assert.throws(() => assertRunnerReportable({
    perfRunId: 'perf_report_001',
    success: 100,
    failed: 0,
    durationMs: 1_800_000,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
    perfRunId: 'perf_report_001',
  }), /sent must be a non-negative integer/)
})

test('assertRunnerReportable rejects mismatched run id', () => {
  assert.throws(() => assertRunnerReportable({
    perfRunId: 'perf_report_other',
    sent: 100,
    success: 100,
    failed: 0,
    durationMs: 1_800_000,
  }, {
    reportType: 'capacity',
    minDurationMin: 30,
    perfRunId: 'perf_report_001',
  }), /does not match/)
})

test('assertRunnerSummaryComplete rejects missing failed evidence', () => {
  assert.throws(() => assertRunnerSummaryComplete({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 100,
    durationMs: 1_800_000,
  }, {
    perfRunId: 'perf_report_001',
  }), /failed must be a non-negative integer/)
})

test('assertRunnerSummaryComplete rejects inconsistent counters', () => {
  assert.throws(() => assertRunnerSummaryComplete({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 90,
    failed: 0,
    durationMs: 1_800_000,
  }, {
    perfRunId: 'perf_report_001',
  }), /sent must equal success plus failed/)
})

test('assertRunnerSummaryComplete rejects negative counters', () => {
  assert.throws(() => assertRunnerSummaryComplete({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 101,
    failed: -1,
    durationMs: 1_800_000,
  }, {
    perfRunId: 'perf_report_001',
  }), /failed must be a non-negative integer/)
})

test('assertRunnerSummaryComplete accepts fractional duration evidence', () => {
  assert.doesNotThrow(() => assertRunnerSummaryComplete({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 100,
    failed: 0,
    durationMs: 1800.25,
  }, {
    perfRunId: 'perf_report_001',
  }))
})

test('assertRunnerSummaryComplete rejects negative duration evidence', () => {
  assert.throws(() => assertRunnerSummaryComplete({
    perfRunId: 'perf_report_001',
    sent: 100,
    success: 100,
    failed: 0,
    durationMs: -0.1,
  }, {
    perfRunId: 'perf_report_001',
  }), /durationMs must be a non-negative number/)
})

test('commandForCleanup defaults to ledger dry run', () => {
  assert.deepEqual(commandForCleanup({
    perfRunId: 'perf_20260523_001',
    scope: 'ledger',
    execute: false,
  }), [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', 'perf_20260523_001',
      '--scope', 'ledger',
      '--execute', 'false',
      '--mysql', 'mysql',
      '--database', 'canvas_db',
    ],
  ])
})

test('commandForCleanup forwards custom mysql and database settings', () => {
  assert.deepEqual(commandForCleanup({
    perfRunId: 'perf_20260523_002',
    scope: 'ledger',
    execute: false,
    mysql: 'mysql8',
    database: 'canvas_perf',
  }), [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', 'perf_20260523_002',
      '--scope', 'ledger',
      '--execute', 'false',
      '--mysql', 'mysql8',
      '--database', 'canvas_perf',
    ],
  ])
})

test('runGuide dispatches doctor command', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs(['doctor']), {
    doctor: async () => {
      calls.push('doctor')
      return { status: 'PASS' }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.deepEqual(calls, ['doctor'])
})

test('fixture command refuses rebuild without explicit flag', async () => {
  const result = await runGuide(parseGuideArgs(['fixture']), {})

  assert.equal(result.status, 'DRY_RUN')
  assert.match(result.message, /--rebuild true/)
})

test('fixture command creates and publishes standard perf canvases', async () => {
  const client = {
    async request(method, requestPath, { body } = {}) {
      if (requestPath === '/auth/login') return { code: 0, data: { token: 'token-1' } }
      if (requestPath.startsWith('/canvas/event-definitions')) return { code: 0, data: { total: 0, list: [] } }
      if (requestPath.startsWith('/canvas/list')) return { code: 0, data: { total: 0, list: [] } }
      if (method === 'POST' && requestPath === '/canvas') {
        const ids = {
          PERF_DIRECT_LIGHT: 11,
          PERF_EVENT_LIGHT: 12,
          PERF_ENGINE_ACCURACY: 13,
        }
        return { code: 0, data: { id: ids[body.name] } }
      }
      if (method === 'POST' && requestPath.endsWith('/publish?operator=perf')) {
        return { code: 0, data: { id: 99 } }
      }
      throw new Error(`unexpected ${method} ${requestPath}`)
    },
  }

  const result = await runGuide(parseGuideArgs(['fixture', '--rebuild', 'true']), {
    fixtureClient: client,
    env: {
      PERF_ADMIN_USERNAME: 'admin',
      PERF_ADMIN_PASSWORD: 'Admin@123',
    },
  })

  assert.equal(result.status, 'READY')
  assert.equal(result.directCanvasId, 11)
  assert.equal(result.eventCanvasId, 12)
  assert.equal(result.engineAccuracyCanvasId, 13)
})

test('smoke stops after first verifier failure', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async ({ mode }) => {
      calls.push(mode)
      return mode === 'direct'
        ? {
            summary: { perfRunId: 'perf_smoke_001_direct', sent: 50, success: 50, failed: 0, durationMs: 1000 },
            verifier: { verdict: 'FAIL' },
          }
        : {
            summary: { perfRunId: 'perf_smoke_001_event', sent: 100, success: 100, failed: 0, durationMs: 1000 },
            verifier: { verdict: 'PASS' },
          }
    },
  })

  assert.equal(result.status, 'FAIL')
  assert.equal(result.failedMode, 'direct')
  assert.deepEqual(calls, ['direct'])
})

test('smoke runs direct and event when both pass', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async ({ mode }) => {
      calls.push(mode)
      return {
        summary: { perfRunId: `perf_smoke_001_${mode}`, sent: 1, success: 1, failed: 0, durationMs: 1000 },
        verifier: { verdict: 'PASS' },
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.deepEqual(calls, ['direct', 'event'])
})

test('smoke fails when runner reports request failures', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async ({ mode }) => {
      calls.push(mode)
      return {
        summary: { perfRunId: `perf_smoke_001_${mode}`, sent: 50, success: 49, failed: 1, durationMs: 1000 },
        verifier: { verdict: 'PASS' },
      }
    },
  })

  assert.equal(result.status, 'FAIL')
  assert.equal(result.failedMode, 'direct')
  assert.match(result.reason, /failed request/)
  assert.deepEqual(calls, ['direct'])
})

test('smoke fails closed when runner summary is incomplete', async () => {
  await assert.rejects(() => runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async () => ({
      summary: { perfRunId: 'perf_smoke_001_direct', sent: 50, success: 50, durationMs: 1000 },
      verifier: { verdict: 'PASS' },
    }),
  }), /failed must be a non-negative integer/)
})

test('threshold delegates to threshold-runner with event secret env', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'event',
    '--event-secret-env', 'PERF_EVENT_SECRET',
  ]), {
    runCommand: (command, args) => {
      calls.push([command, args])
      return {
        status: 0,
        stdout: JSON.stringify({ verdict: 'MAX_STAGE_STABLE' }),
        stderr: '',
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.equal(calls[0][0], process.execPath)
  assert.ok(calls[0][1].includes('--event-secret-env'))
  assert.ok(calls[0][1].includes('PERF_EVENT_SECRET'))
})

test('threshold direct mode forwards event secret env for machine signing', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'direct',
    '--canvas-id', '42',
    '--event-secret-env', 'PERF_EVENT_SECRET',
  ]), {
    runCommand: (command, args) => {
      calls.push([command, args])
      return {
        status: 0,
        stdout: JSON.stringify({ verdict: 'MAX_STAGE_STABLE' }),
        stderr: '',
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.ok(calls[0][1].includes('--canvas-id'))
  assert.ok(calls[0][1].includes('--event-secret-env'))
  assert.ok(calls[0][1].includes('PERF_EVENT_SECRET'))
})

test('threshold forwards database and output directory settings', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'event',
    '--mysql', 'mysql8',
    '--database', 'canvas_perf',
    '--threshold-root', 'tmp/perf-threshold-custom',
  ]), {
    runCommand: (command, args) => {
      calls.push([command, args])
      return {
        status: 0,
        stdout: JSON.stringify({ verdict: 'MAX_STAGE_STABLE' }),
        stderr: '',
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.deepEqual(calls[0][1].slice(calls[0][1].indexOf('--mysql'), calls[0][1].indexOf('--mysql') + 2), [
    '--mysql', 'mysql8',
  ])
  assert.deepEqual(calls[0][1].slice(calls[0][1].indexOf('--database'), calls[0][1].indexOf('--database') + 2), [
    '--database', 'canvas_perf',
  ])
  assert.deepEqual(calls[0][1].slice(calls[0][1].indexOf('--out-dir'), calls[0][1].indexOf('--out-dir') + 2), [
    '--out-dir', 'tmp/perf-threshold-custom',
  ])
})

test('threshold rejects unknown verdict instead of reporting pass', async () => {
  await assert.rejects(() => runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'event',
  ]), {
    runCommand: () => ({
      status: 0,
      stdout: JSON.stringify({ verdict: 'PARTIAL_JSON' }),
      stderr: '',
    }),
  }), /unknown threshold verdict PARTIAL_JSON/)
})

test('threshold guide status cannot be overwritten by child JSON', async () => {
  const result = await runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'event',
  ]), {
    runCommand: () => ({
      status: 0,
      stdout: JSON.stringify({ verdict: 'NO_STABLE_STAGE', status: 'PASS' }),
      stderr: '',
    }),
  })

  assert.equal(result.status, 'FAIL')
})

test('soak rejects run shorter than minimum duration', async () => {
  const result = await runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--min-duration-min', '30',
  ]), {
    runScenario: async () => ({
      summary: { perfRunId: 'perf_soak_001', sent: 100, success: 100, failed: 0, durationMs: 60_000 },
      verifier: { verdict: 'PASS' },
    }),
  })

  assert.equal(result.status, 'FAIL')
  assert.match(result.reason, /duration/)
})

test('soak fails when runner reports request failures', async () => {
  const result = await runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--min-duration-min', '30',
  ]), {
    runScenario: async () => ({
      summary: { perfRunId: 'perf_soak_001', sent: 300000, success: 299999, failed: 1, durationMs: 1_800_000 },
      verifier: { verdict: 'PASS' },
    }),
  })

  assert.equal(result.status, 'FAIL')
  assert.match(result.reason, /failed request/)
})

test('soak fails closed when runner summary is incomplete', async () => {
  await assert.rejects(() => runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--min-duration-min', '30',
  ]), {
    runScenario: async () => ({
      summary: { perfRunId: 'perf_soak_001', sent: 300000, success: 300000, durationMs: 1_800_000 },
      verifier: { verdict: 'PASS' },
    }),
  }), /failed must be a non-negative integer/)
})

test('soak forwards configured count and concurrency', async () => {
  let scenarioOptions
  const result = await runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--count', '600000',
    '--concurrency', '200',
    '--min-duration-min', '1',
  ]), {
    runScenario: async (options) => {
      scenarioOptions = options
      return {
        summary: { perfRunId: 'perf_soak_001', sent: 600000, success: 600000, failed: 0, durationMs: 60_000 },
        verifier: { verdict: 'PASS' },
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.equal(scenarioOptions.count, 600000)
  assert.equal(scenarioOptions.concurrency, 200)
})

test('soak uses original perf run id for reportable evidence', async () => {
  let scenarioOptions
  const result = await runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--min-duration-min', '1',
  ]), {
    runScenario: async (options) => {
      scenarioOptions = options
      return {
        summary: { perfRunId: 'perf_soak_001', sent: 100, success: 100, failed: 0, durationMs: 60_000 },
        verifier: { verdict: 'PASS' },
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.equal(scenarioOptions.perfRunId, 'perf_soak_001')
})

test('accuracy runs runner, trace verifier, and side-effect verifier', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'accuracy',
    '--perf-run-id', 'perf_accuracy_001',
    '--canvas-id', '13',
    '--count', '2',
    '--concurrency', '1',
    '--wiremock-url', 'http://localhost:18099',
  ]), {
    runCommand: (command, args) => {
      calls.push([command, args])
      if (args[0] === 'tools/perf/perf-runner.mjs') {
        return {
          status: 0,
          stdout: JSON.stringify({ perfRunId: 'perf_accuracy_001', sent: 2, success: 2, failed: 0, durationMs: 1000 }),
          stderr: '',
        }
      }
      if (args[0] === 'tools/perf/verifier.mjs') {
        return {
          status: 0,
          stdout: JSON.stringify(completeVerifierEvidence({
            perfRunId: 'perf_accuracy_001',
            sentSuccess: 2,
            planned: 2,
            accepted: 2,
            expectedExecutions: 2,
            actualExecutions: 2,
            success: 2,
            traceEnabled: true,
            traceCounts: [
              { nodeId: 'direct', status: 1, statusName: 'success', expected: 2, actual: 2 },
            ],
          })),
          stderr: '',
        }
      }
      return {
        status: 0,
        stdout: JSON.stringify({ verdict: 'PASS', sideEffectVerdict: 'PASS', actualTotal: 2 }),
        stderr: '',
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.equal(calls[0][1][0], 'tools/perf/perf-runner.mjs')
  assert.equal(calls[1][1][0], 'tools/perf/verifier.mjs')
  assert.ok(calls[1][1].includes('--expect-trace'))
  assert.ok(calls[1][1].includes('send_even:success=even'))
  assert.equal(calls[2][1][0], 'tools/perf/side-effect-verifier.mjs')
  assert.deepEqual(calls[2][1].slice(calls[2][1].indexOf('--wiremock-url'), calls[2][1].indexOf('--wiremock-url') + 2), [
    '--wiremock-url', 'http://localhost:18099',
  ])
})

test('defaultRunScenario writes verifier evidence under provided perf run id', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-'))
  const calls = []

  try {
    const result = await defaultRunScenario({
      baseUrl: 'http://localhost:8080',
      runRoot,
      perfRunId: 'perf_soak_001',
      eventCode: 'PERF_ORDER_PAID',
      eventSecretEnv: 'PERF_EVENT_SECRET',
      canvasId: '42',
      mysql: 'mysql8',
      database: 'canvas_perf',
      matchedCanvasCount: 1,
    }, {
      mode: 'event',
      count: 1,
      concurrency: 1,
      perfRunId: 'perf_soak_001',
    }, {
      runCommand: (command, args) => {
        calls.push([command, args])
        if (args[0] === 'tools/perf/perf-runner.mjs') {
          return {
            status: 0,
            stdout: JSON.stringify({ perfRunId: 'perf_soak_001', sent: 1, success: 1, failed: 0, durationMs: 60_000 }),
            stderr: '',
          }
        }
        return {
          status: 0,
          stdout: JSON.stringify({ verdict: 'PASS' }),
          stderr: '',
        }
      },
    })

    assert.equal(result.perfRunId, 'perf_soak_001')
    assert.equal(calls[0][1][calls[0][1].indexOf('--perf-run-id') + 1], 'perf_soak_001')
    assert.equal(calls[1][1][calls[1][1].indexOf('--perf-run-id') + 1], 'perf_soak_001')
    assert.deepEqual(
      JSON.parse(readFileSync(path.join(runRoot, 'perf_soak_001', 'verifier.json'), 'utf8')),
      { verdict: 'PASS', perfRunId: 'perf_soak_001', sentSuccess: 1 },
    )
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('defaultRunScenario forwards event secret env for direct machine signing', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-'))
  const calls = []

  try {
    await defaultRunScenario({
      baseUrl: 'http://localhost:8080',
      runRoot,
      perfRunId: 'perf_smoke_001',
      eventCode: 'PERF_ORDER_PAID',
      eventSecretEnv: 'PERF_EVENT_SECRET',
      canvasId: '42',
      mysql: 'mysql',
      database: 'canvas_db',
      matchedCanvasCount: 1,
    }, {
      mode: 'direct',
      count: 50,
      concurrency: 5,
    }, {
      runCommand: (command, args) => {
        calls.push([command, args])
        if (args[0] === 'tools/perf/perf-runner.mjs') {
          return {
            status: 0,
            stdout: JSON.stringify({ perfRunId: 'perf_smoke_001_direct', sent: 50, success: 50, failed: 0, durationMs: 1000 }),
            stderr: '',
          }
        }
        return {
          status: 0,
          stdout: JSON.stringify({ verdict: 'PASS' }),
          stderr: '',
        }
      },
    })

    assert.ok(calls[0][1].includes('--event-secret-env'))
    assert.ok(calls[0][1].includes('PERF_EVENT_SECRET'))
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report rejects pass verifier when runner has request failures', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_001', 'runner-summary.json'), {
      perfRunId: 'perf_report_001',
      sent: 100,
      success: 99,
      failed: 1,
      durationMs: 1_800_000,
    })
    writeJson(path.join(runRoot, 'perf_report_001', 'verifier.json'), completeVerifierEvidence({
      perfRunId: 'perf_report_001',
      planned: 99,
      sentSuccess: 99,
      accepted: 99,
      expectedExecutions: 99,
      actualExecutions: 99,
      success: 99,
    }))

    await assert.rejects(() => runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_001',
      '--run-root', runRoot,
      '--min-duration-min', '30',
    ])), /failed request/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report rejects short capacity runs', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_002', 'runner-summary.json'), {
      perfRunId: 'perf_report_002',
      sent: 100,
      success: 100,
      failed: 0,
      durationMs: 60_000,
    })
    writeJson(path.join(runRoot, 'perf_report_002', 'verifier.json'), completeVerifierEvidence({
      perfRunId: 'perf_report_002',
      sentSuccess: 100,
    }))

    await assert.rejects(() => runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_002',
      '--run-root', runRoot,
      '--min-duration-min', '30',
    ])), /duration/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report accepts complete runner and verifier evidence', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_003', 'runner-summary.json'), {
      perfRunId: 'perf_report_003',
      sent: 100,
      success: 100,
      failed: 0,
      durationMs: 60_000,
    })
    writeJson(path.join(runRoot, 'perf_report_003', 'verifier.json'), completeVerifierEvidence({
      perfRunId: 'perf_report_003',
      sentSuccess: 100,
    }))

    const result = await runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_003',
      '--run-root', runRoot,
      '--min-duration-min', '1',
    ]))

    assert.equal(result.status, 'PASS')
    assert.equal(result.verifierVerdict, 'PASS')
    assert.match(result.summaryPath, /runner-summary\.json$/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report rejects stale verifier evidence with mismatched run id', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_004', 'runner-summary.json'), {
      perfRunId: 'perf_report_004',
      sent: 100,
      success: 100,
      failed: 0,
      durationMs: 60_000,
    })
    writeJson(path.join(runRoot, 'perf_report_004', 'verifier.json'), completeVerifierEvidence({
      perfRunId: 'perf_report_old',
      sentSuccess: 100,
    }))

    await assert.rejects(() => runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_004',
      '--run-root', runRoot,
      '--min-duration-min', '1',
    ])), /verifier perfRunId/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report rejects stale verifier evidence with mismatched sent success', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_005', 'runner-summary.json'), {
      perfRunId: 'perf_report_005',
      sent: 90,
      success: 90,
      failed: 0,
      durationMs: 60_000,
    })
    writeJson(path.join(runRoot, 'perf_report_005', 'verifier.json'), completeVerifierEvidence({
      perfRunId: 'perf_report_005',
      sentSuccess: 100,
    }))

    await assert.rejects(() => runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_005',
      '--run-root', runRoot,
      '--min-duration-min', '1',
    ])), /sentSuccess/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('report rejects trace mismatch evidence', async () => {
  const runRoot = mkdtempSync(path.join(tmpdir(), 'perf-guide-report-'))

  try {
    writeJson(path.join(runRoot, 'perf_report_006', 'runner-summary.json'), {
      perfRunId: 'perf_report_006',
      sent: 100,
      success: 100,
      failed: 0,
      durationMs: 60_000,
    })
    writeJson(path.join(runRoot, 'perf_report_006', 'verifier.json'), completeVerifierEvidence({
      verdict: 'FAIL',
      perfRunId: 'perf_report_006',
      sentSuccess: 100,
      traceEnabled: true,
      traceCounts: [
        { nodeId: 'event', status: 1, statusName: 'success', expected: 100, actual: 99 },
      ],
      traceMismatch: 1,
      traceMismatches: [
        { nodeId: 'event', status: 1, statusName: 'success', expected: 100, actual: 99 },
      ],
      traceVerdict: 'FAIL',
    }))

    await assert.rejects(() => runGuide(parseGuideArgs([
      'report',
      '--perf-run-id', 'perf_report_006',
      '--run-root', runRoot,
      '--min-duration-min', '1',
    ])), /verifier verdict FAIL/)
  } finally {
    rmSync(runRoot, { recursive: true, force: true })
  }
})

test('commandForCleanup treats string false as false for programmatic calls', () => {
  const [, args] = commandForCleanup({
    perfRunId: 'perf_20260523_003',
    scope: 'ledger',
    execute: 'false',
  })

  assert.deepEqual(args.slice(args.indexOf('--execute'), args.indexOf('--execute') + 2), [
    '--execute', 'false',
  ])
})
