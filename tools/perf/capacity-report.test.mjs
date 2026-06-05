import test from 'node:test'
import assert from 'node:assert/strict'
import { estimateCapacity, evaluateHardeningGates, parseCapacityArgs } from './capacity-report.mjs'

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
    disruptorWorkerCapacity: 9000,
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
    disruptorWorkerCapacity: 8500,
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
    'DISRUPTOR_WORKER',
    'REDIS_OPS',
    'DB_WRITE',
  ])
})

test('estimateCapacity includes disruptor worker bottleneck', () => {
  const result = estimateCapacity({
    localStableQps: 1000,
    localAppCores: 4,
    prodAppCoresTotal: 8,
    writesPerEvent: 1,
    prodDbSafeWriteQps: 10000,
    redisOpsPerEvent: 1,
    prodRedisSafeOps: 9000,
    rocketmqCapacity: 8000,
    disruptorWorkerCapacity: 1200,
    downstreamRateLimitPerSec: 7000,
    downstreamCallsPerEvent: 1,
    cpuEfficiencyFactor: 1,
    safetyFactor: 0.5,
  })

  assert.equal(result.bottleneck, 'DISRUPTOR_WORKER')
  assert.equal(result.rawCapacity, 1200)
  assert.equal(result.recommendedCapacity, 600)
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
    disruptorWorkerCapacity: 100,
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
    disruptorWorkerCapacity: 9000,
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
    '--disruptor-worker-capacity', '9000',
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

test('evaluateHardeningGates passes healthy hardening samples', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 8,
    redisP99Ms: 20,
    mysqlActiveConnections: 40,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'PASS')
  assert.deepEqual(result.stopGates, [])
})

test('evaluateHardeningGates reports redis and mysql stop gates in stable order', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 25,
    redisP99Ms: 55,
    mysqlActiveConnections: 90,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 1200,
    normalMqBacklogGrowing: false,
    disruptorOverflowConsecutiveSamples: 0,
    retryBacklogGrowingAfterRecovery: false,
    dlqGrowingAfterRecovery: false,
    lightP95Ms: 500,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'STOP')
  assert.deepEqual(result.stopGates, [
    'REDIS_REGISTRY_LATENCY_SUSTAINED',
    'MYSQL_POOL_SATURATION',
    'MYSQL_SLOW_SQL',
  ])
})

test('evaluateHardeningGates reports queue, retry, dlq, and protected lane stop gates', () => {
  const result = evaluateHardeningGates({
    redisP95Ms: 8,
    redisP99Ms: 20,
    mysqlActiveConnections: 40,
    mysqlMaxConnections: 100,
    mysqlSlowSqlMs: 200,
    normalMqBacklogGrowing: true,
    disruptorOverflowConsecutiveSamples: 2,
    retryBacklogGrowingAfterRecovery: true,
    dlqGrowingAfterRecovery: true,
    lightP95Ms: 1100,
    standardP95Ms: 700,
  })

  assert.equal(result.verdict, 'STOP')
  assert.deepEqual(result.stopGates, [
    'NORMAL_MQ_BACKLOG_STARVED_BY_RETRY',
    'DISRUPTOR_OVERFLOW_GROWING',
    'RETRY_BACKLOG_GROWING_AFTER_RECOVERY',
    'DLQ_GROWING_AFTER_RECOVERY',
    'PROTECTED_LANE_LATENCY_VIOLATION',
  ])
})
