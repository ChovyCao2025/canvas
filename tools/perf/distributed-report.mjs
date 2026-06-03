#!/usr/bin/env node

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const PLAN_VERSION = 1
const MODES = new Set(['event', 'direct', 'audience'])

function parseWorkerIds(workerIds) {
  const ids = Array.isArray(workerIds)
    ? workerIds
    : String(workerIds || '').split(',')
  const normalized = ids.map((id) => String(id).trim()).filter(Boolean)
  if (normalized.length === 0) {
    throw new Error('at least one worker id is required')
  }
  const seen = new Set()
  for (const workerId of normalized) {
    if (seen.has(workerId)) {
      throw new Error(`duplicate worker id ${workerId}`)
    }
    seen.add(workerId)
  }
  return normalized
}

function requirePositiveInteger(name, value) {
  if (!Number.isSafeInteger(value) || value < 1) {
    throw new Error(`${name} must be a positive integer`)
  }
  return value
}

function requireNonNegativeInteger(name, value) {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new Error(`${name} must be a non-negative integer`)
  }
  return value
}

function splitPositiveTotal(total, parts) {
  const base = Math.floor(total / parts)
  const remainder = total % parts
  return Array.from({ length: parts }, (_, index) => base + (index < remainder ? 1 : 0))
}

function validateMode(input) {
  const mode = input.mode || 'event'
  if (!MODES.has(mode)) {
    throw new Error('mode must be one of event, direct, audience')
  }
  if (mode === 'direct' && !input.canvasId) {
    throw new Error('canvasId is required for direct mode')
  }
  if (mode === 'audience' && !input.audienceId) {
    throw new Error('audienceId is required for audience mode')
  }
  return mode
}

export function buildDistributedPlan(input) {
  if (!input.perfRunId) {
    throw new Error('perfRunId is required')
  }
  const mode = validateMode(input)
  const workerIds = parseWorkerIds(input.workerIds)
  const totalCount = requirePositiveInteger('totalCount', Number(input.totalCount))
  const totalConcurrency = requirePositiveInteger('totalConcurrency', Number(input.totalConcurrency))
  if (totalConcurrency < workerIds.length) {
    throw new Error('totalConcurrency must be at least the worker count')
  }

  const counts = splitPositiveTotal(totalCount, workerIds.length)
  const concurrency = splitPositiveTotal(totalConcurrency, workerIds.length)
  let nextSeqStart = 1
  const workers = workerIds.map((workerId, index) => {
    const worker = {
      workerId,
      seqStart: nextSeqStart,
      count: counts[index],
      concurrency: concurrency[index],
    }
    nextSeqStart += worker.count
    return worker
  })

  return {
    version: PLAN_VERSION,
    kind: 'canvas-distributed-perf-plan',
    createdAt: input.createdAt || new Date().toISOString(),
    perfRunId: input.perfRunId,
    mode,
    baseUrl: input.baseUrl || 'http://localhost:8080',
    eventCode: input.eventCode || 'PERF_ORDER_PAID',
    canvasId: input.canvasId || '',
    audienceId: input.audienceId || '',
    eventSecretEnv: input.eventSecretEnv || 'PERF_EVENT_SECRET',
    matchedCanvasCount: Number(input.matchedCanvasCount || 1),
    reportType: input.reportType || 'capacity',
    minDurationMin: Number(input.minDurationMin || 30),
    accuracy: input.accuracy === true,
    totalCount,
    totalConcurrency,
    workerIds,
    workers,
  }
}

function workerConfig(plan, workerId) {
  const worker = plan.workers.find((candidate) => candidate.workerId === workerId)
  if (!worker) {
    throw new Error(`unknown worker id ${workerId}`)
  }
  return worker
}

export function workerRunnerArgs({ plan, workerId, summaryFile }) {
  const worker = workerConfig(plan, workerId)
  const args = [
    'tools/perf/perf-runner.mjs',
    '--mode', plan.mode,
    '--base-url', plan.baseUrl,
    '--perf-run-id', plan.perfRunId,
    '--count', String(worker.count),
    '--concurrency', String(worker.concurrency),
    '--worker-id', worker.workerId,
    '--seq-start', String(worker.seqStart),
  ]

  if (summaryFile) {
    args.push('--summary-file', summaryFile)
  }
  if (plan.mode === 'event') {
    args.push('--event-code', plan.eventCode)
    args.push('--event-secret-env', plan.eventSecretEnv || 'PERF_EVENT_SECRET')
  }
  if (plan.mode === 'direct') {
    args.push('--canvas-id', plan.canvasId)
    args.push('--event-secret-env', plan.eventSecretEnv || 'PERF_EVENT_SECRET')
  }
  if (plan.mode === 'audience') {
    args.push('--audience-id', plan.audienceId)
  }

  return args
}

function assertSummaryCounters(summary, worker) {
  requireNonNegativeInteger(`${worker.workerId}.sent`, summary.sent)
  requireNonNegativeInteger(`${worker.workerId}.success`, summary.success)
  requireNonNegativeInteger(`${worker.workerId}.failed`, summary.failed)
  if (summary.sent !== summary.success + summary.failed) {
    throw new Error(`worker ${worker.workerId} sent must equal success plus failed`)
  }
  if (summary.sent !== worker.count) {
    throw new Error(`worker ${worker.workerId} sent ${summary.sent} does not match planned count ${worker.count}`)
  }
}

function assertLatencyBuckets(summary, workerId) {
  if (!Array.isArray(summary.latencyBuckets) || summary.latencyBuckets.length === 0) {
    throw new Error(`worker ${workerId} missing latency bucket evidence`)
  }
  for (const [index, bucket] of summary.latencyBuckets.entries()) {
    if (!bucket || typeof bucket !== 'object') {
      throw new Error(`worker ${workerId} latency bucket ${index} must be an object`)
    }
    if (typeof bucket.leMs !== 'string' || bucket.leMs.length === 0) {
      throw new Error(`worker ${workerId} latency bucket ${index} missing leMs`)
    }
    requireNonNegativeInteger(`worker ${workerId} latency bucket ${index}.count`, bucket.count)
  }
}

export function validateWorkerSummaries(plan, summaries) {
  if (!plan || typeof plan !== 'object') {
    throw new Error('plan is required')
  }
  if (!Array.isArray(summaries)) {
    throw new Error('worker summaries must be an array')
  }
  const byWorkerId = new Map()
  for (const summary of summaries) {
    if (!summary.workerId) {
      throw new Error('worker summary missing workerId')
    }
    if (byWorkerId.has(summary.workerId)) {
      throw new Error(`duplicate worker summary ${summary.workerId}`)
    }
    byWorkerId.set(summary.workerId, summary)
  }

  for (const worker of plan.workers) {
    if (!byWorkerId.has(worker.workerId)) {
      throw new Error(`missing worker summary ${worker.workerId}`)
    }
  }

  const expectedWorkerIds = new Set(plan.workers.map((worker) => worker.workerId))
  for (const workerId of byWorkerId.keys()) {
    if (!expectedWorkerIds.has(workerId)) {
      throw new Error(`unexpected worker summary ${workerId}`)
    }
  }

  const orderedSummaries = plan.workers.map((worker) => {
    const summary = byWorkerId.get(worker.workerId)
    if (summary.perfRunId !== plan.perfRunId) {
      throw new Error(`worker ${worker.workerId} perfRunId ${summary.perfRunId || '(missing)'} does not match ${plan.perfRunId}`)
    }
    if (summary.mode && summary.mode !== plan.mode) {
      throw new Error(`worker ${worker.workerId} mode ${summary.mode} does not match ${plan.mode}`)
    }
    if (summary.seqStart !== worker.seqStart) {
      throw new Error(`worker ${worker.workerId} seqStart ${summary.seqStart} does not match planned ${worker.seqStart}`)
    }
    if (summary.seqCount !== worker.count) {
      throw new Error(`worker ${worker.workerId} seqCount ${summary.seqCount} does not match planned ${worker.count}`)
    }
    assertSummaryCounters(summary, worker)
    assertLatencyBuckets(summary, worker.workerId)
    return summary
  })

  let expectedSeqStart = 1
  for (const summary of orderedSummaries) {
    if (summary.seqStart > expectedSeqStart) {
      throw new Error(`sequence gap before worker ${summary.workerId}`)
    }
    if (summary.seqStart < expectedSeqStart) {
      throw new Error(`sequence overlap at worker ${summary.workerId}`)
    }
    expectedSeqStart += summary.seqCount
  }
  if (expectedSeqStart !== plan.totalCount + 1) {
    throw new Error(`sequence coverage ended at ${expectedSeqStart - 1}, expected ${plan.totalCount}`)
  }

  return orderedSummaries
}

export function aggregateLatencyBuckets(summaries) {
  if (!Array.isArray(summaries) || summaries.length === 0) {
    throw new Error('at least one worker summary is required')
  }
  const template = summaries[0].latencyBuckets
  if (!Array.isArray(template) || template.length === 0) {
    throw new Error(`worker ${summaries[0].workerId || '(unknown)'} missing latency bucket evidence`)
  }
  return template.map((bucket, index) => {
    const leMs = bucket.leMs
    let count = 0
    for (const summary of summaries) {
      const candidate = summary.latencyBuckets[index]
      if (!candidate || candidate.leMs !== leMs) {
        throw new Error(`worker ${summary.workerId} latency bucket boundaries do not match`)
      }
      count += candidate.count
    }
    return { leMs, count }
  })
}

function bucketUpperBound(bucket) {
  return bucket.leMs === 'Inf' ? Infinity : Number(bucket.leMs)
}

export function percentileFromBuckets(buckets, percentile) {
  if (!Array.isArray(buckets) || buckets.length === 0) {
    throw new Error('latency buckets are required')
  }
  const total = buckets.at(-1).count
  if (total === 0) {
    return 0
  }
  const threshold = Math.ceil(total * percentile)
  for (const bucket of buckets) {
    if (bucket.count >= threshold) {
      return bucketUpperBound(bucket)
    }
  }
  return bucketUpperBound(buckets.at(-1))
}

function parseTime(value, field) {
  const timestamp = Date.parse(value)
  if (!Number.isFinite(timestamp)) {
    throw new Error(`${field} must be an ISO timestamp`)
  }
  return timestamp
}

function weightedAverage(summaries) {
  const totalSent = summaries.reduce((sum, summary) => sum + summary.sent, 0)
  if (totalSent === 0) {
    return 0
  }
  const weighted = summaries.reduce((sum, summary) => {
    return sum + (summary.avgMs || 0) * summary.sent
  }, 0)
  return weighted / totalSent
}

export function aggregateWorkerSummaries(plan, summaries) {
  const orderedSummaries = validateWorkerSummaries(plan, summaries)
  const latencyBuckets = aggregateLatencyBuckets(orderedSummaries)
  const startedTimestamps = orderedSummaries.map((summary) => parseTime(summary.startedAt, `${summary.workerId}.startedAt`))
  const finishedTimestamps = orderedSummaries.map((summary) => parseTime(summary.finishedAt, `${summary.workerId}.finishedAt`))
  const startedMs = Math.min(...startedTimestamps)
  const finishedMs = Math.max(...finishedTimestamps)

  const sent = orderedSummaries.reduce((sum, summary) => sum + summary.sent, 0)
  const success = orderedSummaries.reduce((sum, summary) => sum + summary.success, 0)
  const failed = orderedSummaries.reduce((sum, summary) => sum + summary.failed, 0)

  return {
    perfRunId: plan.perfRunId,
    distributed: true,
    mode: plan.mode,
    workerCount: plan.workers.length,
    totalCount: plan.totalCount,
    totalConcurrency: plan.totalConcurrency,
    sent,
    success,
    failed,
    p95Ms: percentileFromBuckets(latencyBuckets, 0.95),
    p99Ms: percentileFromBuckets(latencyBuckets, 0.99),
    minMs: Math.min(...orderedSummaries.map((summary) => summary.minMs || 0)),
    maxMs: Math.max(...orderedSummaries.map((summary) => summary.maxMs || 0)),
    avgMs: weightedAverage(orderedSummaries),
    latencyBuckets,
    startedAt: new Date(startedMs).toISOString(),
    finishedAt: new Date(finishedMs).toISOString(),
    durationMs: finishedMs - startedMs,
    settings: {
      baseUrl: plan.baseUrl,
      eventCode: plan.eventCode,
      canvasId: plan.canvasId,
      audienceId: plan.audienceId,
      matchedCanvasCount: plan.matchedCanvasCount,
      reportType: plan.reportType,
      minDurationMin: plan.minDurationMin,
      accuracy: plan.accuracy === true,
    },
    workers: orderedSummaries.map((summary) => ({
      workerId: summary.workerId,
      seqStart: summary.seqStart,
      seqCount: summary.seqCount,
      sent: summary.sent,
      success: summary.success,
      failed: summary.failed,
      durationMs: summary.durationMs,
      p95Ms: summary.p95Ms,
      p99Ms: summary.p99Ms || 0,
      machine: summary.machine || {},
    })),
  }
}

export function assertDistributedReportable(summary, { reportType = 'capacity', minDurationMin = 30 } = {}) {
  requireNonNegativeInteger('distributed sent', summary.sent)
  requireNonNegativeInteger('distributed success', summary.success)
  requireNonNegativeInteger('distributed failed', summary.failed)
  if (summary.sent !== summary.success + summary.failed) {
    throw new Error('distributed sent must equal success plus failed')
  }
  if (summary.failed > 0) {
    throw new Error(`distributed runner reported ${summary.failed} failed request(s)`)
  }
  if (!Number.isFinite(summary.p95Ms) || !Number.isFinite(summary.p99Ms)) {
    throw new Error('distributed p95Ms and p99Ms must be finite')
  }
  if (reportType === 'capacity') {
    const requiredMs = minDurationMin * 60 * 1000
    if ((summary.durationMs || 0) < requiredMs) {
      throw new Error(`distributed duration ${summary.durationMs || 0}ms is below required ${requiredMs}ms`)
    }
  }
}

export function distributedDirectory(root, perfRunId) {
  return path.join(root || 'tmp/perf-distributed', perfRunId)
}

export function planFileFor(root, perfRunId) {
  return path.join(distributedDirectory(root, perfRunId), 'plan.json')
}

export function workerSummaryFileFor(directory, workerId) {
  return path.join(directory, 'workers', workerId, 'runner-summary.json')
}

export function readJson(filePath) {
  return JSON.parse(readFileSync(filePath, 'utf8'))
}

export function writeJson(filePath, value) {
  mkdirSync(path.dirname(filePath), { recursive: true })
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`)
}

export function loadWorkerSummaries(plan, directory) {
  return plan.workers.map((worker) => readJson(workerSummaryFileFor(directory, worker.workerId)))
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}
