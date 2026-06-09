import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { generateWorkerPrompt } from './generate-worker-prompt.mjs'

async function fixture({ state }) {
  const root = await mkdtemp(path.join(tmpdir(), 'worker-prompt-'))
  mkdirSync(path.join(root, 'docs/program-coordination'), { recursive: true })
  writeFileSync(
    path.join(root, 'docs/program-coordination/dispatch-state.json'),
    `${JSON.stringify(state, null, 2)}\n`,
  )
  writeFileSync(path.join(root, 'docs/program-coordination/subagent-worker-packets.md'), [
    '# Subagent Worker Packets',
    '',
    '## Universal Worker Rules',
    '',
    '```text',
    'Do not modify files outside your assigned write scope.',
    'If the coordinator did not provide a dispatch id for code-writing work, return NEEDS_CONTEXT before editing files.',
    '```',
    '',
    '## Coordinator-Only Tasks',
    '',
    '### DDD-E01: HTTP API Inventory Explorer',
    '',
    '```text',
    'Program: DDD modular rewrite',
    'Task id: DDD-E01',
    'Mode: read-only',
    'Allowed write scope: none',
    '```',
    '',
    '### DDD-W01: Platform Worker',
    '',
    '```text',
    'Program: DDD modular rewrite',
    'Task id: DDD-W01',
    'Readiness gate: R2 / G4',
    'Target backend state: DDD_FINAL_MODULE',
    'Allowed write scope:',
    '  backend/canvas-platform/**',
    '```',
    '',
    '### OSG-W07A Through OSG-W07F: Official Plugin Workers',
    '',
    'Use one worker per row.',
    '',
    '| Task | Plugin | Allowed write scope | Docs file |',
    '| --- | --- | --- | --- |',
    '| OSG-W07A | webhook | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**` | `docs/open-source/plugins/official/webhook.md` |',
    '| OSG-W07B | message | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/**` | `docs/open-source/plugins/official/message.md` |',
    '',
    'Shared packet:',
    '',
    '```text',
    'Readiness gate: R5 after G9 and OSG-C07',
    'Target backend state: DDD_FINAL_MODULE',
    '```',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/program-coordination/collaboration-and-recovery-protocol.md'), [
    '## Worker Return Contract',
    '',
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
  ].join('\n'))
  return root
}

function state(overrides = {}) {
  return {
    schemaVersion: 1,
    updatedAt: '2026-06-09',
    readiness: {
      level: 'R0',
      gate: 'G0/G0B-before-code-writing/G1/G2',
      backendTarget: 'no code worker active',
      writeMode: 'coordinator-only for planning docs',
    },
    activeDispatches: [],
    workerBoard: [
      {
        taskId: 'DDD-E01',
        status: 'READY',
        mode: 'read-only',
        gate: 'R0',
        writeScope: [],
      },
      {
        taskId: 'DDD-W01',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
      {
        taskId: 'OSG-W07B',
        status: 'NOT_STARTED',
        mode: 'code-writing',
        gate: 'G9/G10 plus OSG-C07',
        writeScope: ['backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/**'],
      },
    ],
    parallelGroups: [
      {
        groupId: 'P7-plugin-burst',
        startCondition: 'DDD-W08 integrated and OSG-C07 done',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 2,
        taskIds: ['OSG-W07B'],
        notes: 'Fixture plugin burst.',
      },
    ],
    reviewerBoard: [],
    recoveryAudit: {
      status: 'clean',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
    lastVerifiedEvidence: [],
    ...overrides,
  }
}

function dispatch() {
  return {
    dispatchId: 'DISPATCH-DDD-W01-001',
    taskId: 'DDD-W01',
    status: 'RESERVED',
    worker: 'worker-1',
    mode: 'code-writing',
    branch: 'work/ddd-w01-platform',
    worktree: '../canvas-ddd-w01-platform',
    baseSha: 'abc1234',
    integrationTarget: 'DDD_FINAL_MODULE',
    exactReservedFiles: ['backend/canvas-platform/**'],
    coordinatorOwnedExceptions: [],
    gateAtDispatch: 'G4',
    lastCommandResult: 'not run yet',
    evidencePath: 'docs/program-coordination/evidence/DISPATCH-DDD-W01-001/',
    nextAction: 'send prompt',
    rollbackPointer: 'git diff -- backend/canvas-platform',
  }
}

function pluginDispatch() {
  return {
    dispatchId: 'DISPATCH-OSG-W07B-001',
    taskId: 'OSG-W07B',
    status: 'RESERVED',
    worker: 'worker-plugin-message',
    mode: 'code-writing',
    branch: 'work/osg-w07b-message',
    worktree: '../canvas-osg-w07b-message',
    baseSha: 'abc1234',
    integrationTarget: 'DDD_FINAL_MODULE',
    exactReservedFiles: [
      'backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/**',
      'docs/open-source/plugins/official/message.md',
    ],
    coordinatorOwnedExceptions: [],
    gateAtDispatch: 'G10',
    lastCommandResult: 'not run yet',
    evidencePath: 'docs/program-coordination/evidence/DISPATCH-OSG-W07B-001/',
    nextAction: 'send prompt',
    rollbackPointer: 'git diff -- backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message docs/open-source/plugins/official/message.md',
  }
}

test('requires an active dispatch row for code-writing worker prompts', async () => {
  const root = await fixture({ state: state() })

  assert.throws(
    () => generateWorkerPrompt(root, 'DDD-W01'),
    /active dispatch row is required/,
  )
})

test('generates a code-writing worker prompt from state and packet section', async () => {
  const root = await fixture({
    state: state({
      activeDispatches: [dispatch()],
      recoveryAudit: {
        status: 'active dispatch registered',
        activeDispatches: 1,
        commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
      },
    }),
  })

  const prompt = generateWorkerPrompt(root, 'DDD-W01')

  assert.match(prompt, /DISPATCH-DDD-W01-001/)
  assert.match(prompt, /backend\/canvas-platform\/\*\*/)
  assert.match(prompt, /Do not modify files outside your assigned write scope/)
  assert.match(prompt, /DDD-W01: Platform Worker/)
  assert.match(prompt, /verification output summary\/path:/)
})

test('allows read-only explorer prompts without a code-writing dispatch row', async () => {
  const root = await fixture({ state: state() })

  const prompt = generateWorkerPrompt(root, 'DDD-E01')

  assert.match(prompt, /Task id: DDD-E01/)
  assert.match(prompt, /No active code-writing reservation is required/)
  assert.match(prompt, /HTTP API Inventory Explorer/)
})

test('generates a prompt for a shared packet worker row', async () => {
  const root = await fixture({
    state: state({
      activeDispatches: [pluginDispatch()],
      recoveryAudit: {
        status: 'active dispatch registered',
        activeDispatches: 1,
        commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
      },
    }),
  })

  const prompt = generateWorkerPrompt(root, 'OSG-W07B')

  assert.match(prompt, /DISPATCH-OSG-W07B-001/)
  assert.match(prompt, /OSG-W07A Through OSG-W07F/)
  assert.match(prompt, /Selected shared-packet task: OSG-W07B/)
  assert.match(prompt, /official\/message\/\*\*/)
  assert.match(prompt, /docs\/open-source\/plugins\/official\/message\.md/)
})
