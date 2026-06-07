export interface SearchMarketingSource {
  id: number
  tenantId: number
  provider: string
  sourceKey: string
  displayName: string
  channel: string
  externalAccountId?: string | null
  siteUrl?: string | null
  currency?: string | null
  timezone?: string | null
  enabled: boolean
  metadata?: Record<string, unknown>
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingKeyword {
  id: number
  tenantId: number
  channel: string
  keywordText: string
  keywordKey: string
  matchType?: string | null
  landingPageUrl?: string | null
  landingPageUrlHash?: string | null
  searchIntent?: string | null
  labels: string[]
  status: string
  metadata?: Record<string, unknown>
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingSnapshot {
  id: number
  tenantId: number
  sourceId: number
  keywordId?: number | null
  channel: string
  snapshotDate: string
  device?: string | null
  country?: string | null
  queryGroupKey?: string | null
  impressionCount: number
  clickCount: number
  costAmount: number
  conversionCount: number
  revenueAmount: number
  averagePosition?: number | null
  metadata?: Record<string, unknown>
  evidence?: Record<string, unknown>
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingOpportunity {
  id: number
  tenantId: number
  sourceId: number
  keywordId?: number | null
  channel: string
  opportunityType: string
  snapshotDate: string
  severity: string
  status: string
  recommendation: string
  impactScore?: number | null
  score?: number | null
  evidence: Record<string, unknown>
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingMutation {
  id: number
  tenantId?: number
  sourceId?: number | null
  opportunityId?: number | null
  keywordId?: number | null
  provider: string
  channel?: string | null
  mutationKey: string
  mutationType: string
  entityType: string
  externalEntityId?: string | null
  requestHash: string
  idempotencyKey: string
  status: string
  approvalStatus: string
  dryRunRequired?: boolean | null
  payload?: Record<string, unknown>
  validation?: Record<string, unknown>
  dryRunResult?: Record<string, unknown>
  providerResult?: Record<string, unknown>
  providerRequest?: Record<string, unknown>
  providerResponse?: Record<string, unknown>
  errorCode?: string | null
  errorMessage?: string | null
  createdBy?: string | null
  approvedBy?: string | null
  approvedAt?: string | null
  executedBy?: string | null
  executedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingSyncRun {
  id: number
  tenantId: number
  sourceId: number
  runType: string
  provider: string
  channel: string
  idempotencyKey: string
  windowStart?: string | null
  windowEnd?: string | null
  cursorValue?: string | null
  status: string
  retryable: boolean
  requestedCount: number
  successCount: number
  failedCount: number
  providerRequestId?: string | null
  errorCode?: string | null
  errorMessage?: string | null
  evidence: Record<string, unknown>
  createdBy?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingUrlInspection {
  id: number
  tenantId: number
  sourceId: number
  provider: string
  pageUrl: string
  pageUrlHash: string
  inspectionDate: string
  indexedState: string
  crawlState?: string | null
  canonicalUrl?: string | null
  sitemapState?: string | null
  mobileUsabilityState?: string | null
  lastCrawlAt?: string | null
  evidence: Record<string, unknown>
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingProviderChange {
  id: number
  tenantId: number
  sourceId?: number | null
  mutationId?: number | null
  provider: string
  externalResourceId?: string | null
  changeType: string
  changedFields: Record<string, unknown>
  providerActor?: string | null
  providerChangedAt?: string | null
  reconciliationStatus: string
  evidence: Record<string, unknown>
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingReconciliation {
  tenantId: number
  mutationId: number
  providerChangeId?: number | null
  status: string
  providerOperationId?: string | null
  evidence: Record<string, unknown>
  reconciledAt: string
}

export interface SearchMarketingImpactWindow {
  id: number
  tenantId: number
  opportunityId: number
  mutationId: number
  sourceId?: number | null
  keywordId?: number | null
  pageUrlHash?: string | null
  baselineStartDate: string
  baselineEndDate: string
  postStartDate: string
  postEndDate: string
  status: string
  decision?: string | null
  confidence?: number | null
  metricDeltas: Record<string, unknown>
  evidence: Record<string, unknown>
  dueAt?: string | null
  evaluatedAt?: string | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SearchMarketingReadiness {
  tenantId: number
  status: string
  blockers: string[]
  evidence: Record<string, unknown>
  evaluatedAt: string
}

export interface SearchMarketingSourceQuery {
  provider?: string
  channel?: string
  enabled?: boolean
  limit?: number
}

export interface SearchMarketingKeywordQuery {
  channel?: string
  status?: string
  limit?: number
}

export interface SearchMarketingSnapshotQuery {
  channel?: string
  sourceId?: number
  keywordId?: number
  startDate?: string
  endDate?: string
  limit?: number
}

export interface SearchMarketingOpportunityQuery {
  channel?: string
  sourceId?: number
  status?: string
  severity?: string
  limit?: number
}

export interface SearchMarketingMutationQuery {
  sourceId?: number
  status?: string
  approvalStatus?: string
  limit?: number
}

export interface SearchMarketingSyncRunQuery {
  sourceId?: number
  runType?: string
  status?: string
  limit?: number
}

export interface SearchMarketingUrlInspectionQuery {
  sourceId?: number
  indexedState?: string
  startDate?: string
  endDate?: string
  limit?: number
}

export interface SearchMarketingProviderChangeQuery {
  sourceId?: number
  mutationId?: number
  provider?: string
  reconciliationStatus?: string
  limit?: number
}

export interface SearchMarketingImpactWindowQuery {
  opportunityId?: number
  mutationId?: number
  sourceId?: number
  status?: string
  decision?: string
  limit?: number
}

export interface SearchMarketingSyncRequest {
  runType?: string
  windowStart?: string
  windowEnd?: string
  cursorValue?: string
}

export interface SearchMarketingSyncDueRequest {
  limit?: number
}

export interface SearchMarketingOpportunityMutationCommand {
  mutationKey: string
  mutationType: string
  entityType: string
  externalEntityId?: string
  dryRunRequired?: boolean
  idempotencyKey?: string
  payload?: Record<string, unknown>
}

export interface SearchMarketingMutationCommand extends SearchMarketingOpportunityMutationCommand {
  sourceId: number
  opportunityId?: number
  keywordId?: number
}

export interface SearchMarketingOpportunityStatusCommand {
  status: string
  reason?: string
}

export interface SearchMarketingMutationApprovalCommand {
  decision: 'APPROVED' | 'REJECTED'
  reason?: string
}

export interface SearchMarketingMutationExecuteCommand {
  dryRun?: boolean
  partialFailure?: boolean
  metadata?: Record<string, unknown>
}

export interface SearchMarketingKpiInput {
  snapshots?: SearchMarketingSnapshot[]
  opportunities?: SearchMarketingOpportunity[]
  mutations?: SearchMarketingMutation[]
}

export interface SearchMarketingKpis {
  seoClicks: number
  semSpend: number
  conversions: number
  roas: number
  openOpportunities: number
  failedWrites: number
  unreconciledWrites: number
}

export interface SearchMarketingActionState {
  canApprove: boolean
  canDryRun: boolean
  canApply: boolean
}

export function readinessStatusView(status: string) {
  switch (normalize(status)) {
    case 'LIVE':
      return { text: '生产就绪', color: 'green' }
    case 'DEGRADED':
      return { text: '降级', color: 'gold' }
    case 'BLOCKED':
      return { text: '阻断', color: 'red' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

export function syncRunStatusView(status: string) {
  switch (normalize(status)) {
    case 'SUCCEEDED':
      return { text: '成功', color: 'green' }
    case 'RUNNING':
      return { text: '运行中', color: 'blue' }
    case 'FAILED':
      return { text: '失败', color: 'red' }
    case 'PARTIAL':
      return { text: '部分成功', color: 'gold' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

export function opportunitySeverityView(severity: string) {
  switch (normalize(severity)) {
    case 'CRITICAL':
      return { text: '严重', color: 'red' }
    case 'HIGH':
      return { text: '高', color: 'red' }
    case 'MEDIUM':
      return { text: '中', color: 'gold' }
    case 'LOW':
      return { text: '低', color: 'blue' }
    default:
      return { text: severity || '未知', color: 'default' }
  }
}

export function opportunityStatusView(status: string) {
  switch (normalize(status)) {
    case 'OPEN':
      return { text: '待处理', color: 'gold' }
    case 'ACCEPTED':
      return { text: '已接受', color: 'blue' }
    case 'MUTED':
      return { text: '已静默', color: 'default' }
    case 'CLOSED':
      return { text: '已关闭', color: 'default' }
    case 'IMPACT_POSITIVE':
      return { text: '正向影响', color: 'green' }
    case 'IMPACT_NEUTRAL':
      return { text: '影响中性', color: 'default' }
    case 'IMPACT_NEGATIVE':
      return { text: '负向影响', color: 'orange' }
    case 'ROLLBACK_REQUIRED':
      return { text: '需要回滚', color: 'red' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

export function mutationActionState(item: SearchMarketingMutation): SearchMarketingActionState {
  const approved = item.approvalStatus === 'APPROVED'
  const terminal = ['APPLIED', 'CANCELLED', 'RECONCILED'].includes(item.status)
  return {
    canApprove: item.approvalStatus === 'PENDING',
    canDryRun: approved && !terminal,
    canApply: approved && item.status === 'DRY_RUN_OK',
  }
}

export function canDryRunMutation(item: SearchMarketingMutation) {
  return mutationActionState(item).canDryRun
}

export function canApplyMutation(item: SearchMarketingMutation, readinessStatus: string) {
  if (normalize(readinessStatus) !== 'LIVE' || item.approvalStatus !== 'APPROVED') {
    return false
  }
  if (item.dryRunRequired === false && item.status === 'READY') {
    return true
  }
  return item.status === 'DRY_RUN_OK'
}

export function sanitizeSearchMarketingEvidence<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map(item => sanitizeSearchMarketingEvidence(item)) as T
  }
  if (!isRecord(value)) {
    return value
  }
  return Object.fromEntries(
    Object.entries(value).map(([key, entry]) => [
      key,
      isSecretKey(key) ? '[REDACTED]' : sanitizeSearchMarketingEvidence(entry),
    ]),
  ) as T
}

export const redactSearchMarketingSecrets = sanitizeSearchMarketingEvidence

export function calculateSearchMarketingKpis(input: SearchMarketingKpiInput): SearchMarketingKpis {
  const snapshots = input.snapshots ?? []
  const opportunities = input.opportunities ?? []
  const mutations = input.mutations ?? []
  const semSpend = snapshots
    .filter(snapshot => normalize(snapshot.channel) === 'SEM')
    .reduce((total, snapshot) => total + numberValue(snapshot.costAmount), 0)
  const revenue = snapshots.reduce((total, snapshot) => total + numberValue(snapshot.revenueAmount), 0)
  return {
    seoClicks: snapshots
      .filter(snapshot => normalize(snapshot.channel) === 'SEO')
      .reduce((total, snapshot) => total + numberValue(snapshot.clickCount), 0),
    semSpend: round2(semSpend),
    conversions: snapshots.reduce((total, snapshot) => total + numberValue(snapshot.conversionCount), 0),
    roas: semSpend <= 0 ? 0 : round2(revenue / semSpend),
    openOpportunities: opportunities
      .filter(item => ['OPEN', 'ACCEPTED'].includes(normalize(item.status)))
      .length,
    failedWrites: mutations
      .filter(item => ['FAILED', 'DRY_RUN_FAILED', 'RECONCILE_FAILED'].includes(normalize(item.status)))
      .length,
    unreconciledWrites: mutations.filter(item => normalize(item.status) === 'APPLIED').length,
  }
}

function normalize(value?: string | null) {
  return (value ?? '').trim().toUpperCase()
}

function numberValue(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

function round2(value: number) {
  return Math.round(value * 100) / 100
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

function isSecretKey(key: string) {
  const normalized = key.replace(/[^a-zA-Z0-9]/g, '').toLowerCase()
  return normalized === 'token'
    || normalized === 'accesstoken'
    || normalized === 'refreshtoken'
    || normalized === 'developertoken'
    || normalized === 'clientsecret'
    || normalized === 'apikey'
    || normalized === 'password'
    || normalized.endsWith('secret')
}
