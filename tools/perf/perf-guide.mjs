#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { computeVerdict } from './verifier.mjs'
import { createPerfFixtures, engineAccuracyTraceVerifierArgs } from './fixture.mjs'
import {
  aggregateWorkerSummaries,
  assertDistributedReportable,
  buildDistributedPlan,
  distributedDirectory,
  loadWorkerSummaries,
  planFileFor,
  readJson as readDistributedJson,
  workerRunnerArgs,
  workerSummaryFileFor,
  writeJson as writeDistributedJson,
} from './distributed-report.mjs'

const COMMANDS = new Set([
  'doctor',
  'fixture',
  'smoke',
  'threshold',
  'soak',
  'accuracy',
  'report',
  'cleanup',
  'distributed-plan',
  'distributed-worker',
  'distributed-report',
])

const DEFAULTS = {
  baseUrl: 'http://localhost:8080',
  runRoot: 'tmp/perf-runs',
  thresholdRoot: 'tmp/perf-threshold',
  perfRunId: '',
  mode: 'event',
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  mqTopic: 'CANVAS_MQ_TRIGGER',
  mqTag: 'PERF_MQ',
  matchedCanvasCount: 1,
  eventSecretEnv: 'PERF_EVENT_SECRET',
  mysql: 'mysql',
  database: 'canvas_db',
  scope: 'ledger',
  execute: false,
  rebuild: false,
  reportType: 'capacity',
  minDurationMin: 30,
  count: 300000,
  concurrency: 100,
  wiremockUrl: 'http://localhost:8099',
  distributedRoot: 'tmp/perf-distributed',
  workerIds: '',
  workerId: '',
  totalCount: 300000,
  totalConcurrency: 100,
  planFile: '',
  evidenceDir: '',
  accuracy: false,
}

const FLAG_NAMES = {
  '--base-url': 'baseUrl',
  '--run-root': 'runRoot',
  '--threshold-root': 'thresholdRoot',
  '--perf-run-id': 'perfRunId',
  '--mode': 'mode',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--mq-topic': 'mqTopic',
  '--mq-tag': 'mqTag',
  '--matched-canvas-count': 'matchedCanvasCount',
  '--event-secret-env': 'eventSecretEnv',
  '--mysql': 'mysql',
  '--database': 'database',
  '--scope': 'scope',
  '--execute': 'execute',
  '--rebuild': 'rebuild',
  '--report-type': 'reportType',
  '--min-duration-min': 'minDurationMin',
  '--count': 'count',
  '--concurrency': 'concurrency',
  '--wiremock-url': 'wiremockUrl',
  '--distributed-root': 'distributedRoot',
  '--worker-ids': 'workerIds',
  '--worker-id': 'workerId',
  '--total-count': 'totalCount',
  '--total-concurrency': 'totalConcurrency',
  '--plan-file': 'planFile',
  '--evidence-dir': 'evidenceDir',
  '--accuracy': 'accuracy',
}

const NUMBER_FLAGS = new Set(['matchedCanvasCount', 'minDurationMin', 'count', 'concurrency', 'totalCount', 'totalConcurrency'])
const BOOLEAN_FLAGS = new Set(['execute', 'rebuild', 'accuracy'])

function parseBoolean(flag, value) {
  if (value === 'true') return true
  if (value === 'false') return false
  throw new Error(`${flag} must be true or false`)
}

function parsePositiveInteger(flag, value) {
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error(`${flag} must be a positive integer`)
  }
  return Number(value)
}

export function parseGuideArgs(argv) {
  const [command, ...rest] = argv
  if (!COMMANDS.has(command)) {
    throw new Error(`command must be one of ${[...COMMANDS].join(', ')}`)
  }

  const args = { ...DEFAULTS, command }
  for (let index = 0; index < rest.length; index += 2) {
    const flag = rest[index]
    const name = FLAG_NAMES[flag]
    if (!name) {
      throw new Error(`Unknown flag: ${flag}`)
    }
    if (index + 1 >= rest.length || rest[index + 1] === '' || rest[index + 1].startsWith('--')) {
      throw new Error(`Missing value for ${flag}`)
    }
    const value = rest[index + 1]
    if (NUMBER_FLAGS.has(name)) {
      args[name] = parsePositiveInteger(flag, value)
    } else if (BOOLEAN_FLAGS.has(name)) {
      args[name] = parseBoolean(flag, value)
    } else {
      args[name] = value
    }
  }

  if (!['ledger', 'all'].includes(args.scope)) {
    throw new Error('--scope must be ledger or all')
  }
  if (!['capacity', 'fault'].includes(args.reportType)) {
    throw new Error('--report-type must be capacity or fault')
  }
  return args
}

export function runDirectory(config, perfRunId = config.perfRunId) {
  if (!perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  return path.join(config.runRoot, perfRunId)
}

export function writeJson(filePath, value) {
  mkdirSync(path.dirname(filePath), { recursive: true })
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`)
}

export function readJson(filePath) {
  return JSON.parse(readFileSync(filePath, 'utf8'))
}

export function assertCapacityReportable(verifier, { reportType }) {
  if (verifier.verdict === 'FAIL') {
    throw new Error('verifier verdict FAIL cannot be used for capacity reporting')
  }
  if (reportType === 'capacity' && verifier.verdict === 'PASS_WITH_EXPECTED_FAILURES') {
    throw new Error('PASS_WITH_EXPECTED_FAILURES is only allowed for fault reports')
  }
  if (verifier.verdict !== 'PASS' && verifier.verdict !== 'PASS_WITH_EXPECTED_FAILURES') {
    throw new Error(`unknown verifier verdict ${verifier.verdict}`)
  }
}

function requireNumericEvidence(value, field) {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) {
    throw new Error(`runner summary field ${field} must be a non-negative integer`)
  }
  return value
}

export function assertRunnerSummaryComplete(summary, { perfRunId = '' } = {}) {
  for (const field of ['sent', 'success', 'failed']) {
    requireNumericEvidence(summary[field], field)
    if (!Number.isSafeInteger(summary[field])) {
      throw new Error(`runner summary field ${field} must be a non-negative integer`)
    }
  }
  if (typeof summary.durationMs !== 'number' || !Number.isFinite(summary.durationMs) || summary.durationMs < 0) {
    throw new Error('runner summary field durationMs must be a non-negative number')
  }
  if (perfRunId && summary.perfRunId !== perfRunId) {
    throw new Error(`runner summary perfRunId ${summary.perfRunId || '(missing)'} does not match ${perfRunId}`)
  }
  if (summary.sent !== summary.success + summary.failed) {
    throw new Error('runner summary sent must equal success plus failed')
  }
}

const VERIFIER_COUNT_FIELDS = [
  'planned',
  'sentSuccess',
  'accepted',
  'expectedExecutions',
  'actualExecutions',
  'success',
  'failedWithRecord',
  'dlq',
  'rejectedWithRecord',
  'retryPending',
  'duplicateExecution',
  'intentionalDuplicates',
  'expectedFailedWithRecord',
  'expectedRejectedWithRecord',
  'expectedDlq',
  'unexpectedFailedWithRecord',
  'unexpectedRejectedWithRecord',
  'unexpectedDlq',
  'deduplicated',
  'unexpectedDedup',
  'ackWithoutLedger',
  'unexpectedLoss',
  'wrongAudienceCount',
  'traceMismatch',
  'traceFailed',
  'traceDuplicateSuccess',
  'traceBufferPending',
]

const LEDGER_COUNT_FIELDS = [
  'eventLog',
  'requestRows',
  'requestSources',
  'executions',
  'success',
  'failed',
  'dlq',
  'retryPending',
  'rejectedWithRecord',
  'duplicateInput',
  'audienceRuns',
]

function requireVerifierCount(value, field) {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0 || !Number.isSafeInteger(value)) {
    throw new Error(`verifier evidence field ${field} must be a non-negative integer`)
  }
}

function assertTraceCountEntry(entry, field) {
  if (!entry || typeof entry !== 'object' || Array.isArray(entry)) {
    throw new Error(`verifier evidence field ${field} must be an object`)
  }
  if (typeof entry.nodeId !== 'string' || entry.nodeId.length === 0) {
    throw new Error(`verifier evidence field ${field}.nodeId must be a non-empty string`)
  }
  requireVerifierCount(entry.status, `${field}.status`)
  if (typeof entry.statusName !== 'string' || entry.statusName.length === 0) {
    throw new Error(`verifier evidence field ${field}.statusName must be a non-empty string`)
  }
  requireVerifierCount(entry.expected, `${field}.expected`)
  requireVerifierCount(entry.actual, `${field}.actual`)
}

function traceCountKey(entry) {
  return [
    entry.nodeId,
    entry.status,
    entry.statusName,
    entry.expected,
    entry.actual,
  ].join('\u0000')
}

function assertTraceEvidenceComplete(verifier) {
  if (typeof verifier.traceEnabled !== 'boolean') {
    throw new Error('verifier evidence field traceEnabled must be a boolean')
  }
  if (!Array.isArray(verifier.traceCounts)) {
    throw new Error('verifier evidence field traceCounts must be an array')
  }
  if (!Array.isArray(verifier.traceMismatches)) {
    throw new Error('verifier evidence field traceMismatches must be an array')
  }
  if (!['PASS', 'FAIL'].includes(verifier.traceVerdict)) {
    throw new Error('verifier evidence field traceVerdict must be PASS or FAIL')
  }

  verifier.traceCounts.forEach((entry, index) => {
    assertTraceCountEntry(entry, `traceCounts.${index}`)
  })
  verifier.traceMismatches.forEach((entry, index) => {
    assertTraceCountEntry(entry, `traceMismatches.${index}`)
  })

  if (!verifier.traceEnabled && verifier.traceCounts.length > 0) {
    throw new Error('verifier traceCounts must be empty when traceEnabled is false')
  }
  if (verifier.traceEnabled && verifier.traceCounts.length === 0) {
    throw new Error('verifier traceCounts must include at least one expectation when traceEnabled is true')
  }

  const derivedMismatches = verifier.traceCounts.filter((entry) => entry.actual !== entry.expected)
  if (verifier.traceMismatch !== derivedMismatches.length) {
    throw new Error(`verifier traceMismatch ${verifier.traceMismatch} does not match traceCounts mismatches ${derivedMismatches.length}`)
  }

  const expectedMismatchKeys = new Set(derivedMismatches.map(traceCountKey))
  if (verifier.traceMismatches.length !== derivedMismatches.length) {
    throw new Error(`verifier traceMismatches length ${verifier.traceMismatches.length} does not match traceMismatch ${derivedMismatches.length}`)
  }
  for (const mismatch of verifier.traceMismatches) {
    if (!expectedMismatchKeys.has(traceCountKey(mismatch))) {
      throw new Error('verifier traceMismatches must match mismatched traceCounts')
    }
  }

  const expectedTraceVerdict = [
    verifier.traceMismatch,
    verifier.traceFailed,
    verifier.traceDuplicateSuccess,
    verifier.traceBufferPending,
  ].some((value) => value > 0) ? 'FAIL' : 'PASS'
  if (verifier.traceVerdict !== expectedTraceVerdict) {
    throw new Error(`verifier traceVerdict ${verifier.traceVerdict} does not match recomputed ${expectedTraceVerdict}`)
  }
}

export function assertVerifierEvidenceComplete(verifier, { perfRunId, sentSuccess }) {
  if (verifier.perfRunId !== perfRunId) {
    throw new Error(`verifier perfRunId ${verifier.perfRunId || '(missing)'} does not match ${perfRunId}`)
  }
  if (verifier.sentSuccess !== sentSuccess) {
    throw new Error(`verifier sentSuccess ${verifier.sentSuccess ?? '(missing)'} does not match runner success ${sentSuccess}`)
  }
  for (const field of VERIFIER_COUNT_FIELDS) {
    requireVerifierCount(verifier[field], field)
  }
  assertTraceEvidenceComplete(verifier)
  if (!verifier.ledgerCounts || typeof verifier.ledgerCounts !== 'object' || Array.isArray(verifier.ledgerCounts)) {
    throw new Error('verifier evidence field ledgerCounts must be an object')
  }
  for (const field of LEDGER_COUNT_FIELDS) {
    requireVerifierCount(verifier.ledgerCounts[field], `ledgerCounts.${field}`)
  }
  if (verifier.planned !== sentSuccess) {
    throw new Error(`verifier planned ${verifier.planned} does not match runner success ${sentSuccess}`)
  }
  for (const [field, ledgerField] of [
    ['actualExecutions', 'executions'],
    ['success', 'success'],
    ['failedWithRecord', 'failed'],
    ['dlq', 'dlq'],
    ['rejectedWithRecord', 'rejectedWithRecord'],
    ['retryPending', 'retryPending'],
  ]) {
    if (verifier[field] !== verifier.ledgerCounts[ledgerField]) {
      throw new Error(`verifier ${field} ${verifier[field]} does not match ledgerCounts.${ledgerField} ${verifier.ledgerCounts[ledgerField]}`)
    }
  }
  const rawVerifierEvidence = { ...verifier }
  for (const field of [
    'ackWithoutLedger',
    'deduplicated',
    'duplicateExecution',
    'unexpectedDedup',
    'unexpectedDlq',
    'unexpectedFailedWithRecord',
    'unexpectedLoss',
    'unexpectedRejectedWithRecord',
    'verdict',
  ]) {
    delete rawVerifierEvidence[field]
  }
  const recomputed = computeVerdict({
    ...rawVerifierEvidence,
    duplicateExecution: verifier.ledgerCounts.duplicateInput,
  })
  for (const field of [
    'duplicateExecution',
    'deduplicated',
    'unexpectedDedup',
    'ackWithoutLedger',
    'unexpectedFailedWithRecord',
    'unexpectedRejectedWithRecord',
    'unexpectedDlq',
    'unexpectedLoss',
    'wrongAudienceCount',
    'traceMismatch',
    'traceFailed',
    'traceDuplicateSuccess',
    'traceBufferPending',
  ]) {
    if (verifier[field] !== recomputed[field]) {
      throw new Error(`verifier ${field} ${verifier[field]} does not match recomputed ${recomputed[field]}`)
    }
  }
  if (verifier.verdict !== recomputed.verdict) {
    throw new Error(`verifier verdict ${verifier.verdict} does not match recomputed ${recomputed.verdict}`)
  }
}

export function assertRunnerReportable(summary, { reportType, minDurationMin, perfRunId = '' }) {
  assertRunnerSummaryComplete(summary, { perfRunId })
  if (failedRequestCount(summary) > 0) {
    throw new Error(`runner reported ${failedRequestCount(summary)} failed request(s); run cannot be reported`)
  }
  if (reportType === 'capacity') {
    const requiredMs = minDurationMin * 60 * 1000
    if ((summary.durationMs || 0) < requiredMs) {
      throw new Error(`runner duration ${summary.durationMs || 0}ms is below required ${requiredMs}ms`)
    }
  }
}

export function commandForCleanup(config) {
  return [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', config.perfRunId,
      '--scope', config.scope || 'ledger',
      '--execute', String(config.execute === true),
      '--mysql', config.mysql || 'mysql',
      '--database', config.database || 'canvas_db',
    ],
  ]
}

export function runCommand(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || process.cwd(),
    encoding: 'utf8',
    timeout: options.timeout,
  })
  if (result.error) {
    throw result.error
  }
  return result
}

export function parseJsonCommandResult(result, commandName) {
  if (result.status !== 0 && result.status !== 2) {
    throw new Error(`${commandName} failed: ${result.stderr || result.stdout}`)
  }
  try {
    return JSON.parse(result.stdout)
  } catch (error) {
    throw new Error(`${commandName} did not output JSON: ${error.message}`)
  }
}

export async function defaultRunScenario(config, { mode, count, concurrency, perfRunId }, deps = {}) {
  const run = deps.runCommand || runCommand
  const scenarioPerfRunId = perfRunId || (config.perfRunId ? `${config.perfRunId}_${mode}` : '')
  const directory = runDirectory(config, scenarioPerfRunId)
  mkdirSync(directory, { recursive: true })
  const summaryFile = path.join(directory, 'runner-summary.json')
  const verifierFile = path.join(directory, 'verifier.json')
  rmSync(verifierFile, { force: true })
  const runnerArgs = [
    'tools/perf/perf-runner.mjs',
    '--mode', mode,
    '--base-url', config.baseUrl,
    '--perf-run-id', scenarioPerfRunId,
    '--count', String(count),
    '--concurrency', String(concurrency),
    '--summary-file', summaryFile,
  ]

  if (mode === 'direct') {
    runnerArgs.push('--canvas-id', config.canvasId)
    runnerArgs.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }
  if (mode === 'event') {
    runnerArgs.push('--event-code', config.eventCode)
    runnerArgs.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }

  const runner = parseJsonCommandResult(
    run(process.execPath, runnerArgs),
    'perf-runner',
  )
  const verifierArgs = [
    'tools/perf/verifier.mjs',
    '--mysql', config.mysql,
    '--database', config.database,
    '--mode', mode,
    '--perf-run-id', scenarioPerfRunId,
    '--sent-success', String(runner.success),
    '--matched-canvas-count', String(config.matchedCanvasCount),
  ]
  const verifier = parseJsonCommandResult(
    run(process.execPath, verifierArgs),
    'verifier',
  )
  writeJson(verifierFile, {
    ...verifier,
    perfRunId: scenarioPerfRunId,
    sentSuccess: runner.success,
  })
  return { summary: runner, verifier, directory, perfRunId: scenarioPerfRunId }
}

function failedRequestCount(summary) {
  return Number(summary.failed || 0)
}

function distributedPlanPath(config) {
  if (config.planFile) {
    return config.planFile
  }
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  return planFileFor(config.distributedRoot, config.perfRunId)
}

function distributedEvidenceDirectory(config, plan) {
  if (config.evidenceDir) {
    return config.evidenceDir
  }
  if (config.planFile) {
    return path.dirname(config.planFile)
  }
  return distributedDirectory(config.distributedRoot, plan.perfRunId)
}

async function defaultDoctor() {
  return { status: 'PASS' }
}

async function defaultFixture(config, deps = {}) {
  return createPerfFixtures(config, {
    client: deps.fixtureClient,
    env: deps.env,
  })
}

async function defaultSmoke(config, deps = {}) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for smoke')
  }
  if (!config.canvasId) {
    throw new Error('--canvas-id is required for direct smoke')
  }
  const runScenario = deps.runScenario || ((options) => defaultRunScenario(config, options, deps))
  const modes = [
    { mode: 'direct', count: 50, concurrency: 5 },
    { mode: 'event', count: 100, concurrency: 10 },
  ]
  const runs = []
  for (const scenario of modes) {
    const run = await runScenario(scenario)
    runs.push({ mode: scenario.mode, verifier: run.verifier, summary: run.summary })
    assertRunnerSummaryComplete(run.summary)
    if (failedRequestCount(run.summary) > 0) {
      return {
        status: 'FAIL',
        failedMode: scenario.mode,
        reason: `runner reported ${failedRequestCount(run.summary)} failed request(s)`,
        runs,
      }
    }
    if (run.verifier.verdict !== 'PASS') {
      return { status: 'FAIL', failedMode: scenario.mode, runs }
    }
  }
  return { status: 'PASS', runs }
}

async function defaultThreshold(config, deps = {}) {
  const run = deps.runCommand || runCommand
  const args = [
    'tools/perf/threshold-runner.mjs',
    '--mode', config.mode,
    '--base-url', config.baseUrl,
    '--matched-canvas-count', String(config.matchedCanvasCount),
    '--mysql', config.mysql,
    '--database', config.database,
    '--out-dir', config.thresholdRoot,
  ]
  if (config.mode === 'event') {
    args.push('--event-code', config.eventCode)
    args.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }
  if (config.mode === 'direct') {
    args.push('--canvas-id', config.canvasId)
    args.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }
  const result = parseJsonCommandResult(run(process.execPath, args), 'threshold-runner')
  const statuses = new Map([
    ['MAX_STAGE_STABLE', 'PASS'],
    ['THRESHOLD_FOUND', 'PASS'],
    ['NO_STABLE_STAGE', 'FAIL'],
  ])
  if (!statuses.has(result.verdict)) {
    throw new Error(`unknown threshold verdict ${result.verdict}`)
  }
  return {
    ...result,
    status: statuses.get(result.verdict),
  }
}

async function defaultSoak(config, deps = {}) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for soak')
  }
  const runScenario = deps.runScenario || ((options) => defaultRunScenario(config, options, deps))
  const run = await runScenario({
    mode: config.mode,
    perfRunId: config.perfRunId,
    count: config.count,
    concurrency: config.concurrency,
  })
  assertRunnerSummaryComplete(run.summary, { perfRunId: config.perfRunId })
  if (failedRequestCount(run.summary) > 0) {
    return {
      status: 'FAIL',
      reason: `runner reported ${failedRequestCount(run.summary)} failed request(s)`,
      summary: run.summary,
      verifier: run.verifier,
    }
  }
  const requiredMs = config.minDurationMin * 60 * 1000
  if ((run.summary.durationMs || 0) < requiredMs) {
    return {
      status: 'FAIL',
      reason: `duration ${run.summary.durationMs || 0}ms is below required ${requiredMs}ms`,
      summary: run.summary,
      verifier: run.verifier,
    }
  }
  if (run.verifier.verdict !== 'PASS') {
    return {
      status: 'FAIL',
      reason: `verifier verdict ${run.verifier.verdict}`,
      summary: run.summary,
      verifier: run.verifier,
    }
  }
  return { status: 'PASS', summary: run.summary, verifier: run.verifier }
}

async function defaultAccuracy(config, deps = {}) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for accuracy')
  }
  if (!config.canvasId) {
    throw new Error('--canvas-id is required for accuracy')
  }
  const run = deps.runCommand || runCommand
  const directory = runDirectory(config)
  mkdirSync(directory, { recursive: true })
  const summaryFile = path.join(directory, 'runner-summary.json')
  const verifierFile = path.join(directory, 'verifier.json')
  const sideEffectFile = path.join(directory, 'side-effect-verifier.json')

  const runnerArgs = [
    'tools/perf/perf-runner.mjs',
    '--mode', 'direct',
    '--base-url', config.baseUrl,
    '--perf-run-id', config.perfRunId,
    '--canvas-id', config.canvasId,
    '--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET',
    '--count', String(config.count),
    '--concurrency', String(config.concurrency),
    '--summary-file', summaryFile,
  ]
  const summary = parseJsonCommandResult(run(process.execPath, runnerArgs), 'perf-runner')
  writeJson(summaryFile, summary)
  assertRunnerSummaryComplete(summary, { perfRunId: config.perfRunId })

  const verifierArgs = [
    'tools/perf/verifier.mjs',
    '--mysql', config.mysql,
    '--database', config.database,
    '--mode', 'direct',
    '--perf-run-id', config.perfRunId,
    '--sent-success', String(summary.success),
    '--matched-canvas-count', '1',
    ...engineAccuracyTraceVerifierArgs(),
  ]
  const verifier = parseJsonCommandResult(run(process.execPath, verifierArgs), 'verifier')
  writeJson(verifierFile, {
    ...verifier,
    perfRunId: config.perfRunId,
    sentSuccess: summary.success,
  })

  const sideEffectArgs = [
    'tools/perf/side-effect-verifier.mjs',
    '--wiremock-url', config.wiremockUrl,
    '--perf-run-id', config.perfRunId,
    '--sent-success', String(summary.success),
    '--path', '/mock/reach/send',
  ]
  const sideEffect = parseJsonCommandResult(run(process.execPath, sideEffectArgs), 'side-effect-verifier')
  writeJson(sideEffectFile, sideEffect)

  if (failedRequestCount(summary) > 0) {
    return {
      status: 'FAIL',
      reason: `runner reported ${failedRequestCount(summary)} failed request(s)`,
      summary,
      verifier,
      sideEffect,
      directory,
    }
  }
  if (verifier.verdict !== 'PASS') {
    return {
      status: 'FAIL',
      reason: `verifier verdict ${verifier.verdict}`,
      summary,
      verifier,
      sideEffect,
      directory,
    }
  }
  if (sideEffect.verdict !== 'PASS') {
    return {
      status: 'FAIL',
      reason: `side-effect verifier verdict ${sideEffect.verdict}`,
      summary,
      verifier,
      sideEffect,
      directory,
    }
  }

  return { status: 'PASS', summary, verifier, sideEffect, directory }
}

async function defaultCleanup(config) {
  const [command, args] = commandForCleanup(config)
  const result = runCommand(command, args)
  return {
    status: result.status === 0 ? 'PASS' : 'FAIL',
    command,
    args,
    stdout: result.stdout,
    stderr: result.stderr,
  }
}

async function defaultReport(config) {
  const directory = runDirectory(config)
  const summaryPath = path.join(directory, 'runner-summary.json')
  const verifierPath = path.join(directory, 'verifier.json')
  if (!existsSync(summaryPath)) {
    throw new Error(`missing runner summary evidence at ${summaryPath}`)
  }
  if (!existsSync(verifierPath)) {
    throw new Error(`missing verifier evidence at ${verifierPath}`)
  }
  const summary = readJson(summaryPath)
  const verifier = readJson(verifierPath)
  assertRunnerReportable(summary, {
    reportType: config.reportType,
    minDurationMin: config.minDurationMin,
    perfRunId: config.perfRunId,
  })
  assertVerifierEvidenceComplete(verifier, {
    perfRunId: summary.perfRunId,
    sentSuccess: summary.success,
  })
  assertCapacityReportable(verifier, { reportType: config.reportType })
  return {
    status: 'PASS',
    summaryPath,
    verifierPath,
    verifierVerdict: verifier.verdict,
    durationMs: summary.durationMs || 0,
  }
}

async function defaultDistributedPlan(config) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for distributed-plan')
  }
  if (!config.workerIds) {
    throw new Error('--worker-ids is required for distributed-plan')
  }
  const plan = buildDistributedPlan({
    perfRunId: config.perfRunId,
    mode: config.mode,
    baseUrl: config.baseUrl,
    eventCode: config.eventCode,
    canvasId: config.canvasId,
    eventSecretEnv: config.eventSecretEnv,
    matchedCanvasCount: config.matchedCanvasCount,
    reportType: config.reportType,
    minDurationMin: config.minDurationMin,
    totalCount: config.totalCount,
    totalConcurrency: config.totalConcurrency,
    workerIds: config.workerIds,
    accuracy: config.accuracy,
  })
  const planFile = distributedPlanPath(config)
  writeDistributedJson(planFile, plan)
  return { status: 'PASS', plan, planFile }
}

async function defaultDistributedWorker(config, deps = {}) {
  if (!config.workerId) {
    throw new Error('--worker-id is required for distributed-worker')
  }
  const run = deps.runCommand || runCommand
  const planFile = distributedPlanPath(config)
  const plan = readDistributedJson(planFile)
  const directory = distributedEvidenceDirectory(config, plan)
  const summaryFile = workerSummaryFileFor(directory, config.workerId)
  const runner = parseJsonCommandResult(
    run(process.execPath, workerRunnerArgs({ plan, workerId: config.workerId, summaryFile })),
    'perf-runner',
  )
  writeDistributedJson(summaryFile, runner)
  return {
    status: failedRequestCount(runner) > 0 ? 'FAIL' : 'PASS',
    planFile,
    summaryFile,
    summary: runner,
  }
}

async function defaultDistributedReport(config, deps = {}) {
  const run = deps.runCommand || runCommand
  const planFile = distributedPlanPath(config)
  const plan = readDistributedJson(planFile)
  const directory = distributedEvidenceDirectory(config, plan)
  const workerSummaries = loadWorkerSummaries(plan, directory)
  const summary = aggregateWorkerSummaries(plan, workerSummaries)
  const summaryFile = path.join(directory, 'distributed-summary.json')
  writeDistributedJson(summaryFile, summary)
  assertDistributedReportable(summary, {
    reportType: plan.accuracy ? 'fault' : plan.reportType,
    minDurationMin: plan.minDurationMin,
  })

  const verifierArgs = [
    'tools/perf/verifier.mjs',
    '--mysql', config.mysql,
    '--database', config.database,
    '--mode', plan.mode,
    '--perf-run-id', plan.perfRunId,
    '--sent-success', String(summary.success),
    '--matched-canvas-count', String(plan.accuracy ? 1 : plan.matchedCanvasCount),
  ]
  if (plan.accuracy) {
    verifierArgs.push(...engineAccuracyTraceVerifierArgs())
  }
  const verifier = parseJsonCommandResult(run(process.execPath, verifierArgs), 'verifier')
  const verifierEvidence = {
    ...verifier,
    perfRunId: plan.perfRunId,
    sentSuccess: summary.success,
  }
  const verifierFile = path.join(directory, 'verifier.json')
  writeDistributedJson(verifierFile, verifierEvidence)
  assertVerifierEvidenceComplete(verifierEvidence, {
    perfRunId: plan.perfRunId,
    sentSuccess: summary.success,
  })
  assertCapacityReportable(verifierEvidence, { reportType: plan.reportType })

  let sideEffect = null
  let sideEffectFile = ''
  if (plan.accuracy) {
    const sideEffectArgs = [
      'tools/perf/side-effect-verifier.mjs',
      '--wiremock-url', config.wiremockUrl,
      '--perf-run-id', plan.perfRunId,
      '--sent-success', String(summary.success),
      '--path', '/mock/reach/send',
    ]
    sideEffect = parseJsonCommandResult(run(process.execPath, sideEffectArgs), 'side-effect-verifier')
    sideEffectFile = path.join(directory, 'side-effect-verifier.json')
    writeDistributedJson(sideEffectFile, sideEffect)
    if (sideEffect.verdict !== 'PASS') {
      return {
        status: 'FAIL',
        reason: `side-effect verifier verdict ${sideEffect.verdict}`,
        summary,
        verifier,
        sideEffect,
        directory,
        summaryFile,
        verifierFile,
        sideEffectFile,
      }
    }
  }

  return {
    status: 'PASS',
    summary,
    verifier,
    sideEffect,
    directory,
    summaryFile,
    verifierFile,
    sideEffectFile,
  }
}

export async function runGuide(config, deps = {}) {
  const handlers = {
    doctor: deps.doctor || defaultDoctor,
    fixture: deps.fixture || ((activeConfig) => defaultFixture(activeConfig, deps)),
    smoke: deps.smoke || ((activeConfig) => defaultSmoke(activeConfig, deps)),
    threshold: deps.threshold || ((activeConfig) => defaultThreshold(activeConfig, deps)),
    soak: deps.soak || ((activeConfig) => defaultSoak(activeConfig, deps)),
    accuracy: deps.accuracy || ((activeConfig) => defaultAccuracy(activeConfig, deps)),
    report: deps.report || defaultReport,
    cleanup: deps.cleanup || defaultCleanup,
    'distributed-plan': deps.distributedPlan || defaultDistributedPlan,
    'distributed-worker': deps.distributedWorker || ((activeConfig) => defaultDistributedWorker(activeConfig, deps)),
    'distributed-report': deps.distributedReport || ((activeConfig) => defaultDistributedReport(activeConfig, deps)),
  }
  return handlers[config.command](config)
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

async function main() {
  const result = await runGuide(parseGuideArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.status === 'FAIL' ? 2 : 0
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
