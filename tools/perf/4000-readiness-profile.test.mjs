import test from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

import {
  validateHardeningProfiles,
} from './hardening-profile.mjs'

const config = JSON.parse(readFileSync(new URL('./4000-readiness-profiles.json', import.meta.url), 'utf8'))

test('requiresExact4000TotalConcurrency', () => {
  const validated = validateHardeningProfiles(config)
  const total = Object.values(validated.lanes).reduce((sum, lane) => sum + lane.concurrency, 0)

  assert.equal(validated.targetConcurrency, 4000)
  assert.equal(total, 4000)
})

test('requiresLightStandardHeavyRetryLaneBudgets', () => {
  const validated = validateHardeningProfiles(config)

  assert.deepEqual(Object.keys(validated.lanes).sort(), ['HEAVY', 'LIGHT', 'RETRY', 'STANDARD'])
  assert.equal(validated.lanes.LIGHT.concurrency, 800)
  assert.equal(validated.lanes.STANDARD.concurrency, 2400)
  assert.equal(validated.lanes.HEAVY.concurrency, 400)
})

test('requiresNonZeroRetryBudget', () => {
  const validated = validateHardeningProfiles(config)

  assert.ok(validated.lanes.RETRY.concurrency > 0)
})

test('requiresMixedHeavyAndRetrySurgeScenarios', () => {
  const validated = validateHardeningProfiles(config)
  const names = new Set(validated.profiles.map((profile) => profile.name))

  assert.ok(names.has('readiness-mixed-4000'))
  assert.ok(names.has('heavy-surge-4000'))
  assert.ok(names.has('retry-surge-4000'))
})

test('requiresStopGatesForRedisMysqlRocketMqDownstreamAndRetryBacklog', () => {
  const validated = validateHardeningProfiles(config)

  assert.ok(validated.stopGates.includes('REDIS_ROLE_LATENCY_SUSTAINED'))
  assert.ok(validated.stopGates.includes('MYSQL_POOL_SATURATION'))
  assert.ok(validated.stopGates.includes('ROCKETMQ_BACKLOG_STARVED'))
  assert.ok(validated.stopGates.includes('DOWNSTREAM_BULKHEAD_OPEN'))
  assert.ok(validated.stopGates.includes('RETRY_BACKLOG_GROWING_AFTER_RECOVERY'))
  assert.equal(validated.blockedUntil, 'p1-004-accepted')
})

test('validateOnlyPrintsMachineReadableSummary', () => {
  const output = execFileSync('node', [
    'tools/perf/hardening-profile.mjs',
    '--profile-file',
    'tools/perf/4000-readiness-profiles.json',
    '--profile',
    'readiness-mixed-4000',
    '--validate-only',
    'true',
  ], { encoding: 'utf8' })
  const summary = JSON.parse(output)

  assert.equal(summary.profile, 'readiness-mixed-4000')
  assert.equal(summary.totalConcurrency, 4000)
  assert.equal(summary.blockedUntil, 'p1-004-accepted')
  assert.deepEqual(summary.laneBudgets, config.lanes)
  assert.ok(summary.stopGates.includes('DOWNSTREAM_BULKHEAD_OPEN'))
})
