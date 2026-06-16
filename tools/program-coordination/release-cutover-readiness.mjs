#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const failureSummaryLimit = 1200

const requiredGates = [
  {
    name: 'cutover compatibility preflight',
    command: 'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json',
    type: 'node',
    args: ['tools/program-coordination/cutover-compatibility-preflight.mjs', '.', '--require-ready', '--json']
  },
  {
    name: 'open source growth guardrails',
    command: 'node tools/open-source-growth/guardrail-verifier.mjs',
    type: 'node',
    args: ['tools/open-source-growth/guardrail-verifier.mjs']
  },
  {
    name: 'G10 public API stability',
    command: 'node tools/open-source-growth/g10-public-api-stability.mjs',
    type: 'node',
    args: ['tools/open-source-growth/g10-public-api-stability.mjs']
  },
  {
    name: 'playground runtime smoke',
    command: 'node tools/open-source-growth/playground-runtime-smoke.mjs',
    type: 'node',
    args: ['tools/open-source-growth/playground-runtime-smoke.mjs']
  },
  {
    name: 'Helm render smoke',
    command: 'bash scripts/release/verify-helm-render.sh'
  },
  {
    name: 'release script syntax',
    command: 'bash -n scripts/release/*.sh'
  },
  {
    name: 'backend image dry-run',
    command: 'CANVAS_IMAGE_TAG=test-sha bash scripts/release/build-image.sh --dry-run --image canvas-boot'
  },
  {
    name: 'pre-deploy dry-run',
    command: 'bash scripts/release/pre-deploy-check.sh --dry-run'
  },
  {
    name: 'post-deploy dry-run',
    command: 'bash scripts/release/post-deploy-check.sh --dry-run'
  },
  {
    name: 'rollback drill dry-run',
    command: 'bash scripts/release/rollback-drill.sh --dry-run'
  }
]

export function parseArgs(argv) {
  let root = '.'
  let json = false

  for (const arg of argv) {
    if (arg === '--json') {
      json = true
      continue
    }
    if (arg.startsWith('--')) {
      throw new Error(`Unknown option: ${arg}`)
    }
    root = arg
  }

  return { root, json }
}

export function runReadiness(rootDirectory = '.') {
  const root = path.resolve(rootDirectory)
  const gates = requiredGates.map((gate) => runGate(root, gate))
  const failedGates = gates.filter((gate) => gate.status !== 'pass')

  return {
    ok: failedGates.length === 0,
    readiness: failedGates.length === 0 ? 'ready' : 'blocked',
    root,
    gates,
    failureSummary: failedGates.map((gate) => ({
      name: gate.name,
      command: gate.command,
      exitCode: gate.exitCode,
      summary: gate.failureSummary
    }))
  }
}

function runGate(root, gate) {
  const result = gate.type === 'node'
    ? spawnSync(process.execPath, gate.args, {
        cwd: root,
        encoding: 'utf8',
        maxBuffer: 10 * 1024 * 1024
      })
    : spawnSync(gate.command, {
        cwd: root,
        encoding: 'utf8',
        shell: true,
        maxBuffer: 10 * 1024 * 1024
      })
  const exitCode = typeof result.status === 'number' ? result.status : 1
  const passed = exitCode === 0 && !result.error

  return {
    name: gate.name,
    command: gate.command,
    status: passed ? 'pass' : 'fail',
    exitCode,
    failureSummary: passed ? null : summarizeFailure(result)
  }
}

function summarizeFailure(result) {
  const parts = []
  if (result.error) {
    parts.push(result.error.message)
  }
  parts.push(result.stderr ?? '')
  parts.push(result.stdout ?? '')

  const compact = parts
    .join('\n')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .join('\n')

  if (compact.length <= failureSummaryLimit) {
    return compact
  }
  return `${compact.slice(0, failureSummaryLimit)}\n... truncated`
}

function printHuman(report) {
  const status = report.ok ? 'READY' : 'BLOCKED'
  console.log(`Release cutover readiness: ${status}`)
  for (const gate of report.gates) {
    const prefix = gate.status === 'pass' ? 'PASS' : 'FAIL'
    console.log(`${prefix} ${gate.name}: ${gate.command}`)
    if (gate.failureSummary) {
      console.log(gate.failureSummary)
    }
  }
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) {
  try {
    const options = parseArgs(process.argv.slice(2))
    const report = runReadiness(options.root)
    if (options.json) {
      console.log(JSON.stringify(report, null, 2))
    } else {
      printHuman(report)
    }
    if (!report.ok) {
      process.exitCode = 1
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 2
  }
}
