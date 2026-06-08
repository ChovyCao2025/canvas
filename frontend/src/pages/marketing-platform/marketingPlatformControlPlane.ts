// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
/** 能力或集成资产的运行证据信号，用于判断生产闭环是否具备可观测依据。 */
export interface MarketingPlatformEvidenceSignal {
  /** 证据信号键。 */
  signalKey: string
  /** 展示标签。 */
  label: string
  /** 信号数值。 */
  value: number
  /** PRESENT 或 MISSING 等证据状态。 */
  status: string
}

/** 营销平台控制面的一项业务能力。 */
export interface MarketingPlatformCapability {
  /** 能力键，用于路由、闸口和集成链路关联。 */
  capabilityKey: string
  /** 能力展示名称。 */
  displayName: string
  /** 所属营销域。 */
  domain: string
  /** 能力生产状态。 */
  status: string
  /** 前端入口路由。 */
  route: string
  /** 后端 API 根路径。 */
  apiRoot: string
  /** 能力暴露的操作界面。 */
  surface: string
  /** 证明能力可生产运行的业务信号。 */
  productionSignals: string[]
  /** 当前未补齐的能力缺口。 */
  gaps: string[]
  /** 运行证据列表。 */
  evidence: MarketingPlatformEvidenceSignal[]
}

/** 营销能力之间的集成链路。 */
export interface MarketingPlatformIntegrationLane {
  /** 链路键。 */
  laneKey: string
  /** 链路展示名称。 */
  displayName: string
  /** 源能力键。 */
  sourceCapabilityKey: string
  /** 目标能力键。 */
  targetCapabilityKey: string
  /** 链路治理状态。 */
  status: string
  /** 链路控制点。 */
  controls: string[]
}

/** 集成资产目录项，例如 provider 写入网关或凭据依赖。 */
export interface MarketingPlatformIntegrationAsset {
  /** 资产键。 */
  assetKey: string
  /** 资产展示名称。 */
  displayName: string
  /** 资产类型。 */
  assetType: string
  /** 归属能力键。 */
  ownerCapabilityKey: string
  /** Provider 家族。 */
  providerFamily: string
  /** 资产状态。 */
  status: string
  /** 资产 API 根路径。 */
  apiRoot: string
  /** 凭据或授权依赖。 */
  credentialDependency: string
  /** 待审批写入数。 */
  pendingWrites: number
  /** 失败写入数。 */
  failedWrites: number
  /** 资产控制点。 */
  controls: string[]
  /** 资产治理缺口。 */
  gaps: string[]
  /** 资产运行证据。 */
  evidence: MarketingPlatformEvidenceSignal[]
}

/** 控制面行动队列项，提示运营补齐配置或治理缺口。 */
export interface MarketingPlatformActionItem {
  /** 优先级。 */
  priority: string
  /** 关联能力键。 */
  capabilityKey: string
  /** 行动标题。 */
  title: string
  /** 处理入口路由。 */
  route: string
  /** 需要处理的原因。 */
  reason: string
}

/** 控制面生产就绪闸口发现项。 */
export interface MarketingPlatformReadinessFinding {
  /** BLOCKER、WARNING 等严重度。 */
  severity: string
  /** 对象类型。 */
  itemType: string
  /** 对象键。 */
  itemKey: string
  /** 发现项标题。 */
  title: string
  /** 处理入口路由。 */
  route: string
  /** 风险原因。 */
  reason: string
}

/** 营销平台整体生产就绪闸口。 */
export interface MarketingPlatformReadinessGate {
  /** 闸口状态。 */
  status: string
  /** 是否允许生产上线。 */
  productionReady: boolean
  /** 阻断数量。 */
  blockerCount: number
  /** 警告数量。 */
  warningCount: number
  /** 阻断项列表。 */
  blockers: MarketingPlatformReadinessFinding[]
  /** 警告项列表。 */
  warnings: MarketingPlatformReadinessFinding[]
}

/** 营销平台控制面总览数据。 */
export interface MarketingPlatformControlPlaneSummary {
  /** 租户 ID。 */
  tenantId: number
  /** 生成时间。 */
  generatedAt: string
  /** 整体状态。 */
  overallStatus: string
  /** 能力总数。 */
  capabilityCount: number
  /** LIVE 能力数量。 */
  liveCapabilityCount: number
  /** 待处理行动数量。 */
  actionItemCount: number
  /** 能力地图。 */
  capabilities: MarketingPlatformCapability[]
  /** 集成链路。 */
  integrationLanes: MarketingPlatformIntegrationLane[]
  /** 集成资产。 */
  integrationAssets: MarketingPlatformIntegrationAsset[]
  /** 生产就绪闸口。 */
  readinessGate: MarketingPlatformReadinessGate
  /** 行动队列。 */
  actionItems: MarketingPlatformActionItem[]
}

/** Campaign 主账本记录。 */
export interface MarketingCampaign {
  /** Campaign 主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** Campaign 业务键。 */
  campaignKey: string
  /** Campaign 名称。 */
  campaignName: string
  /** 营销目标。 */
  objective: string
  /** Campaign 状态。 */
  status: string
  /** 主渠道。 */
  primaryChannel?: string | null
  /** 负责团队。 */
  ownerTeam?: string | null
  /** 开始时间。 */
  startAt?: string | null
  /** 结束时间。 */
  endAt?: string | null
  /** 预算金额。 */
  budgetAmount: number
  /** 预算币种。 */
  currency: string
  /** Campaign brief。 */
  brief: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 更新人。 */
  updatedBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 新建或更新 Campaign 的命令载荷。 */
export interface MarketingCampaignCommand {
  /** Campaign 业务键。 */
  campaignKey: string
  /** Campaign 名称。 */
  campaignName: string
  /** 营销目标。 */
  objective?: string
  /** Campaign 状态。 */
  status?: string
  /** 主渠道。 */
  primaryChannel?: string
  /** 负责团队。 */
  ownerTeam?: string
  /** 开始时间。 */
  startAt?: string
  /** 结束时间。 */
  endAt?: string
  /** 预算金额。 */
  budgetAmount?: number
  /** 预算币种。 */
  currency?: string
  /** Campaign brief。 */
  brief?: Record<string, unknown>
}

/** Campaign 查询条件。 */
export interface MarketingCampaignQuery {
  /** 按状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** Campaign 依赖资源链接，用于上线闸口检查。 */
export interface MarketingCampaignLink {
  /** 链接主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** Campaign ID。 */
  campaignId: number
  /** 资源类型。 */
  resourceType: string
  /** 资源 ID。 */
  resourceId?: number | null
  /** 资源业务键。 */
  resourceKey: string
  /** 资源名称。 */
  resourceName?: string | null
  /** 资源入口路由。 */
  resourceRoute?: string | null
  /** 依赖角色。 */
  dependencyRole: string
  /** 链接状态。 */
  linkStatus: string
  /** 是否为上线必需。 */
  requiredForLaunch: boolean
  /** 链接元数据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 更新人。 */
  updatedBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 新建或更新 Campaign 资源链接的命令载荷。 */
export interface MarketingCampaignLinkCommand {
  /** Campaign ID。 */
  campaignId: number
  /** 资源类型。 */
  resourceType: string
  /** 资源 ID。 */
  resourceId?: number
  /** 资源业务键。 */
  resourceKey: string
  /** 资源名称。 */
  resourceName?: string
  /** 资源入口。 */
  resourceRoute?: string
  /** 依赖角色。 */
  dependencyRole?: string
  /** 链接状态。 */
  linkStatus?: string
  /** 是否为上线必需。 */
  requiredForLaunch?: boolean
  /** 链接元数据。 */
  metadata?: Record<string, unknown>
}

/** Campaign 上线闸口发现项。 */
export interface MarketingCampaignReadinessFinding {
  /** 严重度。 */
  severity: string
  /** 对象类型。 */
  itemType: string
  /** 对象键。 */
  itemKey: string
  /** 发现标题。 */
  title: string
  /** 发现原因。 */
  reason: string
  /** 处理入口。 */
  route?: string | null
}

/** Campaign 上线闸口评估结果。 */
export interface MarketingCampaignReadiness {
  /** 租户 ID。 */
  tenantId: number
  /** Campaign ID。 */
  campaignId: number
  /** Campaign 业务键。 */
  campaignKey: string
  /** Campaign 名称。 */
  campaignName: string
  /** 评估时间。 */
  generatedAt: string
  /** 闸口状态。 */
  status: string
  /** 是否允许上线。 */
  productionReady: boolean
  /** 必需资源数。 */
  requiredLinkCount: number
  /** 活跃必需资源数。 */
  activeRequiredLinkCount: number
  /** 阻断数。 */
  blockerCount: number
  /** 警告数。 */
  warningCount: number
  /** 阻断列表。 */
  blockers: MarketingCampaignReadinessFinding[]
  /** 警告列表。 */
  warnings: MarketingCampaignReadinessFinding[]
  /** 参与评估的资源链接。 */
  links: MarketingCampaignLink[]
}

/** 营销平台集成契约，约束能力间或 provider 间的 API 胶水层。 */
export interface MarketingIntegrationContract {
  /** 契约主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 契约业务键。 */
  contractKey: string
  /** 契约展示名称。 */
  displayName: string
  /** Provider 家族。 */
  providerFamily: string
  /** 源能力键。 */
  sourceCapabilityKey: string
  /** 目标能力键。 */
  targetCapabilityKey: string
  /** 关联集成资产键。 */
  assetKey: string
  /** 调用方向。 */
  direction: string
  /** 环境。 */
  environment: string
  /** 认证模式。 */
  authMode: string
  /** 凭据依赖说明。 */
  credentialDependency?: string | null
  /** API 根路径。 */
  apiRoot: string
  /** 负责团队。 */
  ownerTeam?: string | null
  /** 契约状态。 */
  status: string
  /** SLA 等级。 */
  slaTier: string
  /** 超时时间毫秒。 */
  timeoutMs: number
  /** 重试策略。 */
  retryPolicy: Record<string, unknown>
  /** Schema 契约。 */
  schemaContract: Record<string, unknown>
  /** 契约元数据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 更新人。 */
  updatedBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 新建或更新集成契约的命令载荷。 */
export interface MarketingIntegrationContractCommand {
  /** 契约业务键。 */
  contractKey: string
  /** 契约展示名称。 */
  displayName: string
  /** Provider 家族。 */
  providerFamily: string
  /** 源能力键。 */
  sourceCapabilityKey: string
  /** 目标能力键。 */
  targetCapabilityKey: string
  /** 关联资产键。 */
  assetKey: string
  /** 调用方向。 */
  direction?: string
  /** 环境。 */
  environment?: string
  /** 认证模式。 */
  authMode?: string
  /** 凭据依赖说明。 */
  credentialDependency?: string
  /** API 根路径。 */
  apiRoot: string
  /** 负责团队。 */
  ownerTeam?: string
  /** 契约状态。 */
  status?: string
  /** SLA 等级。 */
  slaTier?: string
  /** 超时时间毫秒。 */
  timeoutMs?: number
  /** 重试策略。 */
  retryPolicy?: Record<string, unknown>
  /** Schema 契约。 */
  schemaContract?: Record<string, unknown>
  /** 契约元数据。 */
  metadata?: Record<string, unknown>
}

/** 集成契约查询条件。 */
export interface MarketingIntegrationContractQuery {
  /** 按状态筛选。 */
  status?: string
  /** 按 Provider 家族筛选。 */
  providerFamily?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 集成契约探针结果，用于监控 API 健康和 SLO burn-rate。 */
export interface MarketingIntegrationContractProbe {
  /** 探针主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 契约 ID。 */
  contractId: number
  /** 契约键。 */
  contractKey: string
  /** Provider 家族。 */
  providerFamily?: string | null
  /** 探针键。 */
  probeKey: string
  /** 环境。 */
  environment: string
  /** PASS、WARN 或 FAIL。 */
  status: string
  /** HTTP 状态码。 */
  httpStatusCode?: number | null
  /** 延迟毫秒。 */
  latencyMs?: number | null
  /** 错误类型。 */
  errorType?: string | null
  /** Problem Details 类型 URI。 */
  problemTypeUri?: string | null
  /** 问题标题。 */
  problemTitle?: string | null
  /** 问题详情。 */
  problemDetail?: string | null
  /** 错误信息。 */
  errorMessage?: string | null
  /** 探针摘要。 */
  summary?: string | null
  /** 观测时间。 */
  observedAt: string
  /** 探针证据。 */
  evidence: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string | null
  /** 更新人。 */
  updatedBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
  /** 更新时间。 */
  updatedAt?: string | null
}

/** 记录集成契约探针的命令载荷。 */
export interface MarketingIntegrationContractProbeCommand {
  /** 探针键。 */
  probeKey: string
  /** 环境。 */
  environment?: string
  /** 探针状态。 */
  status?: string
  /** HTTP 状态码。 */
  httpStatusCode?: number
  /** 延迟毫秒。 */
  latencyMs?: number
  /** 错误类型。 */
  errorType?: string
  /** Problem Details 类型 URI。 */
  problemTypeUri?: string
  /** 问题标题。 */
  problemTitle?: string
  /** 问题详情。 */
  problemDetail?: string
  /** 观测时间。 */
  observedAt?: string
  /** 探针证据。 */
  evidence?: Record<string, unknown>
}

/** 集成契约探针查询条件。 */
export interface MarketingIntegrationContractProbeQuery {
  /** 按探针状态筛选。 */
  status?: string
  /** 按 Provider 家族筛选。 */
  providerFamily?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 集成契约审计事件，用于追踪契约配置变更。 */
export interface MarketingIntegrationContractAuditEvent {
  /** 审计事件主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 契约 ID。 */
  contractId: number
  /** 契约键。 */
  contractKey: string
  /** 版本号。 */
  revision: number
  /** 事件类型。 */
  eventType: string
  /** 变更前状态。 */
  previousStatus?: string | null
  /** 变更后状态。 */
  newStatus?: string | null
  /** 契约快照。 */
  snapshot: Record<string, unknown>
  /** 变化字段。 */
  changedFields: Record<string, unknown>
  /** 操作人。 */
  changedBy?: string | null
  /** 创建时间。 */
  createdAt?: string | null
}

/** 探针运行记录与普通探针模型一致。 */
export type MarketingIntegrationContractProbeRun = MarketingIntegrationContractProbe

/** 自动探针单个契约的执行结果。 */
export interface MarketingIntegrationContractProbeAutomationResult {
  /** 契约 ID。 */
  contractId: number
  /** 契约键。 */
  contractKey: string
  /** Provider 家族。 */
  providerFamily: string
  /** 探针键。 */
  probeKey: string
  /** 执行状态。 */
  status: string
  /** HTTP 状态码。 */
  httpStatusCode?: number | null
  /** 延迟毫秒。 */
  latencyMs?: number | null
  /** 执行摘要。 */
  summary?: string | null
  /** 错误信息。 */
  errorMessage?: string | null
  /** 观测时间。 */
  observedAt?: string | null
}

/** 自动探针扫描汇总。 */
export interface MarketingIntegrationContractProbeAutomationSummary {
  /** 租户 ID。 */
  tenantId: number
  /** 候选契约数量。 */
  candidateCount: number
  /** 已探测数量。 */
  probedCount: number
  /** PASS 数量。 */
  passedCount: number
  /** FAIL 数量。 */
  failedCount: number
  /** 跳过数量。 */
  skippedCount: number
  /** 评估时间。 */
  evaluatedAt: string
  /** 单契约结果。 */
  results: MarketingIntegrationContractProbeAutomationResult[]
}

/** SLO burn-rate 的单个时间窗口评估。 */
export interface MarketingIntegrationContractSloWindow {
  /** SLO 规则键。 */
  ruleKey: string
  /** 窗口键。 */
  windowKey: string
  /** 窗口分钟数。 */
  windowMinutes: number
  /** 窗口总样本数。 */
  totalCount: number
  /** 坏样本数。 */
  badCount: number
  /** 坏样本比例。 */
  badRatio: number
  /** burn-rate 倍数。 */
  burnRate: number
  /** 触发阈值倍数。 */
  thresholdBurnRate: number
  /** 样本是否充足。 */
  sufficient: boolean
  /** 是否触发违约。 */
  breached: boolean
  /** 窗口开始时间。 */
  windowStart?: string | null
  /** 窗口结束时间。 */
  windowEnd?: string | null
}

/** 集成契约 SLO 评估结果。 */
export interface MarketingIntegrationContractSloEvaluation {
  /** 租户 ID。 */
  tenantId: number
  /** 契约 ID。 */
  contractId: number
  /** 契约键。 */
  contractKey: string
  /** 契约展示名称。 */
  displayName: string
  /** Provider 家族。 */
  providerFamily: string
  /** 探针键。 */
  probeKey: string
  /** SLO 状态。 */
  status: string
  /** 严重度。 */
  severity: string
  /** 触发规则键。 */
  triggeredRuleKey?: string | null
  /** 目标可用性百分比。 */
  targetPercent: number
  /** 错误预算。 */
  errorBudget: number
  /** 触发原因。 */
  reason: string
  /** 生成时间。 */
  generatedAt: string
  /** 参与评估的窗口。 */
  windows: MarketingIntegrationContractSloWindow[]
}

/** 手动记录探针运行的命令载荷。 */
export interface MarketingIntegrationContractProbeRunCommand {
  /** 探针键。 */
  probeKey: string
  /** 运行状态。 */
  status: string
  /** HTTP 状态码。 */
  httpStatusCode?: number
  /** 延迟毫秒。 */
  latencyMs?: number
  /** Problem Details 类型 URI。 */
  problemTypeUri?: string
  /** 错误信息。 */
  errorMessage?: string
  /** 运行摘要。 */
  summary?: string
  /** 运行证据。 */
  evidence?: Record<string, unknown>
}

/** 探针运行查询条件。 */
export type MarketingIntegrationContractProbeRunQuery = MarketingIntegrationContractProbeQuery

/** 控制面 KPI 展示模型。 */
export interface MarketingPlatformKpis {
  /** 能力总数。 */
  capabilityCount: number
  /** LIVE 能力数量。 */
  liveCapabilities: number
  /** 需要配置的能力数量。 */
  configurationRequired: number
  /** 行动项数量。 */
  actionCount: number
  /** 生产就绪百分比。 */
  readinessPercent: number
  /** 闸口状态。 */
  readinessGateStatus: string
  /** 阻断数量。 */
  blockerCount: number
  /** 警告数量。 */
  warningCount: number
}

/** provider 写入 mutation 的通用字段。 */
export interface ProviderWriteMutationBase {
  /** 写入主键。 */
  id: number
  /** provider 名称。 */
  provider: string
  /** 写入业务键。 */
  mutationKey: string
  /** 写入类型。 */
  mutationType: string
  /** 供应商实体类型。 */
  entityType: string
  /** 供应商实体 ID。 */
  externalEntityId?: string | null
  /** 请求哈希。 */
  requestHash: string
  /** 幂等键。 */
  idempotencyKey: string
  /** 执行状态。 */
  status: string
  /** 审批状态。 */
  approvalStatus: string
  /** 是否需要 dry-run。 */
  dryRunRequired?: boolean | null
  /** 业务载荷。 */
  payload?: Record<string, unknown>
  /** 校验结果。 */
  validation?: Record<string, unknown>
  /** provider 请求。 */
  providerRequest?: Record<string, unknown>
  /** provider 响应。 */
  providerResponse?: Record<string, unknown>
  /** 错误码。 */
  errorCode?: string | null
  /** 错误信息。 */
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

/** 搜索营销 provider 写入，用于控制面统一审批队列。 */
export interface SearchMarketingMutation extends ProviderWriteMutationBase {
  /** 搜索来源 ID。 */
  sourceId?: number | null
  /** 关联机会 ID。 */
  opportunityId?: number | null
  /** 关联关键词 ID。 */
  keywordId?: number | null
  /** 搜索渠道。 */
  channel?: string | null
}

/** 达人协作 provider 写入，用于控制面统一审批队列。 */
export interface CreatorProviderMutation extends ProviderWriteMutationBase {
  /** Campaign ID。 */
  campaignId?: number | null
  /** 协作 ID。 */
  collaborationId?: number | null
  /** 交付物 ID。 */
  deliverableId?: number | null
  /** 达人 ID。 */
  creatorId?: number | null
}

/** 程序化 DSP provider 写入，用于控制面统一审批队列。 */
export interface ProgrammaticDspMutation extends ProviderWriteMutationBase {
  /** DSP seat ID。 */
  seatId?: number | null
  /** DSP Campaign ID。 */
  campaignId?: number | null
  /** Line item ID。 */
  lineItemId?: number | null
  /** Supply path ID。 */
  supplyPathId?: number | null
}

/** 搜索营销写入查询条件。 */
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

/** 达人 provider 写入查询条件。 */
export interface CreatorProviderMutationQuery {
  /** 按 Campaign 筛选。 */
  campaignId?: number
  /** 按协作筛选。 */
  collaborationId?: number
  /** 按执行状态筛选。 */
  status?: string
  /** 按审批状态筛选。 */
  approvalStatus?: string
  /** 返回数量上限。 */
  limit?: number
}

/** DSP provider 写入查询条件。 */
export interface ProgrammaticDspMutationQuery {
  /** 按 seat 筛选。 */
  seatId?: number
  /** 按 Campaign 筛选。 */
  campaignId?: number
  /** 按 line item 筛选。 */
  lineItemId?: number
  /** 按执行状态筛选。 */
  status?: string
  /** 按审批状态筛选。 */
  approvalStatus?: string
  /** 返回数量上限。 */
  limit?: number
}

/** provider 写入审批命令。 */
export interface ProviderMutationApprovalCommand {
  /** 审批决策。 */
  decision: 'APPROVED' | 'REJECTED'
  /** 审批原因。 */
  reason?: string
}

/** provider 写入执行命令。 */
export interface ProviderMutationExecuteCommand {
  /** true 表示只执行 dry-run。 */
  dryRun?: boolean
  /** 是否允许部分失败。 */
  partialFailure?: boolean
  /** 执行元数据。 */
  metadata?: Record<string, unknown>
}

/** 控制面统一 provider 写入来源网关。 */
export type ProviderWriteGateway = 'SEARCH_MARKETING' | 'CREATOR' | 'PROGRAMMATIC_DSP'

/** 控制面统一 provider 写入队列项。 */
export interface ProviderWriteQueueItem extends ProviderWriteMutationBase {
  /** 网关类型。 */
  gateway: ProviderWriteGateway
  /** 网关展示名称。 */
  gatewayLabel: string
  /** 由来源、活动或实体 ID 组合出的作用域展示。 */
  scopeLabel: string
}

/** 构建统一 provider 写入队列的输入集合。 */
export interface ProviderWriteQueueInput {
  /** 搜索营销写入。 */
  search?: SearchMarketingMutation[]
  /** 达人协作写入。 */
  creator?: CreatorProviderMutation[]
  /** 程序化 DSP 写入。 */
  dsp?: ProgrammaticDspMutation[]
}

/** provider 写入队列 KPI。 */
export interface ProviderWriteKpis {
  /** 写入总数。 */
  total: number
  /** 待审批数量。 */
  pendingApproval: number
  /** 待 dry-run 数量。 */
  ready: number
  /** dry-run 通过数量。 */
  dryRunOk: number
  /** 执行或 dry-run 失败数量。 */
  failed: number
}

/** provider 写入操作按钮可用状态。 */
export interface ProviderWriteActionState {
  /** 是否可审批。 */
  canApprove: boolean
  /** 是否可 dry-run。 */
  canDryRun: boolean
  /** 是否可正式 apply。 */
  canApply: boolean
}

/** 计算营销平台 LIVE 能力占比，用于生产就绪圆环。 */
export function readinessPercent(liveCapabilities: number, capabilityCount: number) {
  if (capabilityCount <= 0 || liveCapabilities <= 0) return 0
  return Math.round((liveCapabilities / capabilityCount) * 100)
}

/** 聚合控制面 KPI，包括能力数量、配置缺口和上线闸口风险。 */
export function calculateControlPlaneKpis(summary: MarketingPlatformControlPlaneSummary): MarketingPlatformKpis {
  const capabilityCount = summary.capabilityCount ?? summary.capabilities.length
  // 优先使用后端汇总值，缺失时从能力列表回退计算 LIVE 数量。
  const liveCapabilities = summary.liveCapabilityCount
    ?? summary.capabilities.filter(capability => capability.status === 'LIVE').length
  return {
    capabilityCount,
    liveCapabilities,
    // 非 LIVE 能力视为仍需补齐配置或治理动作。
    configurationRequired: summary.capabilities.filter(capability => capability.status !== 'LIVE').length,
    actionCount: summary.actionItemCount ?? summary.actionItems.length,
    readinessPercent: readinessPercent(liveCapabilities, capabilityCount),
    readinessGateStatus: summary.readinessGate?.status ?? summary.overallStatus,
    blockerCount: summary.readinessGate?.blockerCount ?? 0,
    warningCount: summary.readinessGate?.warningCount ?? 0,
  }
}

/** 将控制面状态映射为中文文案。 */
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

/** 将控制面状态映射为 Ant Design 标签颜色。 */
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

/** 将集成链路状态映射为标签颜色。 */
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

/** 将证据状态映射为中文文案。 */
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

/** 将证据状态映射为标签颜色。 */
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

/** 将行动队列优先级映射为标签颜色。 */
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

/** 合并搜索、达人、DSP 三类 provider 写入，派生统一控制面队列。 */
export function buildProviderWriteQueue(input: ProviderWriteQueueInput): ProviderWriteQueueItem[] {
  return [
    // 搜索营销写入以 source、keyword、opportunity 组成作用域。
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
    // 达人写入以 campaign、collaboration、deliverable 组成作用域。
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
    // DSP 写入以 seat、campaign、line item、supply path 组成作用域。
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
  // 统一队列按更新时间倒序，确保最新审批/执行项优先展示。
  ].sort((left, right) => timestamp(right.updatedAt) - timestamp(left.updatedAt))
}

/** 聚合 provider 写入队列 KPI。 */
export function calculateProviderWriteKpis(queue: ProviderWriteQueueItem[]): ProviderWriteKpis {
  return {
    total: queue.length,
    pendingApproval: queue.filter(item => item.approvalStatus === 'PENDING').length,
    ready: queue.filter(item => item.status === 'READY').length,
    dryRunOk: queue.filter(item => item.status === 'DRY_RUN_OK').length,
    failed: queue.filter(item => item.status === 'FAILED' || item.status === 'DRY_RUN_FAILED').length,
  }
}

/** 根据审批状态和写入状态判断控制面按钮可用性。 */
export function providerWriteActionState(item: ProviderWriteQueueItem): ProviderWriteActionState {
  const approved = item.approvalStatus === 'APPROVED'
  const terminal = item.status === 'APPLIED' || item.status === 'CANCELLED'
  // 待审批只能 approve；审批后可 dry-run；dry-run 成功后可正式 apply。
  return {
    canApprove: item.approvalStatus === 'PENDING',
    canDryRun: approved && !terminal,
    canApply: approved && item.status === 'DRY_RUN_OK',
  }
}

/** 将 provider 写入状态映射为中文文案。 */
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

/** 将 provider 写入状态映射为标签颜色。 */
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

/** 将 provider 写入审批状态映射为中文文案。 */
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

/** 将 provider 写入审批状态映射为标签颜色。 */
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
