import http from './api'
import type { R } from '../types'

export type RiskStrategyMode = 'SHADOW' | 'ENFORCE'

export type RiskDecisionAction = 'ALLOW' | 'REVIEW' | 'VERIFY' | 'BLOCK' | 'LIMIT' | 'DELAY' | 'SHADOW_ONLY'

export type RiskBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface RiskSceneView {
  sceneKey: string
  displayName: string
  description?: string | null
  latencyBudgetMs?: number
  status?: string
}

export interface RiskRuleSnapshot {
  ruleKey: string
  displayName?: string
  enabled?: boolean
  priority?: number
  action?: RiskDecisionAction
  score?: number
  condition?: unknown
  metadata?: Record<string, unknown>
}

export interface RiskStrategySnapshot {
  strategyKey: string
  version: number
  mode: RiskStrategyMode
  defaultAction: RiskDecisionAction
  defaultScore: number
  rules: RiskRuleSnapshot[]
  metadata?: Record<string, unknown>
}

export interface RiskStrategyCommand {
  sceneKey: string
  strategyKey: string
  name: string
  riskLevel: string
  definitionJson: string
}

export interface RiskStrategyTransitionRequest {
  targetVersion?: number
  reason?: string
}

export interface RiskStrategyVersionView {
  tenantId?: number | null
  strategyKey: string
  version: number
  status: string
  definitionJson?: string | null
  validationJson?: string | null
  createdBy?: string | null
  submittedBy?: string | null
  approvedBy?: string | null
}

export interface RiskStrategyView {
  tenantId?: number | null
  sceneKey: string
  strategyKey: string
  name: string
  status: string
  activeVersion?: number | null
  draftVersion?: number | null
  riskLevel?: string | null
  owner?: string | null
}

export interface RiskStrategyDiffView {
  strategyKey: string
  leftVersion: number
  rightVersion: number
  changes: unknown[]
}

export interface RiskDecisionEvaluateRequest {
  tenantId?: number | null
  requestId: string
  sceneKey: string
  subject: Record<string, unknown>
  eventTime: string
  event: Record<string, unknown>
  context?: Record<string, unknown>
  features?: Record<string, unknown>
  options?: {
    deadlineMs?: number
    includeTrace?: boolean
    modeOverride?: RiskStrategyMode
  }
}

export interface RiskDecisionEvaluateResponse {
  requestId: string
  decisionRunId?: string | null
  sceneKey: string
  strategyKey?: string | null
  strategyVersion?: number | null
  mode?: RiskStrategyMode | null
  decision: RiskDecisionAction
  score: number
  riskBand: RiskBand
  reasons: unknown[]
  matchedRules: unknown[]
  labels: string[]
  missingFeatures: string[]
  traceAvailable: boolean
  latencyMs: number
}

export interface RiskListCommand {
  listKey: string
  listType: string
  subjectType: string
  requiresApproval: boolean
}

export interface RiskListEntryCommand {
  subjectType: string
  rawSubject: string
  reason?: string | null
  source?: string | null
  effectiveFrom?: string | null
  expiresAt?: string | null
}

export interface RiskListImportCommand {
  entries: RiskListEntryCommand[]
  replaceExisting?: boolean
}

export interface RiskListView {
  tenantId?: number | null
  listKey: string
  listType?: string
  subjectType: string
  requiresApproval?: boolean
  entryCount?: number
  status?: string
}

export interface RiskListEntryView {
  id: number
  tenantId?: number | null
  listKey: string
  subjectHash: string
  subjectMasked: string
  reason?: string | null
  source?: string | null
  effectiveFrom?: string | null
  expiresAt?: string | null
  createdBy?: string | null
}

export interface RiskListImportResult {
  importedCount: number
  skippedCount: number
}

export interface RiskSimulationRequest {
  sceneKey: string
  strategyKey: string
  version: number
  sampleWindow: {
    startTime: string
    endTime: string
  }
  sampleLimit: number
  filters?: Record<string, unknown>
}

export interface RiskSimulationView {
  simulationId: string
  sceneKey?: string
  strategyKey?: string
  baselineVersion?: number
  candidateVersion?: number
  status: string
  sampleSize: number
  actionDistribution: Partial<Record<RiskDecisionAction, number>>
  changedActionCount?: number
  actionChanges?: Record<string, number>
  createdAt?: string | null
}

export interface RiskDecisionTraceView {
  traceId: string
  requestId: string
  sceneKey: string
  strategyKey?: string | null
  strategyVersion?: number | null
  mode?: RiskStrategyMode | null
  decision?: RiskDecisionAction | null
  score?: number | null
  riskBand?: RiskBand | null
  reasons?: unknown[]
  matchedRules?: unknown[]
  labels?: string[]
  latencyMs?: number | null
  createdAt?: string | null
}

export const riskApi = {
  listScenes: () =>
    http.get<R<RiskSceneView[]>, R<RiskSceneView[]>>('/canvas/risk/scenes'),

  listStrategies: (sceneKey: string) =>
    http.get<R<RiskStrategyView[]>, R<RiskStrategyView[]>>(
      `/canvas/risk/strategies?sceneKey=${encodeURIComponent(sceneKey)}`,
    ),

  getStrategy: (strategyKey: string) =>
    http.get<R<RiskStrategyView>, R<RiskStrategyView>>(
      `/canvas/risk/strategies/${encodeURIComponent(strategyKey)}`,
    ),

  listStrategyVersions: (strategyKey: string) =>
    http.get<R<RiskStrategyVersionView[]>, R<RiskStrategyVersionView[]>>(
      `/canvas/risk/strategies/${encodeURIComponent(strategyKey)}/versions`,
    ),

  pauseStrategy: (strategyKey: string) =>
    http.post<R<RiskStrategyView>, R<RiskStrategyView>>(
      `/canvas/risk/strategies/${encodeURIComponent(strategyKey)}/pause`,
      {},
    ),

  createStrategyDraft: (payload: RiskStrategyCommand) =>
    http.post<R<RiskStrategyView>, R<RiskStrategyView>>('/canvas/risk/strategies', payload),

  validateStrategyVersion: (strategyKey: string, version: number) =>
    http.post<R<RiskStrategyVersionView>, R<RiskStrategyVersionView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${version}/validate`,
      {},
    ),

  simulateStrategyVersion: (strategyKey: string, version: number, payload: RiskStrategyTransitionRequest = {}) =>
    http.post<R<RiskStrategyVersionView>, R<RiskStrategyVersionView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${version}/simulate`,
      payload,
    ),

  submitStrategyVersion: (strategyKey: string, version: number, payload: RiskStrategyTransitionRequest = {}) =>
    http.post<R<RiskStrategyVersionView>, R<RiskStrategyVersionView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${version}/submit`,
      payload,
    ),

  approveStrategyVersion: (strategyKey: string, version: number, payload: RiskStrategyTransitionRequest = {}) =>
    http.post<R<RiskStrategyVersionView>, R<RiskStrategyVersionView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${version}/approve`,
      payload,
    ),

  activateStrategyVersion: (strategyKey: string, version: number, payload: RiskStrategyTransitionRequest = {}) =>
    http.post<R<RiskStrategyView>, R<RiskStrategyView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${version}/activate`,
      payload,
    ),

  rollbackStrategy: (strategyKey: string, payload: RiskStrategyTransitionRequest) =>
    http.post<R<RiskStrategyView>, R<RiskStrategyView>>(`/canvas/risk/strategies/${strategyKey}/rollback`, payload),

  diffStrategyVersions: (strategyKey: string, leftVersion: number, rightVersion: number) =>
    http.get<R<RiskStrategyDiffView>, R<RiskStrategyDiffView>>(
      `/canvas/risk/strategies/${strategyKey}/versions/${leftVersion}/diff/${rightVersion}`,
    ),

  evaluateDecision: (payload: RiskDecisionEvaluateRequest) =>
    http.post<R<RiskDecisionEvaluateResponse>, R<RiskDecisionEvaluateResponse>>(
      '/canvas/risk/decisions/evaluate',
      payload,
    ),

  listDecisionTraces: (sceneKey: string, limit: number) =>
    http.get<R<RiskDecisionTraceView[]>, R<RiskDecisionTraceView[]>>(
      `/canvas/risk/decisions/traces?sceneKey=${encodeURIComponent(sceneKey)}&limit=${limit}`,
    ),

  listLists: () =>
    http.get<R<RiskListView[]>, R<RiskListView[]>>('/canvas/risk/lists'),

  createList: (payload: RiskListCommand) =>
    http.post<R<RiskListView>, R<RiskListView>>('/canvas/risk/lists', payload),

  listListEntries: (listKey: string) =>
    http.get<R<RiskListEntryView[]>, R<RiskListEntryView[]>>(
      `/canvas/risk/lists/${encodeURIComponent(listKey)}/entries`,
    ),

  addListEntry: (listKey: string, payload: RiskListEntryCommand) =>
    http.post<R<RiskListEntryView>, R<RiskListEntryView>>(
      `/canvas/risk/lists/${encodeURIComponent(listKey)}/entries`,
      payload,
    ),

  importListEntries: (listKey: string, payload: RiskListImportCommand) =>
    http.post<R<RiskListImportResult>, R<RiskListImportResult>>(
      `/canvas/risk/lists/${listKey}/entries/import`,
      payload,
    ),

  deleteListEntry: (listKey: string, entryId: number) =>
    http.delete<R<void>, R<void>>(`/canvas/risk/lists/${encodeURIComponent(listKey)}/entries/${entryId}`),

  startSimulation: (payload: RiskSimulationRequest) =>
    http.post<R<RiskSimulationView>, R<RiskSimulationView>>('/canvas/risk/lab/simulations', payload),

  listSimulations: (sceneKey: string, limit: number) =>
    http.get<R<RiskSimulationView[]>, R<RiskSimulationView[]>>(
      `/canvas/risk/lab/simulations?sceneKey=${encodeURIComponent(sceneKey)}&limit=${limit}`,
    ),
}
