export interface MarketingPlatformEvidenceSignal {
  signalKey: string
  label: string
  value: number
  status: string
}

export interface MarketingPlatformCapability {
  capabilityKey: string
  displayName: string
  domain: string
  status: string
  route: string
  apiRoot: string
  surface: string
  productionSignals: string[]
  gaps: string[]
  evidence: MarketingPlatformEvidenceSignal[]
}

export interface MarketingPlatformIntegrationLane {
  laneKey: string
  displayName: string
  sourceCapabilityKey: string
  targetCapabilityKey: string
  status: string
  controls: string[]
}

export interface MarketingPlatformIntegrationAsset {
  assetKey: string
  displayName: string
  assetType: string
  ownerCapabilityKey: string
  providerFamily: string
  status: string
  apiRoot: string
  credentialDependency: string
  pendingWrites: number
  failedWrites: number
  controls: string[]
  gaps: string[]
  evidence: MarketingPlatformEvidenceSignal[]
}

export interface MarketingPlatformActionItem {
  priority: string
  capabilityKey: string
  title: string
  route: string
  reason: string
}

export interface MarketingPlatformReadinessFinding {
  severity: string
  itemType: string
  itemKey: string
  title: string
  route: string
  reason: string
}

export interface MarketingPlatformReadinessGate {
  status: string
  productionReady: boolean
  blockerCount: number
  warningCount: number
  blockers: MarketingPlatformReadinessFinding[]
  warnings: MarketingPlatformReadinessFinding[]
}

export interface MarketingPlatformControlPlaneSummary {
  tenantId: number
  generatedAt: string
  overallStatus: string
  capabilityCount: number
  liveCapabilityCount: number
  actionItemCount: number
  capabilities: MarketingPlatformCapability[]
  integrationLanes: MarketingPlatformIntegrationLane[]
  integrationAssets: MarketingPlatformIntegrationAsset[]
  readinessGate: MarketingPlatformReadinessGate
  actionItems: MarketingPlatformActionItem[]
}

export interface MarketingCampaign {
  id: number
  tenantId: number
  campaignKey: string
  campaignName: string
  objective: string
  status: string
  primaryChannel?: string | null
  ownerTeam?: string | null
  startAt?: string | null
  endAt?: string | null
  budgetAmount: number
  currency: string
  brief: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MarketingCampaignCommand {
  campaignKey: string
  campaignName: string
  objective?: string
  status?: string
  primaryChannel?: string
  ownerTeam?: string
  startAt?: string
  endAt?: string
  budgetAmount?: number
  currency?: string
  brief?: Record<string, unknown>
}

export interface MarketingCampaignQuery {
  status?: string
  limit?: number
}

export interface MarketingCampaignLink {
  id: number
  tenantId: number
  campaignId: number
  resourceType: string
  resourceId?: number | null
  resourceKey: string
  resourceName?: string | null
  resourceRoute?: string | null
  dependencyRole: string
  linkStatus: string
  requiredForLaunch: boolean
  metadata: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MarketingCampaignLinkCommand {
  campaignId: number
  resourceType: string
  resourceId?: number
  resourceKey: string
  resourceName?: string
  resourceRoute?: string
  dependencyRole?: string
  linkStatus?: string
  requiredForLaunch?: boolean
  metadata?: Record<string, unknown>
}

export interface MarketingCampaignReadinessFinding {
  severity: string
  itemType: string
  itemKey: string
  title: string
  reason: string
  route?: string | null
}

export interface MarketingCampaignReadiness {
  tenantId: number
  campaignId: number
  campaignKey: string
  campaignName: string
  generatedAt: string
  status: string
  productionReady: boolean
  requiredLinkCount: number
  activeRequiredLinkCount: number
  blockerCount: number
  warningCount: number
  blockers: MarketingCampaignReadinessFinding[]
  warnings: MarketingCampaignReadinessFinding[]
  links: MarketingCampaignLink[]
}

export interface MarketingIntegrationContract {
  id: number
  tenantId: number
  contractKey: string
  displayName: string
  providerFamily: string
  sourceCapabilityKey: string
  targetCapabilityKey: string
  assetKey: string
  direction: string
  environment: string
  authMode: string
  credentialDependency?: string | null
  apiRoot: string
  ownerTeam?: string | null
  status: string
  slaTier: string
  timeoutMs: number
  retryPolicy: Record<string, unknown>
  schemaContract: Record<string, unknown>
  metadata: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MarketingIntegrationContractCommand {
  contractKey: string
  displayName: string
  providerFamily: string
  sourceCapabilityKey: string
  targetCapabilityKey: string
  assetKey: string
  direction?: string
  environment?: string
  authMode?: string
  credentialDependency?: string
  apiRoot: string
  ownerTeam?: string
  status?: string
  slaTier?: string
  timeoutMs?: number
  retryPolicy?: Record<string, unknown>
  schemaContract?: Record<string, unknown>
  metadata?: Record<string, unknown>
}

export interface MarketingIntegrationContractQuery {
  status?: string
  providerFamily?: string
  limit?: number
}

export interface MarketingIntegrationContractProbe {
  id: number
  tenantId: number
  contractId: number
  contractKey: string
  providerFamily?: string | null
  probeKey: string
  environment: string
  status: string
  httpStatusCode?: number | null
  latencyMs?: number | null
  errorType?: string | null
  problemTypeUri?: string | null
  problemTitle?: string | null
  problemDetail?: string | null
  errorMessage?: string | null
  summary?: string | null
  observedAt: string
  evidence: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MarketingIntegrationContractProbeCommand {
  probeKey: string
  environment?: string
  status?: string
  httpStatusCode?: number
  latencyMs?: number
  errorType?: string
  problemTypeUri?: string
  problemTitle?: string
  problemDetail?: string
  observedAt?: string
  evidence?: Record<string, unknown>
}

export interface MarketingIntegrationContractProbeQuery {
  status?: string
  providerFamily?: string
  limit?: number
}

export interface MarketingIntegrationContractAuditEvent {
  id: number
  tenantId: number
  contractId: number
  contractKey: string
  revision: number
  eventType: string
  previousStatus?: string | null
  newStatus?: string | null
  snapshot: Record<string, unknown>
  changedFields: Record<string, unknown>
  changedBy?: string | null
  createdAt?: string | null
}

export type MarketingIntegrationContractProbeRun = MarketingIntegrationContractProbe

export interface MarketingIntegrationContractProbeAutomationResult {
  contractId: number
  contractKey: string
  providerFamily: string
  probeKey: string
  status: string
  httpStatusCode?: number | null
  latencyMs?: number | null
  summary?: string | null
  errorMessage?: string | null
  observedAt?: string | null
}

export interface MarketingIntegrationContractProbeAutomationSummary {
  tenantId: number
  candidateCount: number
  probedCount: number
  passedCount: number
  failedCount: number
  skippedCount: number
  evaluatedAt: string
  results: MarketingIntegrationContractProbeAutomationResult[]
}

export interface MarketingIntegrationContractSloWindow {
  ruleKey: string
  windowKey: string
  windowMinutes: number
  totalCount: number
  badCount: number
  badRatio: number
  burnRate: number
  thresholdBurnRate: number
  sufficient: boolean
  breached: boolean
  windowStart?: string | null
  windowEnd?: string | null
}

export interface MarketingIntegrationContractSloEvaluation {
  tenantId: number
  contractId: number
  contractKey: string
  displayName: string
  providerFamily: string
  probeKey: string
  status: string
  severity: string
  triggeredRuleKey?: string | null
  targetPercent: number
  errorBudget: number
  reason: string
  generatedAt: string
  windows: MarketingIntegrationContractSloWindow[]
}

export interface MarketingIntegrationContractProbeRunCommand {
  probeKey: string
  status: string
  httpStatusCode?: number
  latencyMs?: number
  problemTypeUri?: string
  errorMessage?: string
  summary?: string
  evidence?: Record<string, unknown>
}

export type MarketingIntegrationContractProbeRunQuery = MarketingIntegrationContractProbeQuery

export interface MarketingPlatformKpis {
  capabilityCount: number
  liveCapabilities: number
  configurationRequired: number
  actionCount: number
  readinessPercent: number
  readinessGateStatus: string
  blockerCount: number
  warningCount: number
}

export interface ProviderWriteMutationBase {
  id: number
  provider: string
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

export interface SearchMarketingMutation extends ProviderWriteMutationBase {
  sourceId?: number | null
  opportunityId?: number | null
  keywordId?: number | null
  channel?: string | null
}

export interface CreatorProviderMutation extends ProviderWriteMutationBase {
  campaignId?: number | null
  collaborationId?: number | null
  deliverableId?: number | null
  creatorId?: number | null
}

export interface ProgrammaticDspMutation extends ProviderWriteMutationBase {
  seatId?: number | null
  campaignId?: number | null
  lineItemId?: number | null
  supplyPathId?: number | null
}

export interface SearchMarketingMutationQuery {
  sourceId?: number
  status?: string
  approvalStatus?: string
  limit?: number
}

export interface CreatorProviderMutationQuery {
  campaignId?: number
  collaborationId?: number
  status?: string
  approvalStatus?: string
  limit?: number
}

export interface ProgrammaticDspMutationQuery {
  seatId?: number
  campaignId?: number
  lineItemId?: number
  status?: string
  approvalStatus?: string
  limit?: number
}

export interface ProviderMutationApprovalCommand {
  decision: 'APPROVED' | 'REJECTED'
  reason?: string
}

export interface ProviderMutationExecuteCommand {
  dryRun?: boolean
  partialFailure?: boolean
  metadata?: Record<string, unknown>
}

export type ProviderWriteGateway = 'SEARCH_MARKETING' | 'CREATOR' | 'PROGRAMMATIC_DSP'

export interface ProviderWriteQueueItem extends ProviderWriteMutationBase {
  gateway: ProviderWriteGateway
  gatewayLabel: string
  scopeLabel: string
}

export interface ProviderWriteQueueInput {
  search?: SearchMarketingMutation[]
  creator?: CreatorProviderMutation[]
  dsp?: ProgrammaticDspMutation[]
}

export interface ProviderWriteKpis {
  total: number
  pendingApproval: number
  ready: number
  dryRunOk: number
  failed: number
}

export interface ProviderWriteActionState {
  canApprove: boolean
  canDryRun: boolean
  canApply: boolean
}

export function readinessPercent(liveCapabilities: number, capabilityCount: number) {
  if (capabilityCount <= 0 || liveCapabilities <= 0) return 0
  return Math.round((liveCapabilities / capabilityCount) * 100)
}

export function calculateControlPlaneKpis(summary: MarketingPlatformControlPlaneSummary): MarketingPlatformKpis {
  const capabilityCount = summary.capabilityCount ?? summary.capabilities.length
  const liveCapabilities = summary.liveCapabilityCount
    ?? summary.capabilities.filter(capability => capability.status === 'LIVE').length
  return {
    capabilityCount,
    liveCapabilities,
    configurationRequired: summary.capabilities.filter(capability => capability.status !== 'LIVE').length,
    actionCount: summary.actionItemCount ?? summary.actionItems.length,
    readinessPercent: readinessPercent(liveCapabilities, capabilityCount),
    readinessGateStatus: summary.readinessGate?.status ?? summary.overallStatus,
    blockerCount: summary.readinessGate?.blockerCount ?? 0,
    warningCount: summary.readinessGate?.warningCount ?? 0,
  }
}

export function statusText(status: string) {
  switch (status) {
    case 'LIVE':
      return '已上线'
    case 'CONFIGURATION_REQUIRED':
      return '需配置'
    case 'API_ONLY':
      return 'API 就绪'
    case 'READY':
      return '就绪'
    case 'GOVERNED':
      return '已治理'
    case 'BLOCKED':
      return '阻断'
    case 'DEGRADED':
      return '降级可上线'
    default:
      return status || '未知'
  }
}

export function statusColor(status: string) {
  switch (status) {
    case 'LIVE':
    case 'READY':
      return 'green'
    case 'API_ONLY':
      return 'blue'
    case 'CONFIGURATION_REQUIRED':
      return 'gold'
    case 'BLOCKED':
      return 'red'
    case 'DEGRADED':
      return 'gold'
    default:
      return 'default'
  }
}

export function laneStatusColor(status: string) {
  switch (status) {
    case 'GOVERNED':
      return 'green'
    case 'CONFIGURATION_REQUIRED':
      return 'gold'
    case 'API_ONLY':
      return 'blue'
    default:
      return statusColor(status)
  }
}

export function evidenceStatusText(status: string) {
  switch (status) {
    case 'PRESENT':
      return '有证据'
    case 'MISSING':
      return '缺失'
    default:
      return status || '未知'
  }
}

export function evidenceStatusColor(status: string) {
  switch (status) {
    case 'PRESENT':
      return 'green'
    case 'MISSING':
      return 'gold'
    default:
      return 'default'
  }
}

export function priorityColor(priority: string) {
  switch (priority) {
    case 'HIGH':
      return 'red'
    case 'MEDIUM':
      return 'gold'
    case 'LOW':
      return 'blue'
    default:
      return 'default'
  }
}

export function buildProviderWriteQueue(input: ProviderWriteQueueInput): ProviderWriteQueueItem[] {
  return [
    ...(input.search ?? []).map(item => ({
      ...item,
      gateway: 'SEARCH_MARKETING' as const,
      gatewayLabel: 'SEM',
      scopeLabel: compactScope([
        item.sourceId == null ? null : `source#${item.sourceId}`,
        item.keywordId == null ? null : `keyword#${item.keywordId}`,
        item.opportunityId == null ? null : `opportunity#${item.opportunityId}`,
      ]),
    })),
    ...(input.creator ?? []).map(item => ({
      ...item,
      gateway: 'CREATOR' as const,
      gatewayLabel: 'Creator',
      scopeLabel: compactScope([
        item.campaignId == null ? null : `campaign#${item.campaignId}`,
        item.collaborationId == null ? null : `collab#${item.collaborationId}`,
        item.deliverableId == null ? null : `deliverable#${item.deliverableId}`,
      ]),
    })),
    ...(input.dsp ?? []).map(item => ({
      ...item,
      gateway: 'PROGRAMMATIC_DSP' as const,
      gatewayLabel: 'DSP',
      scopeLabel: compactScope([
        item.seatId == null ? null : `seat#${item.seatId}`,
        item.campaignId == null ? null : `campaign#${item.campaignId}`,
        item.lineItemId == null ? null : `line#${item.lineItemId}`,
        item.supplyPathId == null ? null : `supply#${item.supplyPathId}`,
      ]),
    })),
  ].sort((left, right) => timestamp(right.updatedAt) - timestamp(left.updatedAt))
}

export function calculateProviderWriteKpis(queue: ProviderWriteQueueItem[]): ProviderWriteKpis {
  return {
    total: queue.length,
    pendingApproval: queue.filter(item => item.approvalStatus === 'PENDING').length,
    ready: queue.filter(item => item.status === 'READY').length,
    dryRunOk: queue.filter(item => item.status === 'DRY_RUN_OK').length,
    failed: queue.filter(item => item.status === 'FAILED' || item.status === 'DRY_RUN_FAILED').length,
  }
}

export function providerWriteActionState(item: ProviderWriteQueueItem): ProviderWriteActionState {
  const approved = item.approvalStatus === 'APPROVED'
  const terminal = item.status === 'APPLIED' || item.status === 'CANCELLED'
  return {
    canApprove: item.approvalStatus === 'PENDING',
    canDryRun: approved && !terminal,
    canApply: approved && item.status === 'DRY_RUN_OK',
  }
}

export function providerWriteStatusText(status: string) {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'READY':
      return '待 dry-run'
    case 'DRY_RUN_OK':
      return 'Dry-run 通过'
    case 'DRY_RUN_FAILED':
      return 'Dry-run 失败'
    case 'APPLIED':
      return '已执行'
    case 'FAILED':
      return '执行失败'
    case 'CANCELLED':
      return '已取消'
    default:
      return status || '未知'
  }
}

export function providerWriteStatusColor(status: string) {
  switch (status) {
    case 'DRY_RUN_OK':
    case 'APPLIED':
      return 'green'
    case 'READY':
      return 'blue'
    case 'DRAFT':
      return 'gold'
    case 'FAILED':
    case 'DRY_RUN_FAILED':
      return 'red'
    default:
      return 'default'
  }
}

export function providerWriteApprovalText(status: string) {
  switch (status) {
    case 'PENDING':
      return '待审批'
    case 'APPROVED':
      return '已审批'
    case 'REJECTED':
      return '已拒绝'
    default:
      return status || '未知'
  }
}

export function providerWriteApprovalColor(status: string) {
  switch (status) {
    case 'APPROVED':
      return 'green'
    case 'PENDING':
      return 'gold'
    case 'REJECTED':
      return 'red'
    default:
      return 'default'
  }
}

function compactScope(parts: Array<string | null>) {
  const values = parts.filter(Boolean)
  return values.length === 0 ? 'global' : values.join(' · ')
}

function timestamp(value?: string | null) {
  if (!value) return 0
  const parsed = Date.parse(value)
  return Number.isFinite(parsed) ? parsed : 0
}
