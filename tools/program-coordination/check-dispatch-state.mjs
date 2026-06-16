#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

export const DISPATCH_STATE_PATH = 'docs/program-coordination/dispatch-state.json'
export const BACKUP_MANIFEST_PATH = 'docs/program-coordination/evidence/pre-rewrite-backup-manifest.md'

const KNOWN_STATUSES = new Set([
  'NOT_STARTED',
  'READY',
  'RESERVED',
  'RUNNING',
  'RETURNED',
  'REVIEWING',
  'INTEGRATED',
  'DONE',
  'DONE_WITH_CONCERNS',
  'NEEDS_CONTEXT',
  'BLOCKED',
  'ABORTED',
])

const RESERVATION_STATUSES = new Set([
  'RESERVED',
  'RUNNING',
  'RETURNED',
  'REVIEWING',
  'NEEDS_CONTEXT',
  'BLOCKED',
])

const WRITE_MODES = new Set(['code-writing', 'coordinator'])
const READ_ONLY_MODES = new Set(['read-only', 'review'])

const GENERIC_RUNNING_CODE_WORKERS = new Set([
  'main-agent-inline',
  'multi_agent_v1-worker',
  'pending',
  'pending-spawn',
  'unassigned',
])

const INTEGRATION_TARGETS = new Set([
  'NO_CODE',
  'DOCS_ONLY',
  'FRONTEND_ONLY',
  'TOOLING_ONLY',
  'CURRENT_ENGINE_BRIDGE',
  'DDD_FINAL_MODULE',
])

const REQUIRED_STATE_FIELDS = [
  'schemaVersion',
  'updatedAt',
  'readiness',
  'activeDispatches',
  'workerBoard',
  'parallelGroups',
  'reviewerBoard',
  'recoveryAudit',
  'lastVerifiedEvidence',
]

const REQUIRED_DISPATCH_FIELDS = [
  'dispatchId',
  'taskId',
  'status',
  'worker',
  'mode',
  'branch',
  'worktree',
  'baseSha',
  'integrationTarget',
  'exactReservedFiles',
  'coordinatorOwnedExceptions',
  'gateAtDispatch',
  'lastCommandResult',
  'evidencePath',
  'nextAction',
  'rollbackPointer',
]

const REQUIRED_BRIDGE_FIELDS = [
  'exactOldServiceApi',
  'exactOldFiles',
  'finalDddOwnerModule',
  'idempotencyRule',
  'removalGate',
  'rollbackPath',
]

const HARD_COORDINATOR_OWNED = [
  'docs/program-coordination/progress-ledger.md',
  'docs/program-coordination/dispatch-state.json',
]

const COORDINATOR_OWNED_SCOPES = [
  'docs/program-coordination/**',
  'backend/pom.xml',
  'backend/canvas-engine/src/main/resources/application*.yml',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandlerType.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java',
  'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java',
  'backend/canvas-engine/src/main/resources/db/migration/**',
  'frontend/src/App.tsx',
  'frontend/package.json',
  'frontend/package-lock.json',
  'frontend/pnpm-lock.yaml',
  'frontend/yarn.lock',
  'docker-compose.local.yml',
  'docs/INDEX.md',
]

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function normalizeScope(rawScope) {
  return String(rawScope).replaceAll('\\', '/').replace(/\/+$/, '')
}

function scopeParts(rawScope) {
  const normalized = normalizeScope(rawScope)
  const isDirectory = normalized.endsWith('/**')
  const base = isDirectory ? normalized.slice(0, -3).replace(/\/+$/, '') : normalized
  return { base, isDirectory, raw: normalized }
}

function scopesOverlap(left, right) {
  const a = scopeParts(left)
  const b = scopeParts(right)
  if (a.base === b.base) {
    return true
  }
  if (a.isDirectory && (b.base === a.base || b.base.startsWith(`${a.base}/`))) {
    return true
  }
  if (b.isDirectory && (a.base === b.base || a.base.startsWith(`${b.base}/`))) {
    return true
  }
  return false
}

function hasUnsupportedWildcard(scope) {
  if (!scope.includes('*')) {
    return false
  }
  return !(scope.endsWith('/**') && !scope.slice(0, -3).includes('*'))
}

function hasPathTraversal(scope) {
  return normalizeScope(scope).split('/').includes('..') || path.isAbsolute(scope)
}

function arrayIncludesOverlap(scopes, candidate) {
  return scopes.some(scope => scopesOverlap(scope, candidate))
}

function isReservationActive(dispatch) {
  return RESERVATION_STATUSES.has(dispatch.status)
}

function isWriteReservation(dispatch) {
  return isReservationActive(dispatch) && !READ_ONLY_MODES.has(dispatch.mode)
}

function requiresPreRewriteBackup(dispatch) {
  if (!isWriteReservation(dispatch)) {
    return false
  }
  if (dispatch.mode === 'code-writing') {
    return true
  }
  return dispatch.mode === 'coordinator'
    && dispatch.integrationTarget !== 'NO_CODE'
    && dispatch.integrationTarget !== 'DOCS_ONLY'
}

export function loadDispatchState(rootDir = process.cwd()) {
  const root = path.resolve(rootDir)
  const stateFile = path.join(root, DISPATCH_STATE_PATH)
  if (!existsSync(stateFile)) {
    throw new Error(`${DISPATCH_STATE_PATH} is required`)
  }
  return JSON.parse(readFileSync(stateFile, 'utf8'))
}

function validateTopLevel(state, errors) {
  if (!isObject(state)) {
    errors.push(`${DISPATCH_STATE_PATH} must contain a JSON object`)
    return
  }
  for (const field of REQUIRED_STATE_FIELDS) {
    if (!(field in state)) {
      errors.push(`${DISPATCH_STATE_PATH} missing required field: ${field}`)
    }
  }
  if (state.schemaVersion !== 1) {
    errors.push('dispatch-state schemaVersion must be 1')
  }
  if (!isObject(state.readiness)) {
    errors.push('readiness must be an object')
  } else {
    for (const field of ['level', 'gate', 'backendTarget', 'writeMode']) {
      if (!state.readiness[field]) {
        errors.push(`readiness missing required field: ${field}`)
      }
    }
  }
  for (const field of ['activeDispatches', 'workerBoard', 'parallelGroups', 'reviewerBoard', 'lastVerifiedEvidence']) {
    if (field in state && !Array.isArray(state[field])) {
      errors.push(`${field} must be an array`)
    }
  }
  if (!isObject(state.recoveryAudit)) {
    errors.push('recoveryAudit must be an object')
  }
}

function validateWorkerBoard(state, errors) {
  if (!Array.isArray(state.workerBoard)) {
    return
  }
  const workerByTaskId = new Map()
  for (const [index, worker] of state.workerBoard.entries()) {
    const label = `workerBoard[${index}]`
    if (!isObject(worker)) {
      errors.push(`${label} must be an object`)
      continue
    }
    for (const field of ['taskId', 'status', 'mode', 'gate', 'writeScope']) {
      if (!(field in worker)) {
        errors.push(`${label} missing required field: ${field}`)
      }
    }
    if (worker.taskId) {
      if (workerByTaskId.has(worker.taskId)) {
        errors.push(`${label} duplicates taskId: ${worker.taskId}`)
      }
      workerByTaskId.set(worker.taskId, worker)
    }
    if (worker.status && !KNOWN_STATUSES.has(worker.status)) {
      errors.push(`${label} has unknown status: ${worker.status}`)
    }
    if (worker.writeScope && !Array.isArray(worker.writeScope)) {
      errors.push(`${label}.writeScope must be an array`)
    }
  }
  return workerByTaskId
}

function validateParallelGroups(state, workerByTaskId, errors) {
  if (!Array.isArray(state.parallelGroups)) {
    return
  }
  const groupIds = new Set()
  for (const [index, group] of state.parallelGroups.entries()) {
    const label = `parallelGroups[${index}]`
    if (!isObject(group)) {
      errors.push(`${label} must be an object`)
      continue
    }
    for (const field of ['groupId', 'startCondition', 'maxCodeWriters', 'maxReadOnlyReviewers', 'taskIds', 'notes']) {
      if (!(field in group)) {
        errors.push(`${label} missing required field: ${field}`)
      }
    }
    if (group.groupId) {
      if (groupIds.has(group.groupId)) {
        errors.push(`${label} duplicates groupId: ${group.groupId}`)
      }
      groupIds.add(group.groupId)
    }
    if (!Number.isInteger(group.maxCodeWriters) || group.maxCodeWriters < 0) {
      errors.push(`${label}.maxCodeWriters must be a non-negative integer`)
    }
    if (!Number.isInteger(group.maxReadOnlyReviewers) || group.maxReadOnlyReviewers < 0) {
      errors.push(`${label}.maxReadOnlyReviewers must be a non-negative integer`)
    }
    if (!Array.isArray(group.taskIds)) {
      errors.push(`${label}.taskIds must be an array`)
      continue
    }

    const groupTaskIds = new Set()
    let codeWriters = 0
    let readOnlyReviewers = 0
    for (const taskId of group.taskIds) {
      if (groupTaskIds.has(taskId)) {
        errors.push(`${label}.taskIds duplicates task: ${taskId}`)
      }
      groupTaskIds.add(taskId)

      const worker = workerByTaskId?.get(taskId)
      if (!worker) {
        errors.push(`${label}.taskIds references missing workerBoard task: ${taskId}`)
        continue
      }
      if (worker.mode === 'code-writing') {
        codeWriters += 1
      }
      if (READ_ONLY_MODES.has(worker.mode)) {
        readOnlyReviewers += 1
      }
    }
    if (Number.isInteger(group.maxCodeWriters) && codeWriters > group.maxCodeWriters) {
      errors.push(`${label} exceeds maxCodeWriters: ${codeWriters} code-writing tasks for limit ${group.maxCodeWriters}`)
    }
    if (Number.isInteger(group.maxReadOnlyReviewers) && readOnlyReviewers > group.maxReadOnlyReviewers) {
      errors.push(`${label} exceeds maxReadOnlyReviewers: ${readOnlyReviewers} read-only/review tasks for limit ${group.maxReadOnlyReviewers}`)
    }
  }
}

function validateEvidence(state, errors) {
  if (!Array.isArray(state.lastVerifiedEvidence)) {
    return
  }
  for (const [index, evidence] of state.lastVerifiedEvidence.entries()) {
    const label = `lastVerifiedEvidence[${index}]`
    if (!isObject(evidence)) {
      errors.push(`${label} must be an object`)
      continue
    }
    for (const field of ['command', 'result']) {
      if (!evidence[field]) {
        errors.push(`${label} missing required field: ${field}`)
      }
    }
  }
  validateCutoverEvidenceFreshness(state, errors)
}

function evidenceText(evidence) {
  if (!isObject(evidence)) {
    return ''
  }
  return `${evidence.command ?? ''} ${evidence.result ?? ''}`
}

function readinessText(readiness) {
  if (!isObject(readiness)) {
    return ''
  }
  return [
    readiness.level,
    readiness.gate,
    readiness.backendTarget,
    readiness.writeMode,
  ].filter(Boolean).join(' ')
}

function hasCutoverReadySignal(text) {
  return /cutoverReady[:= ]+true/i.test(text)
    || /cutover preflight.*ready/i.test(text)
}

function hasBlockersEmptySignal(text) {
  return /blockers\s*(?:empty|\[\]|0|: \[\])/i.test(text)
    || /no blockers/i.test(text)
}

function currentCutoverStateIsReady(state) {
  const currentText = [
    readinessText(state.readiness),
    state.lastEvent ?? '',
    ...state.lastVerifiedEvidence.slice(-3).map(evidenceText),
  ].join(' ')

  return hasCutoverReadySignal(currentText) && hasBlockersEmptySignal(currentText)
}

function staleCutoverEvidenceReasons(text) {
  const reasons = []
  if (/cutoverReady[:= ]+false/i.test(text)) {
    reasons.push('cutoverReady=false')
  }
  if (/\broute gap candidates?\b/i.test(text)) {
    reasons.push('route gap candidates')
  }
  if (/\bnext (?:preflight )?(?:top gap|clear preflight route batch|candidates?)\b/i.test(text)) {
    reasons.push('next route gap')
  }
  const routeMatches = text.match(/\broute:\/[^\s,;|)]+/gi) ?? []
  reasons.push(...routeMatches)
  if (/\bblockers?\s+remain\b/i.test(text)) {
    reasons.push('blockers remain')
  }
  return [...new Set(reasons)]
}

function validateCutoverEvidenceFreshness(state, errors) {
  if (!currentCutoverStateIsReady(state)) {
    return
  }
  for (const [index, evidence] of state.lastVerifiedEvidence.entries()) {
    const reasons = staleCutoverEvidenceReasons(evidenceText(evidence))
    if (reasons.length > 0) {
      errors.push(`stale contradictory lastVerifiedEvidence[${index}] after cutover-ready evidence: ${reasons.join(', ')}`)
    }
  }
}

function validateBridgeDeclaration(dispatch, label, errors) {
  const bridge = dispatch.bridgeDeclaration
  if (!isObject(bridge)) {
    errors.push(`${label} requires complete Bridge Declaration object`)
    for (const field of REQUIRED_BRIDGE_FIELDS) {
      errors.push(`${label}.bridgeDeclaration missing required field: ${field}`)
    }
    return
  }
  for (const field of REQUIRED_BRIDGE_FIELDS) {
    const value = bridge[field]
    if (Array.isArray(value) ? value.length === 0 : !value) {
      errors.push(`${label}.bridgeDeclaration missing required field: ${field}`)
    }
  }
}

function validateCoordinatorOwned(dispatch, label, errors) {
  for (const reserved of dispatch.exactReservedFiles) {
    for (const hardOwned of HARD_COORDINATOR_OWNED) {
      if (scopesOverlap(reserved, hardOwned)) {
        errors.push(`${label}: ${hardOwned} cannot be reserved for a worker`)
      }
    }
    for (const coordinatorOwned of COORDINATOR_OWNED_SCOPES) {
      if (!scopesOverlap(reserved, coordinatorOwned)) {
        continue
      }
      if (!arrayIncludesOverlap(dispatch.coordinatorOwnedExceptions, reserved)) {
        errors.push(`${label}: coordinator-owned scope ${reserved} requires coordinatorOwnedExceptions entry`)
      }
    }
  }
}

function validateDispatch(dispatch, index, workerByTaskId, errors) {
  const label = dispatch?.dispatchId ? `dispatch ${dispatch.dispatchId}` : `activeDispatches[${index}]`
  if (!isObject(dispatch)) {
    errors.push(`${label} must be an object`)
    return
  }
  for (const field of REQUIRED_DISPATCH_FIELDS) {
    if (!(field in dispatch)) {
      errors.push(`${label} missing required field: ${field}`)
    }
  }
  if (dispatch.taskId && workerByTaskId && !workerByTaskId.has(dispatch.taskId)) {
    errors.push(`${label} references taskId not present in workerBoard: ${dispatch.taskId}`)
  }
  const worker = dispatch.taskId && workerByTaskId?.get(dispatch.taskId)
  if (worker && isReservationActive(dispatch) && worker.status !== dispatch.status) {
    errors.push(`${label}: workerBoard status ${worker.status} must match active dispatch status ${dispatch.status}`)
  }
  if (dispatch.status && !KNOWN_STATUSES.has(dispatch.status)) {
    errors.push(`${label} has unknown status: ${dispatch.status}`)
  }
  if (dispatch.mode && !WRITE_MODES.has(dispatch.mode) && !READ_ONLY_MODES.has(dispatch.mode)) {
    errors.push(`${label} has unknown mode: ${dispatch.mode}`)
  }
  if (dispatch.status === 'RUNNING' && dispatch.mode === 'code-writing') {
    const workerName = String(dispatch.worker ?? '').trim()
    if (GENERIC_RUNNING_CODE_WORKERS.has(workerName)) {
      errors.push(`${label}: RUNNING code-writing dispatch must name the actual spawned worker id/nickname, or an inline fallback with an explicit reason`)
    }
    if (/^main-agent-inline\b/.test(workerName) && !/fallback reason:/i.test(workerName)) {
      errors.push(`${label}: inline code-writing fallback must include "fallback reason:" in worker`)
    }
  }
  if (dispatch.integrationTarget && !INTEGRATION_TARGETS.has(dispatch.integrationTarget)) {
    errors.push(`${label} has unknown integrationTarget: ${dispatch.integrationTarget}`)
  }
  if (!Array.isArray(dispatch.exactReservedFiles)) {
    errors.push(`${label}.exactReservedFiles must be an array`)
    return
  }
  if (!Array.isArray(dispatch.coordinatorOwnedExceptions)) {
    errors.push(`${label}.coordinatorOwnedExceptions must be an array`)
    return
  }
  if (WRITE_MODES.has(dispatch.mode) && dispatch.exactReservedFiles.length === 0) {
    errors.push(`${label}.exactReservedFiles must name at least one write scope`)
  }
  for (const reserved of dispatch.exactReservedFiles) {
    if (!reserved || typeof reserved !== 'string') {
      errors.push(`${label}.exactReservedFiles contains a non-string scope`)
      continue
    }
    if (hasPathTraversal(reserved)) {
      errors.push(`${label}: reserved scope ${reserved} must be relative and cannot contain ..`)
    }
    if (hasUnsupportedWildcard(reserved)) {
      errors.push(`${label}: reserved scope ${reserved} must be exact or end with /** without other wildcards`)
    }
  }
  for (const exception of dispatch.coordinatorOwnedExceptions) {
    if (hasPathTraversal(exception) || hasUnsupportedWildcard(exception)) {
      errors.push(`${label}: coordinatorOwnedExceptions entry ${exception} must be exact and relative`)
    }
  }
  if (dispatch.evidencePath && !String(dispatch.evidencePath).startsWith('docs/program-coordination/evidence/')) {
    errors.push(`${label}.evidencePath must live under docs/program-coordination/evidence/`)
  }
  validateCoordinatorOwned(dispatch, label, errors)

  const touchesOldEngine = dispatch.exactReservedFiles.some(scope => scopesOverlap(scope, 'backend/canvas-engine/**'))
  if (dispatch.integrationTarget === 'CURRENT_ENGINE_BRIDGE' || touchesOldEngine) {
    validateBridgeDeclaration(dispatch, label, errors)
  }
}

function validateOverlaps(activeDispatches, errors) {
  const writeDispatches = activeDispatches.filter(isWriteReservation)
  for (let i = 0; i < writeDispatches.length; i += 1) {
    for (let j = i + 1; j < writeDispatches.length; j += 1) {
      const left = writeDispatches[i]
      const right = writeDispatches[j]
      for (const leftScope of left.exactReservedFiles) {
        for (const rightScope of right.exactReservedFiles) {
          if (scopesOverlap(leftScope, rightScope)) {
            errors.push(
              `active reservations overlap: ${left.dispatchId} (${leftScope}) and ${right.dispatchId} (${rightScope})`,
            )
          }
        }
      }
    }
  }
}

function validateWorkerBoardActiveDispatches(state, errors) {
  if (!Array.isArray(state.workerBoard) || !Array.isArray(state.activeDispatches)) {
    return
  }
  for (const [index, worker] of state.workerBoard.entries()) {
    if (!isObject(worker) || !RESERVATION_STATUSES.has(worker.status)) {
      continue
    }
    const hasActiveDispatch = state.activeDispatches.some(dispatch =>
      isObject(dispatch)
        && dispatch.taskId === worker.taskId
        && isReservationActive(dispatch),
    )
    if (!hasActiveDispatch) {
      errors.push(`workerBoard[${index}] ${worker.taskId} is ${worker.status} but has no matching active dispatch`)
    }
  }
}

function validateRecoveryAudit(state, errors) {
  if (!isObject(state.recoveryAudit)) {
    return
  }
  if (!state.recoveryAudit.status) {
    errors.push('recoveryAudit missing required field: status')
  }
  if (typeof state.recoveryAudit.activeDispatches === 'number' && Array.isArray(state.activeDispatches)) {
    if (state.recoveryAudit.activeDispatches !== state.activeDispatches.length) {
      errors.push('recoveryAudit.activeDispatches must match activeDispatches.length')
    }
  }
  if (!Array.isArray(state.recoveryAudit.commandsToRunBeforeDispatch)) {
    errors.push('recoveryAudit.commandsToRunBeforeDispatch must be an array')
  } else if (!state.recoveryAudit.commandsToRunBeforeDispatch.some(command => String(command).includes('G0B'))) {
    errors.push('recoveryAudit.commandsToRunBeforeDispatch must include G0B backup gate before code-writing')
  }
}

function validateBackupManifest(rootDir, state, errors) {
  if (!Array.isArray(state.activeDispatches)) {
    return
  }
  if (!state.activeDispatches.some(requiresPreRewriteBackup)) {
    return
  }
  if (!existsSync(path.join(rootDir, BACKUP_MANIFEST_PATH))) {
    errors.push(`${BACKUP_MANIFEST_PATH} pre-rewrite backup manifest is required before active code-writing dispatches`)
  }
}

export function checkDispatchState(rootDir = process.cwd()) {
  const errors = []
  const root = path.resolve(rootDir)
  let state
  try {
    state = loadDispatchState(root)
  } catch (error) {
    return {
      ok: false,
      errors: [error.message],
    }
  }

  validateTopLevel(state, errors)
  const workerByTaskId = validateWorkerBoard(state, errors)
  validateParallelGroups(state, workerByTaskId, errors)
  validateEvidence(state, errors)
  validateRecoveryAudit(state, errors)
  validateBackupManifest(root, state, errors)

  if (Array.isArray(state.activeDispatches)) {
    for (const [index, dispatch] of state.activeDispatches.entries()) {
      validateDispatch(dispatch, index, workerByTaskId, errors)
    }
    validateOverlaps(state.activeDispatches, errors)
  }
  validateWorkerBoardActiveDispatches(state, errors)

  return {
    ok: errors.length === 0,
    errors,
  }
}

function printResult(result) {
  if (result.ok) {
    console.log(JSON.stringify({ ok: true }, null, 2))
    return
  }
  console.error(JSON.stringify({ ok: false, errors: result.errors }, null, 2))
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const result = checkDispatchState(process.argv[2] ?? process.cwd())
  printResult(result)
  process.exit(result.ok ? 0 : 1)
}
