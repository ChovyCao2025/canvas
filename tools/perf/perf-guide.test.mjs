import test from 'node:test'
import assert from 'node:assert/strict'

import {
  assertCapacityReportable,
  commandForCleanup,
  parseGuideArgs,
  runGuide,
} from './perf-guide.mjs'

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
