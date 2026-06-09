#!/usr/bin/env node
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

import { checkDispatchState, loadDispatchState } from './check-dispatch-state.mjs'

const PACKET_PATH = 'docs/program-coordination/subagent-worker-packets.md'
const PROTOCOL_PATH = 'docs/program-coordination/collaboration-and-recovery-protocol.md'
const ACTIVE_PROMPT_STATUSES = new Set(['RESERVED', 'RUNNING'])

function readText(root, relativePath) {
  return readFileSync(path.join(root, relativePath), 'utf8')
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function extractBetween(content, startHeading, endHeading) {
  const start = content.indexOf(startHeading)
  if (start === -1) {
    return ''
  }
  const bodyStart = start + startHeading.length
  const end = endHeading ? content.indexOf(endHeading, bodyStart) : -1
  return content.slice(bodyStart, end === -1 ? undefined : end).trim()
}

function extractTaskSection(content, taskId) {
  const pattern = new RegExp(`^### ${escapeRegExp(taskId)}:.*$`, 'm')
  const match = content.match(pattern)
  if (!match || match.index === undefined) {
    return extractSharedTaskSection(content, taskId)
  }
  const start = match.index
  const next = content.slice(start + match[0].length).search(/^### /m)
  if (next === -1) {
    return content.slice(start).trim()
  }
  return content.slice(start, start + match[0].length + next).trim()
}

function extractSharedTaskSection(content, taskId) {
  const sharedHeadings = [
    {
      taskPattern: /^OSG-W07[A-F]$/,
      headingPattern: /^### OSG-W07A Through OSG-W07F:.*$/m,
    },
  ]
  const shared = sharedHeadings.find(candidate => candidate.taskPattern.test(taskId))
  if (!shared) {
    return ''
  }
  const match = content.match(shared.headingPattern)
  if (!match || match.index === undefined) {
    return ''
  }
  const start = match.index
  const next = content.slice(start + match[0].length).search(/^### /m)
  const section = content.slice(start, next === -1 ? undefined : start + match[0].length + next).trim()
  const selectedRow = section.split('\n').find(line => line.includes(`| ${taskId} |`))?.trim()
  return [
    section,
    '',
    `Selected shared-packet task: ${taskId}`,
    selectedRow ? `Selected row: ${selectedRow}` : 'Selected row: not found; return NEEDS_CONTEXT before editing.',
  ].join('\n')
}

function extractReturnContract(content) {
  const section = extractBetween(content, '## Worker Return Contract', '## Reviewer Contract')
  return section || [
    'Every worker return must include:',
    '',
    '```text',
    'status:',
    'task id:',
    'dispatch id:',
    'branch:',
    'worktree:',
    'base commit:',
    'head commit:',
    'files changed:',
    'contracts changed:',
    'tests run:',
    'verification result:',
    'verification output summary/path:',
    'evidence artifact paths:',
    'risks:',
    'coordinator actions needed:',
    'ledger update:',
    'rollback path:',
    '```',
  ].join('\n')
}

function isReadOnlyTask(workerBoardEntry, taskSection) {
  return workerBoardEntry?.mode === 'read-only'
    || taskSection.includes('Mode: read-only')
    || taskSection.includes('Allowed write scope: none')
}

function formatDispatch(dispatch) {
  if (!dispatch) {
    return [
      'No active code-writing reservation is required because this task is read-only.',
      'The worker must not edit files and must return findings only.',
    ].join('\n')
  }
  const bridge = dispatch.bridgeDeclaration
    ? [
      'bridge declaration:',
      `  exact old service/API: ${dispatch.bridgeDeclaration.exactOldServiceApi}`,
      `  exact old files: ${dispatch.bridgeDeclaration.exactOldFiles.join(', ')}`,
      `  final DDD owner module: ${dispatch.bridgeDeclaration.finalDddOwnerModule}`,
      `  idempotency rule: ${dispatch.bridgeDeclaration.idempotencyRule}`,
      `  removal gate: ${dispatch.bridgeDeclaration.removalGate}`,
      `  rollback path: ${dispatch.bridgeDeclaration.rollbackPath}`,
    ].join('\n')
    : 'bridge declaration: not required by dispatch state'

  return [
    `dispatch id: ${dispatch.dispatchId}`,
    `task id: ${dispatch.taskId}`,
    `status: ${dispatch.status}`,
    `worker: ${dispatch.worker}`,
    `mode: ${dispatch.mode}`,
    `branch: ${dispatch.branch}`,
    `worktree: ${dispatch.worktree}`,
    `base SHA: ${dispatch.baseSha}`,
    `integration target: ${dispatch.integrationTarget}`,
    `exact reserved files: ${dispatch.exactReservedFiles.join(', ')}`,
    `coordinator-owned exceptions: ${dispatch.coordinatorOwnedExceptions.join(', ') || 'none'}`,
    `gate at dispatch: ${dispatch.gateAtDispatch}`,
    `last command/result: ${dispatch.lastCommandResult}`,
    `evidence path: ${dispatch.evidencePath}`,
    `next action: ${dispatch.nextAction}`,
    `rollback pointer: ${dispatch.rollbackPointer}`,
    bridge,
  ].join('\n')
}

export function generateWorkerPrompt(rootDir = process.cwd(), taskId) {
  if (!taskId) {
    throw new Error('task id is required')
  }
  const root = path.resolve(rootDir)
  const validation = checkDispatchState(root)
  if (!validation.ok) {
    throw new Error(`dispatch state is invalid:\n${validation.errors.join('\n')}`)
  }

  const state = loadDispatchState(root)
  const packet = readText(root, PACKET_PATH)
  const protocol = readText(root, PROTOCOL_PATH)
  const universalRules = extractBetween(packet, '## Universal Worker Rules', '## Coordinator-Only Tasks')
  const taskSection = extractTaskSection(packet, taskId)
  if (!taskSection) {
    throw new Error(`${PACKET_PATH} does not contain a worker packet for ${taskId}`)
  }

  const workerBoardEntry = state.workerBoard.find(worker => worker.taskId === taskId)
  const activeDispatch = state.activeDispatches.find(dispatch => (
    dispatch.taskId === taskId && ACTIVE_PROMPT_STATUSES.has(dispatch.status)
  ))
  if (!activeDispatch && !isReadOnlyTask(workerBoardEntry, taskSection)) {
    throw new Error(`active dispatch row is required before generating a code-writing prompt for ${taskId}`)
  }

  return [
    '# Worker Dispatch Prompt',
    '',
    'You are receiving a scoped worker assignment from the coordinator. Follow the dispatch state exactly.',
    '',
    '## Required Reading',
    '',
    `- ${PACKET_PATH}`,
    `- ${PROTOCOL_PATH}`,
    '- docs/program-coordination/gate-verification-matrix.md',
    '- the owning DDD or Open Source Growth files named by the worker packet',
    '',
    '## Dispatch State',
    '',
    '```text',
    formatDispatch(activeDispatch),
    '```',
    '',
    '## Universal Worker Rules',
    '',
    universalRules,
    '',
    '## Worker Packet',
    '',
    taskSection,
    '',
    '## Canonical Return Contract',
    '',
    extractReturnContract(protocol),
    '',
    'Return NEEDS_CONTEXT instead of editing when any required field, reservation, bridge declaration, or gate is missing.',
  ].join('\n')
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const taskId = process.argv[2]
  const rootDir = process.argv[3] ?? process.cwd()
  if (!taskId) {
    console.error('Usage: node tools/program-coordination/generate-worker-prompt.mjs <TASK_ID> [repo-root]')
    process.exit(2)
  }
  try {
    process.stdout.write(`${generateWorkerPrompt(rootDir, taskId)}\n`)
  } catch (error) {
    console.error(error.message)
    process.exit(1)
  }
}
