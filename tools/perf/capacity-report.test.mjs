import test from 'node:test'
import assert from 'node:assert/strict'
import { estimateCapacity, parseCapacityArgs } from './capacity-report.mjs'

test('estimateCapacity picks the minimum bottleneck and applies safety factor', () => {
  const result = estimateCapacity({
    localStableQps: 1200,
    localAppCores: 8,
    prodAppCoresTotal: 32,
    writesPerEvent: 4,
    prodDbSafeWriteQps: 12000,
    redisOpsPerEvent: 3,
    prodRedisSafeOps: 30000,
    rocketmqCapacity: 7000,
    downstreamRateLimitPerSec: 5000,
    downstreamCallsPerEvent: 1,
    cpuEfficiencyFactor: 0.75,
    safetyFactor: 0.5,
  })

  assert.equal(result.rawCapacity, 3000)
  assert.equal(result.recommendedCapacity, 1500)
  assert.equal(result.alertThreshold, 1050)
  assert.equal(result.bottleneck, 'DB_WRITE')
})

test('estimateCapacity includes all bottleneck candidates', () => {
  const result = estimateCapacity({
    localStableQps: 1000,
    localAppCores: 4,
    prodAppCoresTotal: 8,
    writesPerEvent: 1,
    prodDbSafeWriteQps: 10000,
    redisOpsPerEvent: 1,
    prodRedisSafeOps: 9000,
    rocketmqCapacity: 8000,
    downstreamRateLimitPerSec: 7000,
    downstreamCallsPerEvent: 1,
    cpuEfficiencyFactor: 1,
    safetyFactor: 0.5,
  })

  assert.equal(result.bottleneck, 'APP_CPU')
  assert.deepEqual(result.candidates.map((candidate) => candidate.name), [
    'APP_CPU',
    'DOWNSTREAM_API',
    'ROCKETMQ',
    'REDIS_OPS',
    'DB_WRITE',
  ])
})

test('estimateCapacity calculates alert threshold from rounded recommendation', () => {
  const result = estimateCapacity({
    localStableQps: 100,
    localAppCores: 1,
    prodAppCoresTotal: 1,
    writesPerEvent: 2,
    prodDbSafeWriteQps: 10,
    redisOpsPerEvent: 1,
    prodRedisSafeOps: 100,
    rocketmqCapacity: 100,
    downstreamRateLimitPerSec: 100,
    downstreamCallsPerEvent: 1,
    cpuEfficiencyFactor: 1,
    safetyFactor: 0.3,
  })

  assert.equal(result.rawCapacity, 5)
  assert.equal(result.recommendedCapacity, 1)
  assert.equal(result.alertThreshold, 0)
})

test('estimateCapacity rejects zero divisor inputs', () => {
  assert.throws(() => estimateCapacity({
    localStableQps: 1200,
    localAppCores: 0,
    prodAppCoresTotal: 32,
    writesPerEvent: 4,
    prodDbSafeWriteQps: 12000,
    redisOpsPerEvent: 3,
    prodRedisSafeOps: 30000,
    rocketmqCapacity: 7000,
    downstreamRateLimitPerSec: 5000,
    downstreamCallsPerEvent: 1,
  }), /localAppCores must be a positive number/)
})

test('parseCapacityArgs applies default safety factors', () => {
  const args = parseCapacityArgs([
    '--local-stable-qps', '1200',
    '--local-app-cores', '8',
    '--prod-app-cores-total', '32',
    '--writes-per-event', '4',
    '--prod-db-safe-write-qps', '12000',
    '--redis-ops-per-event', '3',
    '--prod-redis-safe-ops', '30000',
    '--rocketmq-capacity', '7000',
    '--downstream-rate-limit-per-sec', '5000',
    '--downstream-calls-per-event', '1',
  ])

  assert.equal(args.cpuEfficiencyFactor, 0.75)
  assert.equal(args.safetyFactor, 0.5)
})

test('parseCapacityArgs rejects unknown flags', () => {
  assert.throws(() => parseCapacityArgs([
    '--local-stable-qps', '1200',
    '--unknown', '1',
  ]), /Unknown flag: --unknown/)
})
