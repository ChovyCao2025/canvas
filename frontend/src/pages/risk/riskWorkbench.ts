import type {
  RiskDecisionAction,
  RiskDecisionTraceView,
  RiskListEntryView,
  RiskListView,
  RiskSceneView,
  RiskSimulationView,
  RiskStrategyCommand,
  RiskStrategyVersionView,
  RiskStrategyView,
} from '../../services/riskApi'

export type RiskValidationStatus = 'IDLE' | 'STALE' | 'VALID' | 'INVALID'

export type RiskApprovalStatus = 'DRAFT' | 'BLOCKED' | 'SUBMITTED'

export type RiskWorkbenchTab = 'STUDIO' | 'LISTS' | 'SIMULATION' | 'TRACE'

export interface RiskStudioScene {
  sceneKey: string
  displayName: string
  latencyBudgetMs?: number
  status?: string
}

export interface RiskStudioStrategy {
  strategyKey: string
  sceneKey: string
  displayName: string
  riskLevel?: string | null
}

export interface RiskStudioVersion {
  version: number
  status: string
  submittedBy?: string | null
  approvedBy?: string | null
}

export interface RiskRuleGroupRow {
  groupKey: string
  displayName: string
  priority: number
  ruleCount: number
  status: string
}

export interface RiskRuleEditorState {
  open: boolean
  groupKey: string | null
}

export interface RiskDraftRule {
  sceneKey: string
  ruleKey: string
  displayName: string
  action: RiskDecisionAction
  expression: string
}

export interface RiskValidationState {
  status: RiskValidationStatus
  errors: string[]
}

export interface RiskApprovalState {
  status: RiskApprovalStatus
  reason?: string
}

export interface RiskRollbackState {
  targetVersion: number
  reason: string
}

export interface RiskListEntryDraft {
  listKey: string
  subjectType: string
  subjectKey: string
  action: RiskDecisionAction
  reason: string
}

export interface RiskListEntryRow {
  id?: number
  listKey: string
  subjectType: string
  maskedSubject: string
  action: RiskDecisionAction
  reason: string
}

export interface RiskSimulationDraft {
  simulationId: string
  baselineVersion: number
  candidateVersion: number
  sampleSize: number
  actionDistribution: Partial<Record<RiskDecisionAction, number>>
}

export interface RiskSimulationRow extends RiskSimulationDraft {
  status: string
}

export interface RiskDecisionTraceRow {
  traceId: string
  requestId: string
  action: RiskDecisionAction
  score: number
  latencyMs: number
}

export interface RiskStudioState {
  scenes: RiskStudioScene[]
  strategies: RiskStudioStrategy[]
  selectedSceneKey: string | null
  selectedStrategyKey: string | null
  activeVersion: number | null
  draftVersion: number | null
  versions: RiskStudioVersion[]
  activeTab: RiskWorkbenchTab
  ruleEditor: RiskRuleEditorState
  ruleGroups: RiskRuleGroupRow[]
  listEntries: RiskListEntryRow[]
  simulations: RiskSimulationRow[]
  traces: RiskDecisionTraceRow[]
  selectedTraceId: string | null
  draftRule: RiskDraftRule
  validation: RiskValidationState
  approval: RiskApprovalState
  rollback: RiskRollbackState | null
}

export interface RiskRemoteSnapshot {
  scenes: RiskSceneView[]
  strategies: RiskStrategyView[]
  versions: RiskStrategyVersionView[]
  lists: RiskListView[]
  listEntries: RiskListEntryView[]
  simulations: RiskSimulationView[]
  traces: RiskDecisionTraceView[]
}

export function applyRiskRemoteSnapshot(state: RiskStudioState, snapshot: RiskRemoteSnapshot): RiskStudioState {
  const scenes = snapshot.scenes.map(scene => ({
    sceneKey: scene.sceneKey,
    displayName: scene.displayName,
    latencyBudgetMs: scene.latencyBudgetMs,
    status: scene.status,
  }))
  const strategies = snapshot.strategies.map(strategy => ({
    strategyKey: strategy.strategyKey,
    sceneKey: strategy.sceneKey,
    displayName: strategy.name,
    riskLevel: strategy.riskLevel,
  }))
  const selectedSceneKey = preferredKey(
    state.selectedSceneKey,
    scenes.map(scene => scene.sceneKey),
  )
  const strategyCandidates = selectedSceneKey == null
    ? strategies
    : strategies.filter(strategy => strategy.sceneKey === selectedSceneKey)
  const selectedStrategyKey = preferredKey(
    state.selectedStrategyKey,
    strategyCandidates.length > 0
      ? strategyCandidates.map(strategy => strategy.strategyKey)
      : strategies.map(strategy => strategy.strategyKey),
  )
  const selectedStrategy = snapshot.strategies.find(strategy => strategy.strategyKey === selectedStrategyKey) ?? null
  const definitionVersion = snapshot.versions.find(version => version.version === selectedStrategy?.draftVersion)
    ?? snapshot.versions.find(version => version.version === selectedStrategy?.activeVersion)
    ?? snapshot.versions[0]
  const parsedDefinition = parseStrategyDefinition(definitionVersion?.definitionJson, selectedSceneKey)
  const listByKey = new Map(snapshot.lists.map(list => [list.listKey, list]))
  const traces = snapshot.traces.map(trace => ({
    traceId: trace.traceId,
    requestId: trace.requestId,
    action: trace.decision ?? 'REVIEW',
    score: trace.score ?? 0,
    latencyMs: trace.latencyMs ?? 0,
  }))

  return {
    ...state,
    scenes,
    strategies,
    selectedSceneKey,
    selectedStrategyKey,
    activeVersion: selectedStrategy?.activeVersion ?? null,
    draftVersion: selectedStrategy?.draftVersion ?? null,
    versions: snapshot.versions.map(version => ({
      version: version.version,
      status: version.status,
      submittedBy: version.submittedBy,
      approvedBy: version.approvedBy,
    })),
    ruleGroups: parsedDefinition?.ruleGroups ?? state.ruleGroups,
    listEntries: snapshot.listEntries.map(entry => {
      const list = listByKey.get(entry.listKey)
      return {
        id: entry.id,
        listKey: entry.listKey,
        subjectType: list?.subjectType ?? '',
        maskedSubject: entry.subjectMasked,
        action: actionForList(list?.listType),
        reason: entry.reason ?? '',
      }
    }),
    simulations: snapshot.simulations.map(simulation => ({
      simulationId: simulation.simulationId,
      baselineVersion: simulation.baselineVersion ?? 0,
      candidateVersion: simulation.candidateVersion ?? 0,
      sampleSize: simulation.sampleSize,
      actionDistribution: simulation.actionDistribution,
      status: simulation.status,
    })),
    traces,
    selectedTraceId: preferredKey(
      state.selectedTraceId,
      traces.map(trace => trace.traceId),
    ),
    draftRule: parsedDefinition?.draftRule ?? {
      ...state.draftRule,
      sceneKey: selectedSceneKey ?? '',
    },
  }
}

export function buildRiskStrategyDraftCommand(state: RiskStudioState): RiskStrategyCommand | null {
  if (state.selectedSceneKey == null || state.draftRule.ruleKey.trim() === '') {
    return null
  }
  const strategy = state.strategies.find(item => item.strategyKey === state.selectedStrategyKey)
  const scene = state.scenes.find(item => item.sceneKey === state.selectedSceneKey)
  const strategyKey = strategy?.strategyKey ?? `${normalizeKey(state.selectedSceneKey)}_default`
  const group = state.ruleGroups[0] ?? {
    groupKey: 'default',
    priority: 0,
    status: 'ENABLED',
  }
  const rule = {
    ruleKey: state.draftRule.ruleKey.trim(),
    priority: group.priority,
    mode: 'ENFORCE',
    action: state.draftRule.action,
    scoreDelta: scoreDeltaForAction(state.draftRule.action),
    reasonCode: state.draftRule.ruleKey.trim(),
    dslJson: state.draftRule.expression,
    labels: [state.selectedSceneKey],
  }
  const definition = {
    mode: 'ENFORCE',
    trafficPercent: 100,
    failPolicy: state.draftRule.action === 'BLOCK' ? 'FAIL_CLOSED' : 'FAIL_REVIEW',
    latencyBudgetMs: scene?.latencyBudgetMs ?? 50,
    groups: [
      {
        groupKey: group.groupKey,
        groupType: 'HARD_RULE',
        executionOrder: group.priority,
        matchPolicy: 'ANY_MATCHED',
        enabled: group.status !== 'DISABLED',
        rules: [rule],
      },
    ],
  }

  return {
    sceneKey: state.selectedSceneKey,
    strategyKey,
    name: strategy?.displayName ?? `${scene?.displayName ?? state.selectedSceneKey} 默认策略`,
    riskLevel: strategy?.riskLevel ?? riskLevelForAction(state.draftRule.action),
    definitionJson: JSON.stringify(definition),
  }
}

export function selectRiskWorkbenchTab(state: RiskStudioState, activeTab: RiskWorkbenchTab): RiskStudioState {
  return {
    ...state,
    activeTab,
  }
}

export function selectRiskScene(state: RiskStudioState, sceneKey: string): RiskStudioState {
  const selectedStrategy = state.strategies.find(strategy => strategy.sceneKey === sceneKey) ?? null
  return {
    ...state,
    selectedSceneKey: sceneKey,
    selectedStrategyKey: selectedStrategy?.strategyKey ?? null,
    draftRule: {
      ...state.draftRule,
      sceneKey,
    },
    validation: {
      status: 'IDLE',
      errors: [],
    },
    approval: {
      status: 'DRAFT',
    },
  }
}

export function openRuleEditor(state: RiskStudioState, groupKey: string | null = null): RiskStudioState {
  return {
    ...state,
    ruleEditor: {
      open: true,
      groupKey,
    },
  }
}

export function saveRuleGroup(state: RiskStudioState, row: RiskRuleGroupRow): RiskStudioState {
  const exists = state.ruleGroups.some(item => item.groupKey === row.groupKey)
  return {
    ...state,
    ruleEditor: {
      open: false,
      groupKey: null,
    },
    ruleGroups: exists
      ? state.ruleGroups.map(item => item.groupKey === row.groupKey ? row : item)
      : [...state.ruleGroups, row],
  }
}

export function editDraftRule(state: RiskStudioState, patch: Partial<RiskDraftRule>): RiskStudioState {
  return {
    ...state,
    draftRule: {
      ...state.draftRule,
      ...patch,
    },
    validation: {
      status: 'STALE',
      errors: [],
    },
    approval: {
      status: 'DRAFT',
    },
  }
}

export function validateDraftRule(state: RiskStudioState): RiskStudioState {
  const errors = [
    requiredError(state.draftRule.ruleKey, '规则 Key 必填'),
    requiredError(state.draftRule.displayName, '规则名称必填'),
    requiredError(state.draftRule.expression, '规则条件必填'),
  ].filter((error): error is string => Boolean(error))

  return {
    ...state,
    validation: {
      status: errors.length > 0 ? 'INVALID' : 'VALID',
      errors,
    },
  }
}

export function addRiskListEntry(state: RiskStudioState, draft: RiskListEntryDraft): RiskStudioState {
  return {
    ...state,
    activeTab: 'LISTS',
    listEntries: [
      {
        listKey: draft.listKey,
        subjectType: draft.subjectType,
        maskedSubject: maskSubject(draft.subjectKey),
        action: draft.action,
        reason: draft.reason,
      },
      ...state.listEntries,
    ],
  }
}

export function startLocalSimulation(state: RiskStudioState, draft: RiskSimulationDraft): RiskStudioState {
  return {
    ...state,
    activeTab: 'SIMULATION',
    simulations: [
      {
        ...draft,
        status: 'RUNNING',
      },
      ...state.simulations,
    ],
  }
}

export function selectRiskTrace(state: RiskStudioState, traceId: string): RiskStudioState {
  return {
    ...state,
    activeTab: 'TRACE',
    selectedTraceId: traceId,
  }
}

export function submitStrategyApproval(state: RiskStudioState, reason: string): RiskStudioState {
  if (state.validation.status !== 'VALID') {
    return {
      ...state,
      approval: {
        status: 'BLOCKED',
        reason: '请先通过规则校验',
      },
    }
  }

  return {
    ...state,
    approval: {
      status: 'SUBMITTED',
      reason,
    },
  }
}

export function activateStrategyVersion(state: RiskStudioState, version: number): RiskStudioState {
  return {
    ...state,
    activeVersion: version,
    versions: state.versions.map(item => ({
      ...item,
      status: item.version === version ? 'ACTIVE' : item.status === 'ACTIVE' ? 'SUPERSEDED' : item.status,
    })),
  }
}

export function rollbackStrategyVersion(state: RiskStudioState, targetVersion: number, reason: string): RiskStudioState {
  const targetExists = state.versions.some(item => item.version === targetVersion)
  if (!targetExists || state.activeVersion === targetVersion) {
    return state
  }

  return {
    ...state,
    activeVersion: targetVersion,
    rollback: {
      targetVersion,
      reason,
    },
    versions: state.versions.map(item => ({
      ...item,
      status: item.version === targetVersion ? 'ACTIVE' : item.status === 'ACTIVE' ? 'SUPERSEDED' : item.status,
    })),
  }
}

function requiredError(value: string | null | undefined, message: string): string | null {
  return value == null || value.trim() === '' ? message : null
}

function maskSubject(subjectKey: string): string {
  const trimmed = subjectKey.trim()
  if (trimmed.length <= 4) return '****'
  return `${trimmed.slice(0, 2)}***${trimmed.slice(-2)}`
}

function preferredKey<T extends string | null>(current: T, candidates: string[]): string | null {
  if (current != null && candidates.includes(current)) {
    return current
  }
  return candidates[0] ?? null
}

function actionForList(listType: string | undefined): RiskDecisionAction {
  if (listType === 'BLACK') return 'BLOCK'
  if (listType === 'WHITE') return 'ALLOW'
  return 'REVIEW'
}

function normalizeKey(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'risk_strategy'
}

function scoreDeltaForAction(action: RiskDecisionAction): number {
  if (action === 'BLOCK') return 100
  if (action === 'LIMIT' || action === 'VERIFY') return 70
  if (action === 'REVIEW') return 50
  if (action === 'DELAY') return 30
  return 0
}

function riskLevelForAction(action: RiskDecisionAction): string {
  if (action === 'BLOCK' || action === 'LIMIT') return 'HIGH'
  if (action === 'VERIFY' || action === 'REVIEW') return 'MEDIUM'
  return 'LOW'
}

function parseStrategyDefinition(
  definitionJson: unknown,
  sceneKey: string | null,
): { ruleGroups: RiskRuleGroupRow[], draftRule?: RiskDraftRule } | null {
  const root = parseJsonObject(definitionJson)
  if (root == null || !Array.isArray(root.groups)) {
    return null
  }
  const groups = root.groups.filter(isRecord)
  const ruleGroups = groups.map((group, index) => {
    const rules = Array.isArray(group.rules) ? group.rules : []
    const groupKey = text(group.groupKey, `group-${index}`)
    return {
      groupKey,
      displayName: text(group.displayName, groupKey),
      priority: number(group.executionOrder, index),
      ruleCount: rules.length,
      status: group.enabled === false ? 'DISABLED' : 'ENABLED',
    }
  })
  const firstGroup = groups[0]
  const firstRule = Array.isArray(firstGroup?.rules)
    ? firstGroup.rules.find(isRecord)
    : null
  const draftRule = firstRule == null ? undefined : {
    sceneKey: sceneKey ?? '',
    ruleKey: text(firstRule.ruleKey, ''),
    displayName: text(firstRule.displayName, text(firstRule.ruleKey, '')),
    action: action(firstRule.action),
    expression: expression(firstRule),
  }
  return { ruleGroups, draftRule }
}

function parseJsonObject(value: unknown): Record<string, unknown> | null {
  if (typeof value === 'string') {
    try {
      const parsed: unknown = JSON.parse(value)
      return isRecord(parsed) ? parsed : null
    } catch {
      return null
    }
  }
  return isRecord(value) ? value : null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value != null && !Array.isArray(value)
}

function text(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.trim() !== '' ? value : fallback
}

function number(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function action(value: unknown): RiskDecisionAction {
  const candidate = text(value, 'REVIEW')
  return ['ALLOW', 'REVIEW', 'VERIFY', 'BLOCK', 'LIMIT', 'DELAY', 'SHADOW_ONLY'].includes(candidate)
    ? candidate as RiskDecisionAction
    : 'REVIEW'
}

function expression(rule: Record<string, unknown>): string {
  if (typeof rule.dslJson === 'string') {
    return rule.dslJson
  }
  if (rule.dsl != null) {
    return JSON.stringify(rule.dsl)
  }
  return ''
}
