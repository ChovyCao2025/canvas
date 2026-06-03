#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { mkdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(SCRIPT_DIR, '../..')

const DEFAULT_ARGS = {
  mode: 'event',
  baseUrl: 'http://localhost:8080',
  eventCode: 'PERF_ORDER_PAID',
  eventSecretEnv: 'PERF_EVENT_SECRET',
  canvasId: '',
  stages: '1000:10,5000:50,10000:100,30000:200,50000:400',
  matchedCanvasCount: 1,
  maxFailed: 0,
  maxP95Ms: 0,
  waitAfterRunMs: 10000,
  outDir: 'tmp/perf-threshold',
  runIdPrefix: '',
  mysql: 'mysql',
  database: 'canvas_db',
}

const FLAG_NAMES = {
  '--mode': 'mode',
  '--base-url': 'baseUrl',
  '--event-code': 'eventCode',
  '--event-secret-env': 'eventSecretEnv',
  '--canvas-id': 'canvasId',
  '--stages': 'stages',
  '--matched-canvas-count': 'matchedCanvasCount',
  '--max-failed': 'maxFailed',
  '--max-p95-ms': 'maxP95Ms',
  '--wait-after-run-ms': 'waitAfterRunMs',
  '--out-dir': 'outDir',
  '--run-id-prefix': 'runIdPrefix',
  '--mysql': 'mysql',
  '--database': 'database',
}

const NUMBER_ARGS = new Set([
  'matchedCanvasCount',
  'maxFailed',
  'maxP95Ms',
  'waitAfterRunMs',
])

const MODES = new Set(['event', 'direct'])

function parseNonNegativeInteger(flag, value) {
  if (!/^(0|[1-9]\d*)$/.test(value)) {
    throw new Error(`${flag} must be a non-negative integer`)
  }
  return Number(value)
}

function parsePositiveInteger(label, value) {
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error(`${label} must be a positive integer`)
  }
  return Number(value)
}

export function parseStages(value) {
  if (!value || !value.trim()) {
    throw new Error('--stages is required')
  }

  return value.split(',').map((stage, index) => {
    const parts = stage.trim().split(':')
    if (parts.length !== 2) {
      throw new Error(`Invalid stage at index ${index}: expected count:concurrency`)
    }

    return {
      count: parsePositiveInteger(`stage ${index} count`, parts[0]),
      concurrency: parsePositiveInteger(`stage ${index} concurrency`, parts[1]),
    }
  })
}

export function qpsFromSummary(summary) {
  const durationSeconds = (summary.durationMs || 0) / 1000
  if (durationSeconds <= 0) {
    return 0
  }
  return Number((summary.success / durationSeconds).toFixed(2))
}

export function classifyStage({ summary, verifier, maxFailed, maxP95Ms }) {
  if ((summary.failed || 0) > maxFailed) {
    return { ok: false, reason: 'RUNNER_FAILED' }
  }

  if (maxP95Ms > 0 && (summary.p95Ms || 0) > maxP95Ms) {
    return { ok: false, reason: 'P95_EXCEEDED' }
  }

  if (verifier.verdict !== 'PASS') {
    return { ok: false, reason: 'VERIFIER_FAIL' }
  }

  return { ok: true, reason: 'STABLE' }
}

export function parseThresholdArgs(argv) {
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
    args[name] = NUMBER_ARGS.has(name) ? parseNonNegativeInteger(flag, value) : value
  }

  if (!MODES.has(args.mode)) {
    throw new Error('--mode must be one of event, direct')
  }

  if (args.mode === 'direct' && !args.canvasId) {
    throw new Error('--canvas-id is required for direct mode')
  }

  if (args.matchedCanvasCount < 1) {
    throw new Error('--matched-canvas-count must be positive')
  }

  return {
    ...args,
    stages: parseStages(args.stages),
  }
}

function timestamp(value) {
  return value.replaceAll(/\D/g, '').slice(0, 14) || String(Date.now())
}

function stageRunId({ config, stage, stageIndex, now }) {
  const prefix = config.runIdPrefix || `perf_${timestamp(now())}`
  return `${prefix}_${config.mode}_s${stageIndex + 1}_c${stage.concurrency}_n${stage.count}`
}

export function runnerArgs({ config, stage, perfRunId, summaryFile }) {
  const args = [
    path.join(SCRIPT_DIR, 'perf-runner.mjs'),
    '--mode', config.mode,
    '--base-url', config.baseUrl,
    '--perf-run-id', perfRunId,
    '--count', String(stage.count),
    '--concurrency', String(stage.concurrency),
    '--summary-file', summaryFile,
  ]

  if (config.mode === 'event') {
    args.push('--event-code', config.eventCode)
    args.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }

  if (config.mode === 'direct') {
    args.push('--canvas-id', config.canvasId)
    args.push('--event-secret-env', config.eventSecretEnv || 'PERF_EVENT_SECRET')
  }

  return args
}

function verifierArgs({ config, summary, perfRunId }) {
  return [
    path.join(SCRIPT_DIR, 'verifier.mjs'),
    '--mysql', config.mysql,
    '--database', config.database,
    '--mode', config.mode,
    '--perf-run-id', perfRunId,
    '--sent-success', String(summary.success),
    '--matched-canvas-count', String(config.matchedCanvasCount),
  ]
}

function parseJsonOutput(output, command) {
  try {
    return JSON.parse(output)
  } catch (error) {
    throw new Error(`${command} did not return valid JSON: ${error.message}`)
  }
}

function runNodeJson(args, { allowExitCodes }) {
  const result = spawnSync(process.execPath, args, {
    cwd: REPO_ROOT,
    encoding: 'utf8',
  })

  if (result.error) {
    throw result.error
  }

  if (!allowExitCodes.has(result.status)) {
    const stderr = result.stderr ? `\n${result.stderr.trim()}` : ''
    throw new Error(`Command failed: node ${args.join(' ')}${stderr}`)
  }

  return parseJsonOutput(result.stdout, args[0])
}

async function defaultWait(ms) {
  if (ms <= 0) {
    return
  }
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function defaultRunRunner({ config, stage, perfRunId, summaryFile }) {
  mkdirSync(path.dirname(summaryFile), { recursive: true })
  const summary = runNodeJson(runnerArgs({ config, stage, perfRunId, summaryFile }), {
    allowExitCodes: new Set([0, 2]),
  })

  try {
    return JSON.parse(readFileSync(summaryFile, 'utf8'))
  } catch {
    return summary
  }
}

async function defaultRunVerifier({ config, summary, perfRunId }) {
  return runNodeJson(verifierArgs({ config, summary, perfRunId }), {
    allowExitCodes: new Set([0, 2]),
  })
}

function resultForStage({ stage, stageIndex, perfRunId, summary, verifier, decision, summaryFile }) {
  return {
    stageIndex,
    perfRunId,
    count: stage.count,
    concurrency: stage.concurrency,
    qps: qpsFromSummary(summary),
    p95Ms: summary.p95Ms,
    sent: summary.sent,
    success: summary.success,
    failed: summary.failed,
    verifierVerdict: verifier.verdict,
    reason: decision.reason,
    summaryFile,
  }
}

export async function runThresholdPlan(config, deps = {}) {
  const now = deps.now || (() => new Date().toISOString())
  const wait = deps.wait || defaultWait
  const runRunner = deps.runRunner || defaultRunRunner
  const runVerifier = deps.runVerifier || defaultRunVerifier
  const runs = []
  let stableStage = null

  for (let stageIndex = 0; stageIndex < config.stages.length; stageIndex += 1) {
    const stage = config.stages[stageIndex]
    const perfRunId = stageRunId({ config, stage, stageIndex, now })
    const summaryFile = path.join(config.outDir, `${perfRunId}.json`)
    const summary = await runRunner({ config, stage, stageIndex, perfRunId, summaryFile })

    await wait(config.waitAfterRunMs)

    const verifier = await runVerifier({ config, stage, stageIndex, perfRunId, summary })
    const decision = classifyStage({
      summary,
      verifier,
      maxFailed: config.maxFailed,
      maxP95Ms: config.maxP95Ms,
    })
    const stageResult = resultForStage({
      stage,
      stageIndex,
      perfRunId,
      summary,
      verifier,
      decision,
      summaryFile,
    })

    runs.push(stageResult)

    if (!decision.ok) {
      return {
        verdict: stableStage ? 'THRESHOLD_FOUND' : 'NO_STABLE_STAGE',
        stableStage,
        failedStage: stageResult,
        runs,
      }
    }

    stableStage = stageResult
  }

  return {
    verdict: 'MAX_STAGE_STABLE',
    stableStage,
    failedStage: null,
    runs,
  }
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

async function main() {
  const result = await runThresholdPlan(parseThresholdArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.verdict === 'NO_STABLE_STAGE' ? 2 : 0
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
