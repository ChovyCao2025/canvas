#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const COMMANDS = new Set(['doctor', 'fixture', 'smoke', 'threshold', 'soak', 'report', 'cleanup'])

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
}

const NUMBER_FLAGS = new Set(['matchedCanvasCount', 'minDurationMin'])
const BOOLEAN_FLAGS = new Set(['execute', 'rebuild'])

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
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`runner summary field ${field} must be numeric`)
  }
  return value
}

export function assertRunnerSummaryComplete(summary, { perfRunId = '' } = {}) {
  for (const field of ['sent', 'success', 'failed', 'durationMs']) {
    requireNumericEvidence(summary[field], field)
  }
  if (perfRunId && summary.perfRunId !== perfRunId) {
    throw new Error(`runner summary perfRunId ${summary.perfRunId || '(missing)'} does not match ${perfRunId}`)
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
  }
  if (mode === 'event') {
    runnerArgs.push('--event-code', config.eventCode)
    runnerArgs.push('--event-secret-env', config.eventSecretEnv)
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

async function defaultDoctor() {
  return { status: 'PASS' }
}

async function defaultFixture(config) {
  if (!config.rebuild) {
    return {
      status: 'DRY_RUN',
      message: 'fixture command requires --rebuild true to recreate PERF_ resources',
    }
  }
  return { status: 'READY' }
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
    args.push('--event-secret-env', config.eventSecretEnv)
  }
  if (config.mode === 'direct') {
    args.push('--canvas-id', config.canvasId)
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
    status: statuses.get(result.verdict),
    ...result,
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
    count: 300000,
    concurrency: 100,
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
  if (verifier.perfRunId !== summary.perfRunId) {
    throw new Error(`verifier perfRunId ${verifier.perfRunId || '(missing)'} does not match ${summary.perfRunId}`)
  }
  if (verifier.sentSuccess !== summary.success) {
    throw new Error(`verifier sentSuccess ${verifier.sentSuccess ?? '(missing)'} does not match runner success ${summary.success}`)
  }
  assertCapacityReportable(verifier, { reportType: config.reportType })
  return {
    status: 'PASS',
    summaryPath,
    verifierPath,
    verifierVerdict: verifier.verdict,
    durationMs: summary.durationMs || 0,
  }
}

export async function runGuide(config, deps = {}) {
  const handlers = {
    doctor: deps.doctor || defaultDoctor,
    fixture: deps.fixture || defaultFixture,
    smoke: deps.smoke || ((activeConfig) => defaultSmoke(activeConfig, deps)),
    threshold: deps.threshold || ((activeConfig) => defaultThreshold(activeConfig, deps)),
    soak: deps.soak || ((activeConfig) => defaultSoak(activeConfig, deps)),
    report: deps.report || defaultReport,
    cleanup: deps.cleanup || defaultCleanup,
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
