#!/usr/bin/env node

import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  cpuEfficiencyFactor: 0.75,
  safetyFactor: 0.5,
  verifierVerdict: 'PASS',
}

const FLAG_NAMES = {
  '--local-stable-qps': 'localStableQps',
  '--local-app-cores': 'localAppCores',
  '--prod-app-cores-total': 'prodAppCoresTotal',
  '--writes-per-event': 'writesPerEvent',
  '--prod-db-safe-write-qps': 'prodDbSafeWriteQps',
  '--redis-ops-per-event': 'redisOpsPerEvent',
  '--prod-redis-safe-ops': 'prodRedisSafeOps',
  '--rocketmq-capacity': 'rocketmqCapacity',
  '--disruptor-worker-capacity': 'disruptorWorkerCapacity',
  '--downstream-rate-limit-per-sec': 'downstreamRateLimitPerSec',
  '--downstream-calls-per-event': 'downstreamCallsPerEvent',
  '--cpu-efficiency-factor': 'cpuEfficiencyFactor',
  '--safety-factor': 'safetyFactor',
  '--verifier-verdict': 'verifierVerdict',
}

const VERIFIER_VERDICTS = new Set(['PASS', 'PASS_WITH_EXPECTED_FAILURES', 'FAIL'])

const REQUIRED_POSITIVE_ARGS = [
  'localStableQps',
  'localAppCores',
  'prodAppCoresTotal',
  'writesPerEvent',
  'prodDbSafeWriteQps',
  'redisOpsPerEvent',
  'prodRedisSafeOps',
  'rocketmqCapacity',
  'disruptorWorkerCapacity',
  'downstreamRateLimitPerSec',
  'downstreamCallsPerEvent',
  'cpuEfficiencyFactor',
  'safetyFactor',
]

function parsePositiveNumber(flag, value) {
  if (!/^(0|[1-9]\d*)(\.\d+)?$/.test(value)) {
    throw new Error(`${flag} must be a positive number`)
  }

  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${flag} must be a positive number`)
  }

  return parsed
}

function requirePositive(input, name) {
  const value = input[name]
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive number`)
  }
  return value
}

function requireVerifierVerdict(input) {
  const verdict = input.verifierVerdict || DEFAULT_ARGS.verifierVerdict

  if (!VERIFIER_VERDICTS.has(verdict)) {
    throw new Error('verifierVerdict must be one of PASS, PASS_WITH_EXPECTED_FAILURES, FAIL')
  }

  if (verdict === 'FAIL') {
    throw new Error('Cannot estimate capacity when verifierVerdict is FAIL')
  }

  return verdict
}

export function estimateCapacity(input) {
  for (const name of REQUIRED_POSITIVE_ARGS) {
    requirePositive(input, name)
  }
  const verifierVerdict = requireVerifierVerdict(input)

  const appCpuCapacity = input.localStableQps
    * (input.prodAppCoresTotal / input.localAppCores)
    * input.cpuEfficiencyFactor
  const dbWriteCapacity = input.prodDbSafeWriteQps / input.writesPerEvent
  const redisOpsCapacity = input.prodRedisSafeOps / input.redisOpsPerEvent
  const downstreamCapacity = input.downstreamRateLimitPerSec / input.downstreamCallsPerEvent

  const candidates = [
    { name: 'APP_CPU', capacity: appCpuCapacity },
    { name: 'DB_WRITE', capacity: dbWriteCapacity },
    { name: 'REDIS_OPS', capacity: redisOpsCapacity },
    { name: 'ROCKETMQ', capacity: input.rocketmqCapacity },
    { name: 'DISRUPTOR_WORKER', capacity: input.disruptorWorkerCapacity },
    { name: 'DOWNSTREAM_API', capacity: downstreamCapacity },
  ].sort((left, right) => left.capacity - right.capacity)

  const bottleneck = candidates[0]
  const rawCapacity = Math.floor(bottleneck.capacity)
  const recommendedCapacity = Math.floor(rawCapacity * input.safetyFactor)

  return {
    verifierVerdict,
    bottleneck: bottleneck.name,
    rawCapacity,
    recommendedCapacity,
    alertThreshold: Math.floor(recommendedCapacity * 0.7),
    candidates: candidates.map((candidate) => ({
      name: candidate.name,
      capacity: Math.floor(candidate.capacity),
    })),
  }
}

export function parseCapacityArgs(argv) {
  const args = { ...DEFAULT_ARGS }

  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index]
    const name = FLAG_NAMES[flag]

    if (!name) {
      throw new Error(`Unknown flag: ${flag}`)
    }

    if (index + 1 >= argv.length || argv[index + 1] === '' || argv[index + 1].startsWith('--')) {
      throw new Error(`Missing value for ${flag}`)
    }

    if (name === 'verifierVerdict') {
      args[name] = argv[index + 1]
    } else {
      args[name] = parsePositiveNumber(flag, argv[index + 1])
    }
  }

  for (const name of REQUIRED_POSITIVE_ARGS) {
    requirePositive(args, name)
  }
  requireVerifierVerdict(args)

  return args
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

function main() {
  const result = estimateCapacity(parseCapacityArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  try {
    main()
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
