import test from 'node:test'
import assert from 'node:assert/strict'

import {
  renderThresholdCommand,
  selectProfile,
  validateHardeningProfiles,
} from './hardening-profile.mjs'

const validConfig = {
  targetConcurrency: 3000,
  observationWindowSeconds: 1800,
  lanes: {
    LIGHT: { concurrency: 600, share: 0.2 },
    STANDARD: { concurrency: 1800, share: 0.6 },
    HEAVY: { concurrency: 300, share: 0.1 },
    RETRY: { concurrency: 300, share: 0.1 },
  },
  stopGates: ['RUNNER_FAILED', 'VERIFIER_FAIL'],
  profiles: [
    {
      name: 'default-mixed-3000',
      description: 'default',
      mode: 'event',
      eventCode: 'PERF_ORDER_PAID',
      stages: [
        { count: 10000, concurrency: 600 },
        { count: 50000, concurrency: 3000 },
      ],
      maxFailed: 0,
      maxP95Ms: 1000,
      waitAfterRunMs: 10000,
    },
  ],
}

test('validateHardeningProfiles accepts a 3000 lane total', () => {
  assert.equal(validateHardeningProfiles(validConfig).targetConcurrency, 3000)
})

test('validateHardeningProfiles rejects lane totals above target', () => {
  const invalid = structuredClone(validConfig)
  invalid.lanes.RETRY.concurrency = 301

  assert.throws(
    () => validateHardeningProfiles(invalid),
    /lane total 3001 must equal targetConcurrency 3000/,
  )
})

test('selectProfile finds a configured profile by name', () => {
  const profile = selectProfile(validConfig, 'default-mixed-3000')

  assert.equal(profile.name, 'default-mixed-3000')
})

test('renderThresholdCommand prints a deterministic threshold-runner command', () => {
  const profile = selectProfile(validConfig, 'default-mixed-3000')
  const command = renderThresholdCommand(profile, {
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: 'perf_3000_gate',
  })

  assert.match(command, /node tools\/perf\/threshold-runner\.mjs/)
  assert.match(command, /--mode event/)
  assert.match(command, /--event-code PERF_ORDER_PAID/)
  assert.match(command, /--stages 10000:600,50000:3000/)
  assert.match(command, /--max-failed 0/)
  assert.match(command, /--max-p95-ms 1000/)
})
