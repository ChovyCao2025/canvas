#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { createHmac } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  mode: 'event',
  baseUrl: 'http://localhost:8080',
  perfRunId: '',
  count: 1000,
  concurrency: 20,
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  audienceId: '',
  userPrefix: 'perf_user_',
  userModulo: 1000,
  duplicateRate: 0,
  eventSecretEnv: 'PERF_EVENT_SECRET',
  summaryFile: '',
  workerId: '',
  seqStart: 1,
}

const FLAG_NAMES = {
  '--mode': 'mode',
  '--base-url': 'baseUrl',
  '--perf-run-id': 'perfRunId',
  '--count': 'count',
  '--concurrency': 'concurrency',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--audience-id': 'audienceId',
  '--user-prefix': 'userPrefix',
  '--user-modulo': 'userModulo',
  '--duplicate-rate': 'duplicateRate',
  '--event-secret-env': 'eventSecretEnv',
  '--summary-file': 'summaryFile',
  '--worker-id': 'workerId',
  '--seq-start': 'seqStart',
}

const NUMBER_ARGS = new Set(['count', 'concurrency', 'userModulo', 'seqStart'])
const MODES = new Set(['event', 'direct', 'audience'])
export const LATENCY_BUCKET_UPPER_BOUNDS_MS = [
  1,
  2,
  5,
  10,
  20,
  50,
  100,
  200,
  500,
  1000,
  2000,
  5000,
  10000,
  30000,
  60000,
  'Inf',
]

function parseIntegerArg(flag, value, { allowZero }) {
  const pattern = allowZero ? /^(0|[1-9]\d*)$/ : /^[1-9]\d*$/

  if (!pattern.test(value)) {
    const kind = allowZero ? 'a non-negative integer' : 'a positive integer'
    throw new Error(`${flag} must be ${kind}`)
  }

  return Number(value)
}

function parseDuplicateRate(flag, value) {
  if (!/^(0|0?\.\d+)$/.test(value)) {
    throw new Error(`${flag} must be >= 0 and < 1`)
  }

  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0 || parsed >= 1) {
    throw new Error(`${flag} must be >= 0 and < 1`)
  }

  return parsed
}

function validateArgs(args) {
  if (!MODES.has(args.mode)) {
    throw new Error('--mode must be one of event, direct, audience')
  }

  if (!Number.isSafeInteger(args.count) || args.count < 0) {
    throw new Error('--count must be a non-negative integer')
  }

  if (!Number.isSafeInteger(args.concurrency) || args.concurrency < 1) {
    throw new Error('--concurrency must be a positive integer')
  }

  if (!Number.isSafeInteger(args.userModulo) || args.userModulo < 1) {
    throw new Error('--user-modulo must be a positive integer')
  }

  if (!Number.isSafeInteger(args.seqStart) || args.seqStart < 1) {
    throw new Error('--seq-start must be a positive integer')
  }

  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }

  if (args.mode === 'direct' && !args.canvasId) {
    throw new Error('--canvas-id is required for direct mode')
  }

  if (args.mode === 'audience' && !args.audienceId) {
    throw new Error('--audience-id is required for audience mode')
  }

  if (args.mode !== 'direct' && args.duplicateRate > 0) {
    throw new Error('--duplicate-rate is only supported for direct mode')
  }
}

export function parseRunnerArgs(argv) {
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

    const value = argv[index + 1]
    if (NUMBER_ARGS.has(name)) {
      args[name] = parseIntegerArg(flag, value, { allowZero: name === 'count' })
    } else if (name === 'duplicateRate') {
      args[name] = parseDuplicateRate(flag, value)
    } else {
      args[name] = value
    }
  }

  validateArgs(args)

  return args
}

export function buildEventPayload({
  perfRunId,
  eventCode,
  userPrefix,
  seq,
  userModulo,
}) {
  return {
    eventCode,
    userId: `${userPrefix}${seq % userModulo}`,
    attributes: {
      perfRunId,
      perfInputId: `${perfRunId}:event:${seq}`,
      seq,
      amount: seq % 1000,
    },
  }
}

export function buildDirectPayload({ perfRunId, userPrefix, seq, userModulo }) {
  return {
    userId: `${userPrefix}${seq % userModulo}`,
    idempotencyKey: `${perfRunId}:direct:${seq}`,
    inputParams: {
      perfRunId,
      perfInputId: `${perfRunId}:direct:${seq}`,
      seq,
    },
  }
}

export function duplicateCountFor(count, duplicateRate) {
  return Math.floor(count * duplicateRate)
}

export function logicalSeqForRequest(seq, count, duplicateCount, seqStart = 1) {
  const uniqueEnd = seqStart + count - duplicateCount - 1
  if (duplicateCount <= 0 || seq <= uniqueEnd) {
    return seq
  }

  return seqStart + (seq - uniqueEnd - 1)
}

export function* chunkSeq(count, concurrency, seqStart = 1) {
  const seqEnd = seqStart + count - 1
  for (let start = seqStart; start <= seqEnd; start += concurrency) {
    const chunk = []
    const end = Math.min(start + concurrency - 1, seqEnd)

    for (let seq = start; seq <= end; seq += 1) {
      chunk.push(seq)
    }

    yield chunk
  }
}

function buildRequest(args, seq) {
  if (args.mode === 'event') {
    return {
      url: `${args.baseUrl}/canvas/events/report`,
      body: buildEventPayload({
        perfRunId: args.perfRunId,
        eventCode: args.eventCode,
        userPrefix: args.userPrefix,
        seq,
        userModulo: args.userModulo,
      }),
    }
  }

  if (args.mode === 'direct') {
    if (!args.canvasId) {
      throw new Error('--canvas-id is required for direct mode')
    }

    const logicalSeq = logicalSeqForRequest(seq, args.count, args.duplicateCount || 0, args.seqStart || 1)

    return {
      url: `${args.baseUrl}/canvas/execute/direct/${args.canvasId}`,
      body: buildDirectPayload({
        perfRunId: args.perfRunId,
        userPrefix: args.userPrefix,
        seq: logicalSeq,
        userModulo: args.userModulo,
      }),
    }
  }

  if (args.mode === 'audience') {
    if (!args.audienceId) {
      throw new Error('--audience-id is required for audience mode')
    }

    return {
      url: `${args.baseUrl}/canvas/audiences/${args.audienceId}/compute`,
      body: {
        perfRunId: args.perfRunId,
        perfInputId: `${args.perfRunId}:audience:${seq}`,
      },
    }
  }

  throw new Error(`Unsupported mode: ${args.mode}`)
}

function safeSpawnOutput(command, args) {
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    timeout: 3000,
  })

  if (result.error) {
    return ''
  }

  return `${result.stdout || ''}${result.stderr || ''}`.trim()
}

function javaVersion() {
  return safeSpawnOutput('java', ['-version']).split(/\r?\n/).at(0) || ''
}

function machineMetadata() {
  const cpus = os.cpus()
  const cpuModels = [...new Set(cpus.map((cpu) => cpu.model).filter(Boolean))]
  const cpuSpeedsMhz = [...new Set(cpus.map((cpu) => cpu.speed).filter((speed) => speed > 0))]

  return {
    hostname: os.hostname(),
    platform: os.platform(),
    arch: os.arch(),
    cpuCount: cpus.length,
    cpuModels,
    cpuSpeedsMhz,
    totalMemBytes: os.totalmem(),
    nodeVersion: process.version,
    nodeExecArgv: process.execArgv,
    javaVersion: javaVersion(),
    javaHome: process.env.JAVA_HOME || '',
    javaToolOptions: process.env.JAVA_TOOL_OPTIONS || '',
    jdkJavaOptions: process.env.JDK_JAVA_OPTIONS || '',
    mavenOpts: process.env.MAVEN_OPTS || '',
  }
}

export function buildEventSignatureHeaders({ secret, timestamp, rawBody }) {
  const signature = createHmac('sha256', secret)
    .update(`${timestamp}\n${rawBody}`)
    .digest('hex')

  return {
    'X-Canvas-Timestamp': String(timestamp),
    'X-Canvas-Signature': `sha256=${signature}`,
  }
}

export function resolveEventSecret(args, env = process.env) {
  if (args.mode !== 'event' && args.mode !== 'direct') {
    return { value: '', source: 'none' }
  }

  const envName = args.eventSecretEnv || 'PERF_EVENT_SECRET'
  const envValue = env[envName] || ''
  if (envValue) {
    return { value: envValue, source: `env:${envName}` }
  }

  return { value: '', source: 'none' }
}

export function buildSignedHeaders({
  args,
  rawBody,
  nowMs = () => Date.now(),
  env = process.env,
}) {
  const headers = {
    'content-type': 'application/json',
  }
  const secret = resolveEventSecret(args, env)

  if (!secret.value) {
    return headers
  }

  return {
    ...headers,
    ...buildEventSignatureHeaders({
      secret: secret.value,
      timestamp: String(nowMs()),
      rawBody,
    }),
  }
}

async function sendRequest(args, seq, { performanceNow, nowMs = () => Date.now(), env = process.env }) {
  const request = buildRequest(args, seq)
  const rawBody = JSON.stringify(request.body)
  const startedAt = performanceNow()

  try {
    const response = await fetch(request.url, {
      method: 'POST',
      headers: buildSignedHeaders({ args, rawBody, nowMs, env }),
      body: rawBody,
    })

    return {
      ok: response.ok,
      durationMs: performanceNow() - startedAt,
    }
  } catch (error) {
    return {
      ok: false,
      durationMs: performanceNow() - startedAt,
      error,
    }
  }
}

function summarySettings(args, env = process.env) {
  const eventSecret = resolveEventSecret(args, env)

  return {
    mode: args.mode,
    baseUrl: args.baseUrl,
    count: args.count,
    concurrency: args.concurrency,
    workerId: args.workerId || '',
    seqStart: args.seqStart || 1,
    seqCount: args.count,
    eventCode: args.eventCode,
    canvasId: args.canvasId,
    audienceId: args.audienceId,
    userPrefix: args.userPrefix,
    userModulo: args.userModulo,
    duplicateRate: args.duplicateRate || 0,
    duplicateCount: args.duplicateCount || 0,
    eventSignature: {
      enabled: Boolean(eventSecret.value),
      source: eventSecret.source,
    },
  }
}

function latencyBucketLabel(bound) {
  return bound === 'Inf' ? 'Inf' : String(bound)
}

export function latencyBucketsForDurations(durations) {
  return LATENCY_BUCKET_UPPER_BOUNDS_MS.map((upperBoundMs) => ({
    leMs: latencyBucketLabel(upperBoundMs),
    count: durations.filter((duration) => (
      upperBoundMs === 'Inf' || duration <= upperBoundMs
    )).length,
  }))
}

function percentileForSortedDurations(durations, percentile) {
  const index = Math.min(
    durations.length - 1,
    Math.max(0, Math.ceil(durations.length * percentile) - 1),
  )
  return durations.length === 0 ? 0 : durations[index]
}

function averageDuration(durations) {
  if (durations.length === 0) {
    return 0
  }
  return durations.reduce((sum, duration) => sum + duration, 0) / durations.length
}

export async function run(args, deps = {}) {
  const now = deps.now || (() => new Date().toISOString())
  const nowMs = deps.nowMs || (() => Date.now())
  const env = deps.env || process.env
  const performanceNow = deps.performanceNow || (() => performance.now())
  const getMachineMetadata = deps.machineMetadata || machineMetadata
  const duplicateCount = args.mode === 'direct'
    ? duplicateCountFor(args.count, args.duplicateRate || 0)
    : 0
  const runArgs = {
    ...args,
    duplicateCount,
  }
  const startedAt = now()
  const startedPerf = performanceNow()
  let sent = 0
  let success = 0
  let failed = 0
  const durations = []

  for (const chunk of chunkSeq(runArgs.count, runArgs.concurrency, runArgs.seqStart || 1)) {
    const results = await Promise.all(
      chunk.map(async (seq) => {
        sent += 1
        return sendRequest(runArgs, seq, { performanceNow, nowMs, env })
      }),
    )

    for (const result of results) {
      durations.push(result.durationMs)

      if (result.ok) {
        success += 1
      } else {
        failed += 1
      }
    }
  }

  durations.sort((left, right) => left - right)
  const p95Ms = percentileForSortedDurations(durations, 0.95)
  const p99Ms = percentileForSortedDurations(durations, 0.99)
  const minMs = durations.length === 0 ? 0 : durations[0]
  const maxMs = durations.length === 0 ? 0 : durations[durations.length - 1]
  const avgMs = averageDuration(durations)
  const finishedAt = now()
  const durationMs = performanceNow() - startedPerf

  return {
    perfRunId: runArgs.perfRunId,
    workerId: runArgs.workerId || '',
    mode: runArgs.mode,
    seqStart: runArgs.seqStart || 1,
    seqCount: runArgs.count,
    sent,
    success,
    failed,
    p95Ms,
    p99Ms,
    minMs,
    maxMs,
    avgMs,
    latencyBuckets: latencyBucketsForDurations(durations),
    startedAt,
    finishedAt,
    durationMs,
    settings: summarySettings(runArgs, env),
    machine: getMachineMetadata(),
  }
}

export function exitCodeForSummary(summary) {
  return summary.failed > 0 ? 2 : 0
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

function writeSummaryFile(summaryFile, summary) {
  const directory = path.dirname(summaryFile)
  if (directory && directory !== '.') {
    mkdirSync(directory, { recursive: true })
  }
  writeFileSync(summaryFile, `${JSON.stringify(summary, null, 2)}\n`)
}

async function main() {
  const args = parseRunnerArgs(process.argv.slice(2))
  const summary = await run(args)

  if (args.summaryFile) {
    writeSummaryFile(args.summaryFile, summary)
  }

  console.log(JSON.stringify(summary, null, 2))
  process.exitCode = exitCodeForSummary(summary)
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
