#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
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

export function commandForCleanup(config) {
  return [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', config.perfRunId,
      '--scope', config.scope || 'ledger',
      '--execute', String(Boolean(config.execute)),
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
  const verifierPath = path.join(directory, 'verifier.json')
  if (!existsSync(verifierPath)) {
    throw new Error(`missing verifier evidence at ${verifierPath}`)
  }
  const verifier = readJson(verifierPath)
  assertCapacityReportable(verifier, { reportType: config.reportType })
  return { status: 'PASS', verifierPath, verifierVerdict: verifier.verdict }
}

export async function runGuide(config, deps = {}) {
  const handlers = {
    doctor: deps.doctor || defaultDoctor,
    fixture: deps.fixture || defaultFixture,
    smoke: deps.smoke || (async () => ({ status: 'NOT_IMPLEMENTED' })),
    threshold: deps.threshold || (async () => ({ status: 'NOT_IMPLEMENTED' })),
    soak: deps.soak || (async () => ({ status: 'NOT_IMPLEMENTED' })),
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
