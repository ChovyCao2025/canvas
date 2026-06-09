/** 搜索营销接入的媒体或站点来源，用于 SEO/SEM 数据同步和写入归属。 */
export interface SearchMarketingSource {
  /** 后端来源主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 搜索供应商，例如 GOOGLE_ADS、SEARCH_CONSOLE。 */
  provider: string
  /** 来源业务键。 */
  sourceKey: string
  /** 来源展示名称。 */
  displayName: string
  /** SEO、SEM 等搜索营销渠道。 */
  channel: string
  /** 供应商侧账号 ID。 */
  externalAccountId?: string | null
  /** 站点 URL，通常用于 SEO 抓取和 URL inspection。 */
  siteUrl?: string | null
  /** SEM 花费币种。 */
  currency?: string | null
  /** 来源账号时区。 */
  timezone?: string | null
  /** 是否参与定时同步和工作台展示。 */
  enabled: boolean
  /** 供应商扩展配置。 */
  metadata?: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 搜索关键词主数据，用于快照聚合、机会识别和 provider 写入。 */
export interface SearchMarketingKeyword {
  /** 关键词主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 关键词所属渠道。 */
  channel: string
  /** 关键词原文。 */
  keywordText: string
  /** 关键词业务键。 */
  keywordKey: string
  /** SEM 匹配类型。 */
  matchType?: string | null
  /** 目标落地页。 */
  landingPageUrl?: string | null
  /** 落地页哈希，用于 URL inspection 和影响窗口归因。 */
  landingPageUrlHash?: string | null
  /** 搜索意图分类。 */
  searchIntent?: string | null
  /** 运营标签。 */
  labels: string[]
  /** 关键词投放或治理状态。 */
  status: string
  /** 关键词扩展元数据。 */
  metadata?: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 搜索营销每日/分设备指标快照，用于 KPI 聚合和机会检测。 */
export interface SearchMarketingSnapshot {
  /** 快照主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 数据来源 ID。 */
  sourceId: number
  /** 关键词 ID；为空时表示来源级聚合。 */
  keywordId?: number | null
  /** SEO 或 SEM 渠道。 */
  channel: string
  /** 快照日期。 */
  snapshotDate: string
  /** 设备维度。 */
  device?: string | null
  /** 国家/地区维度。 */
  country?: string | null
  /** 搜索词分组键。 */
  queryGroupKey?: string | null
  /** 展示次数。 */
  impressionCount: number
  /** 点击次数。 */
  clickCount: number
  /** SEM 花费。 */
  costAmount: number
  /** 转化次数。 */
  conversionCount: number
  /** 收入金额。 */
  revenueAmount: number
  /** 平均排名或广告位置。 */
  averagePosition?: number | null
  /** 快照扩展元数据。 */
  metadata?: Record<string, unknown>
  /** 供应商拉取证据。 */
  evidence?: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 搜索营销优化机会，由快照、URL inspection 或供应商差异识别产生。 */
export interface SearchMarketingOpportunity {
  /** 机会主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 来源 ID。 */
  sourceId: number
  /** 关联关键词 ID。 */
  keywordId?: number | null
  /** 渠道。 */
  channel: string
  /** 机会类型，例如 BID、KEYWORD、SEO_INDEXING。 */
  opportunityType: string
  /** 机会对应的指标日期。 */
  snapshotDate: string
  /** 机会严重度。 */
  severity: string
  /** 机会处理状态。 */
  status: string
  /** 推荐动作描述。 */
  recommendation: string
  /** 预估影响分。 */
  impactScore?: number | null
  /** 综合评分。 */
  score?: number | null
  /** 机会判定证据。 */
  evidence: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 对搜索供应商的待审批/待执行写入操作。 */
export interface SearchMarketingMutation {
  /** 写入主键。 */
  id: number
  /** 租户 ID。 */
  tenantId?: number
  /** 来源 ID。 */
  sourceId?: number | null
  /** 触发该写入的机会 ID。 */
  opportunityId?: number | null
  /** 关联关键词 ID。 */
  keywordId?: number | null
  /** 搜索供应商。 */
  provider: string
  /** 渠道。 */
  channel?: string | null
  /** 写入业务键，用于幂等和审计。 */
  mutationKey: string
  /** 写入动作类型。 */
  mutationType: string
  /** 被写入的供应商实体类型。 */
  entityType: string
  /** 供应商侧实体 ID。 */
  externalEntityId?: string | null
  /** 请求体哈希，用于重复写入检测。 */
  requestHash: string
  /** 幂等键。 */
  idempotencyKey: string
  /** 写入执行状态。 */
  status: string
  /** 审批状态。 */
  approvalStatus: string
  /** 是否必须先 dry-run。 */
  dryRunRequired?: boolean | null
  /** 业务写入载荷。 */
  payload?: Record<string, unknown>
  /** 写入前校验结果。 */
  validation?: Record<string, unknown>
  /** dry-run 结果。 */
  dryRunResult?: Record<string, unknown>
  /** provider apply 结果。 */
  providerResult?: Record<string, unknown>
  /** 实际供应商请求。 */
  providerRequest?: Record<string, unknown>
  /** 实际供应商响应。 */
  providerResponse?: Record<string, unknown>
  /** 错误码。 */
  errorCode?: string | null
  /** 错误说明。 */
  errorMessage?: string | null
  /** 创建人。 */
  createdBy?: string | null
  /** 审批人。 */
  approvedBy?: string | null
  /** 审批时间。 */
  approvedAt?: string | null
  /** 执行人。 */
  executedBy?: string | null
  /** 执行时间。 */
  executedAt?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
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

/** 搜索供应商侧检测到的变更，用于和本地 mutation 做 reconciliation。 */
export interface SearchMarketingProviderChange {
  /** 供应商变更主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 关联来源 ID。 */
  sourceId?: number | null
  /** 关联本地写入 ID。 */
  mutationId?: number | null
  /** 搜索供应商。 */
  provider: string
  /** 供应商侧资源 ID。 */
  externalResourceId?: string | null
  /** 变更类型。 */
  changeType: string
  /** 供应商侧变化字段。 */
  changedFields: Record<string, unknown>
  /** 供应商侧操作人。 */
  providerActor?: string | null
  /** 供应商侧变更时间。 */
  providerChangedAt?: string | null
  /** reconciliation 状态。 */
  reconciliationStatus: string
  /** 对账证据。 */
  evidence: Record<string, unknown>
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 本地写入与供应商变更完成对账后的结果。 */
export interface SearchMarketingReconciliation {
  /** 租户 ID。 */
  tenantId: number
  /** 被对账的写入 ID。 */
  mutationId: number
  /** 匹配到的供应商变更 ID。 */
  providerChangeId?: number | null
  /** 对账状态。 */
  status: string
  /** 供应商操作 ID。 */
  providerOperationId?: string | null
  /** 对账证据。 */
  evidence: Record<string, unknown>
  /** 对账完成时间。 */
  reconciledAt: string
}

/** 搜索营销写入后的影响评估窗口，用于判断优化是否正向。 */
export interface SearchMarketingImpactWindow {
  /** 影响窗口主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 关联机会 ID。 */
  opportunityId: number
  /** 关联写入 ID。 */
  mutationId: number
  /** 来源 ID。 */
  sourceId?: number | null
  /** 关键词 ID。 */
  keywordId?: number | null
  /** 页面 URL 哈希。 */
  pageUrlHash?: string | null
  /** 基线窗口开始日期。 */
  baselineStartDate: string
  /** 基线窗口结束日期。 */
  baselineEndDate: string
  /** 写入后观察窗口开始日期。 */
  postStartDate: string
  /** 写入后观察窗口结束日期。 */
  postEndDate: string
  /** 评估状态。 */
  status: string
  /** 影响决策，例如 POSITIVE、NEGATIVE。 */
  decision?: string | null
  /** 评估置信度。 */
  confidence?: number | null
  /** 指标差异集合。 */
  metricDeltas: Record<string, unknown>
  /** 评估证据。 */
  evidence: Record<string, unknown>
  /** 应评估时间。 */
  dueAt?: string | null
  /** 实际评估时间。 */
  evaluatedAt?: string | null
  /** 创建人。 */
  createdBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 搜索营销生产就绪闸口结果。 */
export interface SearchMarketingReadiness {
  /** 租户 ID。 */
  tenantId: number
  /** LIVE、DEGRADED 或 BLOCKED 等就绪状态。 */
  status: string
  /** 阻断真实 provider 写入的原因。 */
  blockers: string[]
  /** 就绪判断证据。 */
  evidence: Record<string, unknown>
  /** 评估时间。 */
  evaluatedAt: string
}

/** 来源列表查询条件。 */
export interface SearchMarketingSourceQuery {
  /** 按供应商筛选。 */
  provider?: string
  /** 按渠道筛选。 */
  channel?: string
  /** 按启用状态筛选。 */
  enabled?: boolean
  /** 返回数量上限。 */
  limit?: number
}

/** 关键词列表查询条件。 */
export interface SearchMarketingKeywordQuery {
  /** 按渠道筛选。 */
  channel?: string
  /** 按关键词状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 搜索指标快照查询条件。 */
export interface SearchMarketingSnapshotQuery {
  /** 按渠道筛选。 */
  channel?: string
  /** 按来源筛选。 */
  sourceId?: number
  /** 按关键词筛选。 */
  keywordId?: number
  /** 快照开始日期。 */
  startDate?: string
  /** 快照结束日期。 */
  endDate?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 优化机会查询条件。 */
export interface SearchMarketingOpportunityQuery {
  /** 按渠道筛选。 */
  channel?: string
  /** 按来源筛选。 */
  sourceId?: number
  /** 按机会状态筛选。 */
  status?: string
  /** 按严重度筛选。 */
  severity?: string
  /** 返回数量上限。 */
  limit?: number
}

/** provider 写入查询条件。 */
export interface SearchMarketingMutationQuery {
  /** 按来源筛选。 */
  sourceId?: number
  /** 按执行状态筛选。 */
  status?: string
  /** 按审批状态筛选。 */
  approvalStatus?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 同步任务查询条件。 */
export interface SearchMarketingSyncRunQuery {
  /** 按来源筛选。 */
  sourceId?: number
  /** 按同步类型筛选。 */
  runType?: string
  /** 按运行状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** URL inspection 查询条件。 */
export interface SearchMarketingUrlInspectionQuery {
  /** 按来源筛选。 */
  sourceId?: number
  /** 按索引状态筛选。 */
  indexedState?: string
  /** 检测开始日期。 */
  startDate?: string
  /** 检测结束日期。 */
  endDate?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 供应商变更查询条件。 */
export interface SearchMarketingProviderChangeQuery {
  /** 按来源筛选。 */
  sourceId?: number
  /** 按本地写入筛选。 */
  mutationId?: number
  /** 按供应商筛选。 */
  provider?: string
  /** 按对账状态筛选。 */
  reconciliationStatus?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 影响窗口查询条件。 */
export interface SearchMarketingImpactWindowQuery {
  /** 按机会筛选。 */
  opportunityId?: number
  /** 按写入筛选。 */
  mutationId?: number
  /** 按来源筛选。 */
  sourceId?: number
  /** 按评估状态筛选。 */
  status?: string
  /** 按影响决策筛选。 */
  decision?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 手动触发来源同步的请求参数。 */
export interface SearchMarketingSyncRequest {
  /** 同步类型。 */
  runType?: string
  /** 同步窗口开始时间。 */
  windowStart?: string
  /** 同步窗口结束时间。 */
  windowEnd?: string
  /** 供应商游标。 */
  cursorValue?: string
}

/** 批量同步或批量评估到期任务的请求参数。 */
export interface SearchMarketingSyncDueRequest {
  /** 本次处理上限。 */
  limit?: number
}

/** 基于优化机会创建 provider 写入的请求。 */
export interface SearchMarketingOpportunityMutationCommand {
  /** 写入业务键。 */
  mutationKey: string
  /** 写入类型。 */
  mutationType: string
  /** 供应商实体类型。 */
  entityType: string
  /** 供应商实体 ID。 */
  externalEntityId?: string
  /** 是否需要 dry-run。 */
  dryRunRequired?: boolean
  /** 幂等键。 */
  idempotencyKey?: string
  /** 业务写入载荷。 */
  payload?: Record<string, unknown>
}

/** 直接创建搜索营销 provider 写入的请求。 */
export interface SearchMarketingMutationCommand extends SearchMarketingOpportunityMutationCommand {
  /** 来源 ID。 */
  sourceId: number
  /** 关联机会 ID。 */
  opportunityId?: number
  /** 关联关键词 ID。 */
  keywordId?: number
}

/** 更新优化机会状态的请求。 */
export interface SearchMarketingOpportunityStatusCommand {
  /** 目标机会状态。 */
  status: string
  /** 状态变更原因。 */
  reason?: string
}

/** 搜索营销 provider 写入审批请求。 */
export interface SearchMarketingMutationApprovalCommand {
  /** 审批决策。 */
  decision: 'APPROVED' | 'REJECTED'
  /** 审批原因。 */
  reason?: string
}

/** 搜索营销 provider 写入执行请求。 */
export interface SearchMarketingMutationExecuteCommand {
  /** true 表示只执行 dry-run。 */
  dryRun?: boolean
  /** 是否允许供应商部分失败。 */
  partialFailure?: boolean
  /** 执行元数据。 */
  metadata?: Record<string, unknown>
}

/** 搜索营销工作台顶部 KPI 的输入集合。 */
export interface SearchMarketingKpiInput {
  /** 用于聚合点击、花费、转化和收入的指标快照。 */
  snapshots?: SearchMarketingSnapshot[]
  /** 用于统计待处理机会的机会列表。 */
  opportunities?: SearchMarketingOpportunity[]
  /** 用于统计写入失败和待 reconciliation 的操作列表。 */
  mutations?: SearchMarketingMutation[]
}

/** 搜索营销工作台 KPI 展示模型。 */
export interface SearchMarketingKpis {
  /** SEO 点击总量。 */
  seoClicks: number
  /** SEM 花费总额。 */
  semSpend: number
  /** 转化总量。 */
  conversions: number
  /** 收入 / SEM 花费。 */
  roas: number
  /** OPEN 或 ACCEPTED 的待处理机会数量。 */
  openOpportunities: number
  /** provider 写入失败数量。 */
  failedWrites: number
  /** 已 apply 但尚未 reconciliation 的写入数量。 */
  unreconciledWrites: number
}

/** 搜索营销写入操作按钮的可用状态。 */
export interface SearchMarketingActionState {
  /** 是否可以审批。 */
  canApprove: boolean
  /** 是否可以发起 dry-run。 */
  canDryRun: boolean
  /** 是否可以正式 apply。 */
  canApply: boolean
}

/** 将搜索营销生产就绪状态映射为展示标签。 */
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

/** 将同步任务状态映射为展示标签。 */
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

/** 将机会严重度映射为中文展示和告警颜色。 */
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

/** 将机会处理状态映射为中文展示和标签颜色。 */
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

/** 基于审批状态和终态判断搜索供应商写入允许哪些操作。 */
export function mutationActionState(item: SearchMarketingMutation): SearchMarketingActionState {
  const approved = item.approvalStatus === 'APPROVED'
  const terminal = ['APPLIED', 'CANCELLED', 'RECONCILED'].includes(item.status)
  // PENDING 只能审批；通过审批且未终态才能 dry-run；dry-run 成功后才能 apply。
  return {
    canApprove: item.approvalStatus === 'PENDING',
    canDryRun: approved && !terminal,
    canApply: approved && item.status === 'DRY_RUN_OK',
  }
}

/** 判断写入是否允许执行 dry-run。 */
export function canDryRunMutation(item: SearchMarketingMutation) {
  return mutationActionState(item).canDryRun
}

/** 判断写入是否允许正式 apply，同时受搜索营销整体就绪状态约束。 */
export function canApplyMutation(item: SearchMarketingMutation, readinessStatus: string) {
  // 生产闸口未 LIVE 或审批未通过时禁止真实写入供应商。
  if (normalize(readinessStatus) !== 'LIVE' || item.approvalStatus !== 'APPROVED') {
    return false
  }
  if (item.dryRunRequired === false && item.status === 'READY') {
    return true
  }
  return item.status === 'DRY_RUN_OK'
}

/** 递归脱敏搜索营销证据，避免 token、密钥等字段进入页面展示。 */
export function sanitizeSearchMarketingEvidence<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map(item => sanitizeSearchMarketingEvidence(item)) as T
  }
  if (!isRecord(value)) {
    return value
  }
  return Object.fromEntries(
    // 保留证据结构，只替换敏感字段值。
    Object.entries(value).map(([key, entry]) => [
      key,
      isSecretKey(key) ? '[REDACTED]' : sanitizeSearchMarketingEvidence(entry),
    ]),
  ) as T
}

export const redactSearchMarketingSecrets = sanitizeSearchMarketingEvidence

/** 聚合搜索营销工作台 KPI，包括 SEO 点击、SEM 花费、ROAS 和写入状态。 */
export function calculateSearchMarketingKpis(input: SearchMarketingKpiInput): SearchMarketingKpis {
  const snapshots = input.snapshots ?? []
  const opportunities = input.opportunities ?? []
  const mutations = input.mutations ?? []
  // 只把 SEM 渠道成本纳入投放花费。
  const semSpend = snapshots
    .filter(snapshot => normalize(snapshot.channel) === 'SEM')
    .reduce((total, snapshot) => total + numberValue(snapshot.costAmount), 0)
  // 收入跨 SEO/SEM 统计，用于和 SEM 花费计算 ROAS。
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
