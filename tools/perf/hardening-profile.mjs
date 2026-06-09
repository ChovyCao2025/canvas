#!/usr/bin/env node

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const DEFAULT_PROFILE_FILE = path.join(SCRIPT_DIR, '3000-hardening-profiles.json')
const READINESS_4000_PROFILE_FILE = path.join(SCRIPT_DIR, '4000-readiness-profiles.json')
const READINESS_4000_ALIAS = '4000-readiness'
const READINESS_4000_DEFAULT_PROFILE = 'readiness-mixed-4000'

function positiveInteger(label, value) {
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${label} must be a positive integer`)
  }
  return value
}

function nonEmptyString(label, value) {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`${label} must be a non-empty string`)
  }
  return value
}

function requireStringArray(label, value) {
  if (
    !Array.isArray(value) ||
    value.length === 0 ||
    value.some((item) => typeof item !== 'string' || item.trim() === '')
  ) {
    throw new Error(`${label} must be a non-empty string array`)
  }
  return value
}

function validateProtectedLaneRules(config) {
  const protectedLanes = requireStringArray('protectedLanes', config.protectedLanes)
  for (const lane of ['HEAVY', 'RETRY']) {
    const blocked = config.borrowRules?.[lane]?.cannotBorrowFrom
    requireStringArray(`${lane}.cannotBorrowFrom`, blocked)
    for (const protectedLane of protectedLanes) {
      if (!blocked.includes(protectedLane)) {
        throw new Error(`${lane} must not borrow protected lane ${protectedLane}`)
      }
    }
  }
}

function validateRequiredProfiles(config) {
  const requiredProfiles = requireStringArray('requiredProfiles', config.requiredProfiles)
  const names = new Set(config.profiles.map((profile) => profile.name))
  for (const name of requiredProfiles) {
    if (!names.has(name)) {
      throw new Error(`missing required profile ${name}`)
    }
  }
}

export function validateHardeningProfiles(config) {
  positiveInteger('targetConcurrency', config.targetConcurrency)
  positiveInteger('observationWindowSeconds', config.observationWindowSeconds)

  const requiredLanes = ['LIGHT', 'STANDARD', 'HEAVY', 'RETRY']
  const laneTotal = requiredLanes.reduce((sum, lane) => {
    const laneConfig = config.lanes?.[lane]
    if (!laneConfig) {
      throw new Error(`missing lane ${lane}`)
    }
    return sum + positiveInteger(`${lane}.concurrency`, laneConfig.concurrency)
  }, 0)

  if (laneTotal !== config.targetConcurrency) {
    throw new Error(`lane total ${laneTotal} must equal targetConcurrency ${config.targetConcurrency}`)
  }

  validateProtectedLaneRules(config)

  if (!Array.isArray(config.stopGates) || config.stopGates.length === 0) {
    throw new Error('stopGates must contain at least one gate')
  }

  if (!Array.isArray(config.profiles) || config.profiles.length === 0) {
    throw new Error('profiles must contain at least one profile')
  }

  for (const profile of config.profiles) {
    nonEmptyString('profile.name', profile.name)
    nonEmptyString(`${profile.name}.description`, profile.description)
    if (!['event', 'direct'].includes(profile.mode)) {
      throw new Error(`${profile.name}.mode must be event or direct`)
    }
    if (profile.mode === 'event') {
      nonEmptyString(`${profile.name}.eventCode`, profile.eventCode)
    }
    if (profile.mode === 'direct') {
      nonEmptyString(`${profile.name}.canvasId`, profile.canvasId)
    }
    if (!Array.isArray(profile.stages) || profile.stages.length === 0) {
      throw new Error(`${profile.name}.stages must contain at least one stage`)
    }
    for (const [index, stage] of profile.stages.entries()) {
      positiveInteger(`${profile.name}.stages[${index}].count`, stage.count)
      positiveInteger(`${profile.name}.stages[${index}].concurrency`, stage.concurrency)
    }
    if (!Number.isInteger(profile.maxFailed) || profile.maxFailed < 0) {
      throw new Error(`${profile.name}.maxFailed must be a non-negative integer`)
    }
    if (!Number.isInteger(profile.maxP95Ms) || profile.maxP95Ms < 0) {
      throw new Error(`${profile.name}.maxP95Ms must be a non-negative integer`)
    }
    if (!Number.isInteger(profile.waitAfterRunMs) || profile.waitAfterRunMs < 0) {
      throw new Error(`${profile.name}.waitAfterRunMs must be a non-negative integer`)
    }
    requireStringArray(`${profile.name}.stopGates`, profile.stopGates)
    requireStringArray(`${profile.name}.rollbackActions`, profile.rollbackActions)
    requireStringArray(`${profile.name}.degradeActions`, profile.degradeActions)
  }

  validateRequiredProfiles(config)

  return config
}

export function selectProfile(config, profileName) {
  const profile = config.profiles.find((candidate) => candidate.name === profileName)
  if (!profile) {
    throw new Error(`unknown profile ${profileName}`)
  }
  return profile
}

export function renderThresholdCommand(profile, options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080'
  const outDir = options.outDir || 'tmp/perf-3000-hardening'
  const runIdPrefix = options.runIdPrefix || `perf_${profile.name}`
  const stages = profile.stages
    .map((stage) => `${stage.count}:${stage.concurrency}`)
    .join(',')

  const args = [
    'node tools/perf/threshold-runner.mjs',
    `--mode ${profile.mode}`,
    `--base-url ${baseUrl}`,
    `--stages ${stages}`,
    '--matched-canvas-count 1',
    `--max-failed ${profile.maxFailed}`,
    `--max-p95-ms ${profile.maxP95Ms}`,
    `--wait-after-run-ms ${profile.waitAfterRunMs}`,
    `--out-dir ${outDir}`,
    `--run-id-prefix ${runIdPrefix}`,
  ]

  if (profile.mode === 'event') {
    args.splice(3, 0, `--event-code ${profile.eventCode}`, '--event-secret-env PERF_EVENT_SECRET')
  }

  if (profile.mode === 'direct') {
    args.splice(3, 0, `--canvas-id ${profile.canvasId}`)
  }

  return args.join(' \\\n  ')
}

export function buildEvidenceManifest(config, profile, options = {}) {
  const now = options.now || new Date().toISOString()
  const runIdPrefix = options.runIdPrefix || `perf_${profile.name}`

  return {
    schemaVersion: 1,
    generatedAt: now,
    runIdPrefix,
    profileName: profile.name,
    targetConcurrency: config.targetConcurrency,
    observationWindowSeconds: config.observationWindowSeconds,
    lanes: config.lanes,
    protectedLanes: config.protectedLanes,
    borrowRules: config.borrowRules,
    stopGates: profile.stopGates,
    rollbackActions: profile.rollbackActions,
    degradeActions: profile.degradeActions,
    command: renderThresholdCommand(profile, options),
    metricSampleFiles: [
      'redis-latency.json',
      'mysql-pool.json',
      'rocketmq-backlog.json',
      'retry-backlog.json',
      'dlq-count.json',
      'trace-buffer.json',
      'downstream-latency.json',
    ],
  }
}

export function buildValidationSummary(config, profile) {
  return {
    profile: profile.name,
    totalConcurrency: config.targetConcurrency,
    laneBudgets: config.lanes,
    stopGates: config.stopGates,
    blockedUntil: config.blockedUntil || null,
  }
}

export function loadProfileFile(filePath = DEFAULT_PROFILE_FILE) {
  return validateHardeningProfiles(JSON.parse(readFileSync(filePath, 'utf8')))
}

function parseCliArgs(argv) {
  const args = {
    profile: 'default-mixed-3000',
    profileFile: DEFAULT_PROFILE_FILE,
    profileFileProvided: false,
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: '',
    writeEvidence: false,
    validateOnly: false,
  }

  for (let index = 0; index < argv.length;) {
    const flag = argv[index]
    if (flag === '--validate-only') {
      const maybeValue = argv[index + 1]
      if (maybeValue && !maybeValue.startsWith('--')) {
        args.validateOnly = maybeValue === 'true'
        index += 2
      } else {
        args.validateOnly = true
        index += 1
      }
      continue
    }
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      throw new Error(`missing value for ${flag}`)
    }
    if (flag === '--profile') args.profile = value
    else if (flag === '--profile-file') {
      args.profileFile = value
      args.profileFileProvided = true
    }
    else if (flag === '--base-url') args.baseUrl = value
    else if (flag === '--out-dir') args.outDir = value
    else if (flag === '--run-id-prefix') args.runIdPrefix = value
    else if (flag === '--write-evidence') args.writeEvidence = value === 'true'
    else throw new Error(`unknown flag ${flag}`)
    index += 2
  }

  if (args.profile === READINESS_4000_ALIAS) {
    args.profile = READINESS_4000_DEFAULT_PROFILE
    if (!args.profileFileProvided) {
      args.profileFile = READINESS_4000_PROFILE_FILE
    }
  }

  return args
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const args = parseCliArgs(process.argv.slice(2))
  const config = loadProfileFile(args.profileFile)
  const profile = selectProfile(config, args.profile)
  if (args.validateOnly) {
    console.log(JSON.stringify(buildValidationSummary(config, profile)))
    process.exit(0)
  }
  const command = renderThresholdCommand(profile, args)
  if (args.writeEvidence) {
    const manifest = buildEvidenceManifest(config, profile, args)
    const runDir = path.join(args.outDir, args.runIdPrefix || `perf_${profile.name}`)
    mkdirSync(runDir, { recursive: true })
    writeFileSync(path.join(runDir, 'evidence-manifest.json'), JSON.stringify(manifest, null, 2))
  }
  console.log(command)
}
