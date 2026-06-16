import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { checkDispatchState } from './check-dispatch-state.mjs'

async function fixture(state, { withBackupManifest = false } = {}) {
  const root = await mkdtemp(path.join(tmpdir(), 'dispatch-state-'))
  mkdirSync(path.join(root, 'docs/program-coordination'), { recursive: true })
  writeFileSync(
    path.join(root, 'docs/program-coordination/dispatch-state.json'),
    `${JSON.stringify(state, null, 2)}\n`,
  )
  if (withBackupManifest) {
    mkdirSync(path.join(root, 'docs/program-coordination/evidence'), { recursive: true })
    writeFileSync(
      path.join(root, 'docs/program-coordination/evidence/pre-rewrite-backup-manifest.md'),
      '# Pre-Rewrite Backup Manifest\n',
    )
  }
  return root
}

function baseState(overrides = {}) {
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
    ],
    parallelGroups: [
      {
        groupId: 'P0-discovery',
        startCondition: 'Current repo available',
        maxCodeWriters: 0,
        maxReadOnlyReviewers: 8,
        taskIds: ['DDD-E01'],
        notes: 'Read-only discovery can run in shared or isolated mode.',
      },
    ],
    reviewerBoard: [],
    recoveryAudit: {
      status: 'clean',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
    lastVerifiedEvidence: [
      {
        command: 'bash docs/program-coordination/checks/program-coordination-checks.sh .',
        result: 'passed',
      },
    ],
    ...overrides,
  }
}

function dispatch(overrides = {}) {
  return {
    dispatchId: 'DISPATCH-DDD-W01-001',
    taskId: 'DDD-W01',
    status: 'RUNNING',
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
    nextAction: 'worker running',
    rollbackPointer: 'git diff -- backend/canvas-platform',
    ...overrides,
  }
}

test('accepts an empty active dispatch state', async () => {
  const root = await fixture(baseState())

  const result = checkDispatchState(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
})

test('rejects stale contradictory cutover evidence after ready evidence with empty blockers', async () => {
  const root = await fixture(baseState({
    readiness: {
      level: 'R5',
      gate: 'Cutover preflight ready with blockers empty',
      backendTarget: 'cutoverReady true; blockers empty',
      writeMode: 'No active dispatches',
    },
    lastEvent: 'Cutover preflight ready: cutoverReady true, blockers empty.',
    lastVerifiedEvidence: [
      {
        command: 'node tools/program-coordination/cutover-compatibility-preflight.mjs . --json',
        result: 'passed; next candidates route:/canvas/batch; cutoverReady=false; blockers remain',
      },
      {
        command: 'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json',
        result: 'passed: cutoverReady true, blockers []',
      },
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /stale contradictory lastVerifiedEvidence\[0\]/)
  assert.match(result.errors.join('\n'), /cutoverReady=false/)
  assert.match(result.errors.join('\n'), /route:\/canvas\/batch/)
  assert.match(result.errors.join('\n'), /blockers remain/)
})

test('rejects missing parallel groups', async () => {
  const state = baseState()
  delete state.parallelGroups
  const root = await fixture(state)

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /parallelGroups/)
})

test('rejects recovery audit without backup gate', async () => {
  const root = await fixture(baseState({
    recoveryAudit: {
      status: 'clean',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G1', 'G2'],
    },
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /G0B backup gate/)
})

test('rejects parallel groups that reference missing worker board tasks', async () => {
  const root = await fixture(baseState({
    parallelGroups: [
      {
        groupId: 'P7-plugin-burst',
        startCondition: 'DDD-W08 integrated and OSG-C07 done',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 2,
        taskIds: ['OSG-W07A', 'OSG-W07B'],
        notes: 'Plugin workers must be individually represented.',
      },
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /parallelGroups\[0\].taskIds references missing workerBoard task: OSG-W07A/)
  assert.match(result.errors.join('\n'), /parallelGroups\[0\].taskIds references missing workerBoard task: OSG-W07B/)
})

test('rejects parallel groups that exceed code writer limits', async () => {
  const root = await fixture(baseState({
    workerBoard: [
      {
        taskId: 'OSG-W01',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G0/G1',
        writeScope: ['README.md'],
      },
      {
        taskId: 'OSG-W02',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G0/G1',
        writeScope: ['wiremock/**'],
      },
    ],
    parallelGroups: [
      {
        groupId: 'P1-too-many-writers',
        startCondition: 'Coordination docs accepted',
        maxCodeWriters: 1,
        maxReadOnlyReviewers: 4,
        taskIds: ['OSG-W01', 'OSG-W02'],
        notes: 'Fixture intentionally exceeds writer count.',
      },
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /exceeds maxCodeWriters/)
})

test('rejects overlapping active write reservations', async () => {
  const root = await fixture(baseState({
    activeDispatches: [
      dispatch({ dispatchId: 'DISPATCH-DDD-W01-001', exactReservedFiles: ['backend/canvas-platform/**'] }),
      dispatch({
        dispatchId: 'DISPATCH-DDD-W01-002',
        taskId: 'DDD-W99',
        worker: 'worker-2',
        branch: 'work/overlap',
        worktree: '../canvas-overlap',
        exactReservedFiles: ['backend/canvas-platform/src/main/java/org/chovy/canvas/platform/PlatformService.java'],
      }),
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /overlap/)
  assert.match(result.errors.join('\n'), /DISPATCH-DDD-W01-001/)
  assert.match(result.errors.join('\n'), /DISPATCH-DDD-W01-002/)
})

test('rejects incomplete active dispatch records', async () => {
  const missingBaseSha = dispatch()
  delete missingBaseSha.baseSha
  const root = await fixture(baseState({ activeDispatches: [missingBaseSha] }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /baseSha/)
})

test('rejects active dispatch status that disagrees with the worker board', async () => {
  const root = await fixture(baseState({
    activeDispatches: [dispatch({ status: 'RUNNING' })],
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'active dispatch registered',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }), { withBackupManifest: true })

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /workerBoard status READY must match active dispatch status RUNNING/)
})

test('rejects active code-writing dispatches without the pre-rewrite backup manifest', async () => {
  const root = await fixture(baseState({
    activeDispatches: [dispatch()],
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'RUNNING',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'active dispatch registered',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /pre-rewrite backup manifest/)
})

test('accepts active code-writing dispatches after the pre-rewrite backup manifest exists', async () => {
  const root = await fixture(baseState({
    activeDispatches: [dispatch()],
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'RUNNING',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'active dispatch registered',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }), { withBackupManifest: true })

  const result = checkDispatchState(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
})

test('rejects active worker board rows without matching active dispatches', async () => {
  const root = await fixture(baseState({
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'RUNNING',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'stale worker board fixture',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /DDD-W01 is RUNNING but has no matching active dispatch/)
})

test('rejects generic RUNNING code-writing worker names', async () => {
  const root = await fixture(baseState({
    activeDispatches: [dispatch({ worker: 'multi_agent_v1-worker' })],
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'RUNNING',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'active dispatch registered',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }), { withBackupManifest: true })

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /must name the actual spawned worker id\/nickname/)
})

test('accepts explicit inline fallback reason when a code-writing worker is running', async () => {
  const root = await fixture(baseState({
    activeDispatches: [dispatch({ worker: 'main-agent-inline fallback reason: multi_agent_v1 unavailable in this runtime' })],
    workerBoard: [
      ...baseState().workerBoard,
      {
        taskId: 'DDD-W01',
        status: 'RUNNING',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    recoveryAudit: {
      status: 'active dispatch registered',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
  }), { withBackupManifest: true })

  const result = checkDispatchState(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
})

test('rejects old canvas-engine writes without a complete bridge declaration', async () => {
  const root = await fixture(baseState({
    activeDispatches: [
      dispatch({
        dispatchId: 'DISPATCH-OSG-W10-001',
        taskId: 'OSG-W10',
        integrationTarget: 'CURRENT_ENGINE_BRIDGE',
        exactReservedFiles: ['backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasDslController.java'],
      }),
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /Bridge Declaration/)
  assert.match(result.errors.join('\n'), /finalDddOwnerModule/)
})

test('rejects coordinator-owned files unless explicitly lent, and never lends state files', async () => {
  const root = await fixture(baseState({
    activeDispatches: [
      dispatch({
        dispatchId: 'DISPATCH-BAD-LEDGER',
        exactReservedFiles: ['docs/program-coordination/progress-ledger.md'],
        coordinatorOwnedExceptions: ['docs/program-coordination/progress-ledger.md'],
      }),
    ],
  }))

  const result = checkDispatchState(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /progress-ledger\.md cannot be reserved/)
})
