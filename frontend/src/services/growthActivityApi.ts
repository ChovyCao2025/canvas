import http from './api'
import type { R } from '../types'

/** 增长活动列表查询条件。 */
export interface GrowthActivityQuery {
  /** 活动类型筛选，例如 REFERRAL_INVITE。 */
  activityType?: string

  /** 活动状态筛选。 */
  status?: string

  /** 关联 Campaign ID。 */
  campaignId?: number

  /** 负责人团队。 */
  ownerTeam?: string

  /** 上线闸口状态。 */
  readinessStatus?: string

  /** 排期状态。 */
  scheduleStatus?: string

  /** 奖励发放健康状态。 */
  grantHealth?: string

  /** 返回条数上限。 */
  limit?: number
}

/** 创建或更新增长活动的请求体。 */
export interface GrowthActivityCommand {
  /** 活动业务 key，用于运营识别和幂等 upsert。 */
  activityKey: string

  /** 活动名称。 */
  activityName: string

  /** 活动类型。 */
  activityType: string

  /** 活动状态。 */
  status?: string

  /** 关联 Campaign ID。 */
  campaignId?: number | null

  /** 活动目标，例如 ACQUISITION / ENGAGEMENT。 */
  objective?: string | null

  /** 负责人团队。 */
  ownerTeam?: string | null

  /** 活动开始时间。 */
  startAt?: string | null

  /** 活动结束时间。 */
  endAt?: string | null

  /** 渠道范围。 */
  channelScope?: string | null

  /** 关联人群引用。 */
  audienceRefs?: Record<string, unknown>

  /** 风控策略引用。 */
  riskPolicyRef?: string | null

  /** 实验引用。 */
  experimentRef?: string | null

  /** 报表看板引用。 */
  dashboardRef?: string | null

  /** 扩展元数据。 */
  metadata?: Record<string, unknown>
}

/** 增长活动主记录。 */
export interface GrowthActivity {
  /** 活动 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动业务 key。 */
  activityKey: string

  /** 活动名称。 */
  activityName: string

  /** 活动类型。 */
  activityType: string

  /** 活动状态，例如 DRAFT / ACTIVE / PAUSED / CLOSED。 */
  status: string

  /** 关联 Campaign ID。 */
  campaignId?: number | null

  /** 活动目标。 */
  objective?: string | null

  /** 负责人团队。 */
  ownerTeam?: string | null

  /** 开始时间。 */
  startAt?: string | null

  /** 结束时间。 */
  endAt?: string | null

  /** 渠道范围。 */
  channelScope?: string | null

  /** 关联人群引用。 */
  audienceRefs?: Record<string, unknown>

  /** 风控策略引用。 */
  riskPolicyRef?: string | null

  /** 实验引用。 */
  experimentRef?: string | null

  /** 报表看板引用。 */
  dashboardRef?: string | null

  /** 扩展元数据。 */
  metadata?: Record<string, unknown>

  /** 创建人。 */
  createdBy?: string | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 增长活动上线检查项。 */
export interface GrowthActivityReadinessCheck {
  /** 严重级别，例如 BLOCKER / WARNING / PASS。 */
  severity: string

  /** 检查对象类型，例如 REWARD_POOL / AUDIENCE / SCHEDULE。 */
  itemType: string

  /** 检查对象 key。 */
  itemKey: string

  /** 检查标题。 */
  title: string

  /** 检查原因或处置建议。 */
  reason: string

  /** 可跳转的配置路由。 */
  route?: string | null
}

/** 增长活动上线闸口汇总。 */
export interface GrowthActivityReadiness {
  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 活动业务 key。 */
  activityKey: string

  /** 活动类型。 */
  activityType: string

  /** 生成时间。 */
  generatedAt: string

  /** 闸口状态。 */
  status: string

  /** 是否可上线。 */
  productionReady: boolean

  /** 阻断项数量。 */
  blockerCount: number

  /** 警告项数量。 */
  warningCount: number

  /** 阻断项列表。 */
  blockers: GrowthActivityReadinessCheck[]

  /** 警告项列表。 */
  warnings: GrowthActivityReadinessCheck[]

  /** 全量检查项。 */
  checks: GrowthActivityReadinessCheck[]
}

/** 增长活动经营报表汇总。 */
export interface GrowthActivityReport {
  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 参与统计。 */
  participation: { totalParticipants: number; activeParticipants: number }

  /** 推荐关系统计。 */
  referral: { totalRelations: number; qualifiedRelations: number; pendingRelations: number; rejectedRelations: number }

  /** 奖励发放统计。 */
  grants: {
    totalGrants: number
    reservedGrants: number
    successGrants: number
    failedGrants: number
    canceledGrants: number
    redeemedGrants: number
    expiredGrants: number
    totalCost: number | string
  }

  /** 转化与 ROI 统计。 */
  conversion: { conversionCount: number; conversionAmount: number | string; roi: number | string }

  /** 任务完成统计。 */
  task: { totalProgress: number; completedProgress: number; completionRate: number | string }
}

/** 奖励池配置与库存/预算状态。 */
export interface GrowthRewardPool {
  /** 奖励池 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属活动 ID。 */
  activityId: number

  /** 奖励池 key。 */
  poolKey: string

  /** 奖励类型，例如 COUPON / POINTS / EXTERNAL。 */
  rewardType: string

  /** 发放渠道。 */
  grantChannel: string

  /** 优惠券类型 key。 */
  couponTypeKey?: string | null

  /** 会员权益 key。 */
  loyaltyRewardKey?: string | null

  /** 积分类型。 */
  pointsType?: string | null

  /** 外部合约 key。 */
  externalContractKey?: string | null

  /** 库存模式。 */
  inventoryMode?: string | null

  /** 总库存。 */
  totalInventory?: number | null

  /** 已预留库存。 */
  reservedInventory?: number | null

  /** 已发放库存。 */
  grantedInventory?: number | null

  /** 单用户领取上限。 */
  perUserLimit?: number | null

  /** 单推荐关系奖励上限。 */
  perReferralLimit?: number | null

  /** 预算金额。 */
  budgetAmount?: number | string | null

  /** 已预留金额。 */
  reservedAmount?: number | string | null

  /** 已发放金额。 */
  grantedAmount?: number | string | null

  /** 成本币种。 */
  costCurrency?: string | null

  /** 奖励池状态。 */
  status: string

  /** 是否低库存。 */
  inventoryLow?: boolean

  /** 扩展元数据。 */
  metadata?: Record<string, unknown>

  /** 创建人。 */
  createdBy?: string | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 奖励发放流水。 */
export interface GrowthRewardGrant {
  /** 发放流水 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 奖励池 ID。 */
  poolId: number

  /** 参与者 ID。 */
  participantId?: number | null

  /** 推荐关系 ID。 */
  referralRelationId?: number | null

  /** 任务进度 ID。 */
  taskProgressId?: number | null

  /** 发放原因。 */
  grantReason: string

  /** 发放状态，例如 RESERVED / SUCCESS / FAILED / CANCELED。 */
  status: string

  /** 发放幂等键，避免重复扣减库存或重复调用供应商。 */
  idempotencyKey: string

  /** 供应商请求快照。 */
  providerRequest?: Record<string, unknown>

  /** 供应商响应或错误证据。 */
  providerResponse?: Record<string, unknown>

  /** 成本金额。 */
  costAmount: number | string

  /** 创建人。 */
  createdBy?: string | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 推荐码记录。 */
export interface GrowthReferralCode {
  /** 推荐码 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 推荐人参与者 ID。 */
  participantId: number

  /** 推荐码。 */
  code: string

  /** 推荐码状态。 */
  status: string

  /** 创建人。 */
  createdBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null
}

/** 推荐关系记录，连接邀请人与被邀请用户。 */
export interface GrowthReferralRelation {
  /** 推荐关系 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 推荐码 ID。 */
  referralCodeId: number

  /** 邀请人参与者 ID。 */
  referrerParticipantId: number

  /** 被邀请用户 ID。 */
  inviteeUserId: string

  /** 推荐关系状态，例如 PENDING / QUALIFIED / REJECTED。 */
  status: string

  /** 风控证据。 */
  riskEvidence?: Record<string, unknown>

  /** 邀请人奖励流水 ID。 */
  inviterRewardGrantId?: number | null

  /** 被邀请人奖励流水 ID。 */
  inviteeRewardGrantId?: number | null

  /** 创建人。 */
  createdBy?: string | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 任务激励活动的任务定义。 */
export interface GrowthTaskDefinition {
  /** 任务 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 任务 key。 */
  taskKey: string

  /** 任务类型。 */
  taskType: string

  /** 完成策略。 */
  completionPolicy: string

  /** 重置策略。 */
  resetPolicy: string

  /** 关联奖励池 ID。 */
  rewardPoolId?: number | null

  /** 目标值。 */
  targetValue: number | string

  /** 任务状态。 */
  status: string

  /** 任务规则。 */
  rule?: Record<string, unknown>

  /** 创建人。 */
  createdBy?: string | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 参与者任务进度。 */
export interface GrowthTaskProgress {
  /** 任务进度 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 活动 ID。 */
  activityId: number

  /** 参与者 ID。 */
  participantId: number

  /** 任务 ID。 */
  taskId: number

  /** 当前进度值。 */
  progressValue: number | string

  /** 目标值。 */
  targetValue: number | string

  /** 进度状态。 */
  status: string

  /** 最近驱动进度变化的事件 key。 */
  lastEventKey?: string | null

  /** 进度证据。 */
  evidence?: Record<string, unknown>

  /** 完成后产生的奖励流水 ID。 */
  rewardGrantId?: number | null

  /** 更新人。 */
  updatedBy?: string | null

  /** 完成时间。 */
  completedAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 奖励池创建或更新命令，结构由不同 rewardType 决定。 */
export type GrowthRewardPoolCommand = Record<string, unknown>
/** 奖励发放命令，结构由发放原因和供应商决定。 */
export type GrowthRewardGrantCommand = Record<string, unknown>
/** 活动事件命令，用于记录参与、转化、任务等事件。 */
export type GrowthActivityEventCommand = Record<string, unknown>
/** 推荐关系创建或更新命令。 */
export type GrowthReferralRelationCommand = Record<string, unknown>
/** 推荐关系达标/拒绝命令。 */
export type GrowthReferralQualificationCommand = Record<string, unknown>
/** 任务定义创建或更新命令。 */
export type GrowthTaskDefinitionCommand = Record<string, unknown>
/** 任务进度记录命令。 */
export type GrowthTaskProgressCommand = Record<string, unknown>

const base = '/canvas/growth-activities'

export const growthActivityApi = {
  /** 查询增长活动列表，支持活动、排期、上线闸口和发放健康筛选。 */
  listActivities: (params?: GrowthActivityQuery) =>
    http.get<R<GrowthActivity[]>, R<GrowthActivity[]>>(base, { params }),

  /** 创建或更新增长活动，后端按 activityKey 做 upsert。 */
  upsertActivity: (payload: GrowthActivityCommand) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(base, payload),

  /** 查询单个活动。 */
  getActivity: (activityId: number) =>
    http.get<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}`),

  /** 发布活动，从草稿进入可运行状态。 */
  publishActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/publish`, {}),

  /** 暂停活动，保留配置但停止继续参与和发放。 */
  pauseActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/pause`, {}),

  /** 关闭活动，结束参与、任务和奖励发放链路。 */
  closeActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/close`, {}),

  /** 查询上线闸口检查结果。 */
  getReadiness: (activityId: number) =>
    http.get<R<GrowthActivityReadiness>, R<GrowthActivityReadiness>>(`${base}/${activityId}/readiness`),

  /** 查询活动经营报表。 */
  getReport: (activityId: number) =>
    http.get<R<GrowthActivityReport>, R<GrowthActivityReport>>(`${base}/${activityId}/report`),

  /** 查询活动奖励池。 */
  listRewardPools: (activityId: number) =>
    http.get<R<GrowthRewardPool[]>, R<GrowthRewardPool[]>>(`${base}/${activityId}/reward-pools`),

  /** 创建或更新奖励池。 */
  upsertRewardPool: (activityId: number, payload: GrowthRewardPoolCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/reward-pools`, payload),

  /** 查询奖励发放流水。 */
  listGrants: (activityId: number) =>
    http.get<R<GrowthRewardGrant[]>, R<GrowthRewardGrant[]>>(`${base}/${activityId}/grants`),

  /** 创建奖励发放流水。 */
  createGrant: (activityId: number, payload: GrowthRewardGrantCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants`, payload),

  /** 重试失败发放，通常重新调用供应商并保留原幂等语义。 */
  retryGrant: (activityId: number, grantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/retry`, {}),

  /** 对账单条发放流水，用供应商状态修正本地状态或重新入队。 */
  reconcileGrant: (activityId: number, grantId: number, payload: Record<string, unknown>) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/reconcile`, payload),

  /** 取消发放流水，释放库存或预算占用。 */
  cancelGrant: (activityId: number, grantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/cancel`, {}),

  /** 查询活动事件时间线，可按事件类型筛选。 */
  listEvents: (activityId: number, params?: { eventType?: string; limit?: number }) =>
    http.get<R<unknown[]>, R<unknown[]>>(`${base}/${activityId}/events`, { params }),

  /** 记录活动事件，驱动参与、推荐、任务或转化状态变更。 */
  recordEvent: (activityId: number, payload: GrowthActivityEventCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/events`, payload),

  /** 查询推荐码。 */
  listReferralCodes: (activityId: number) =>
    http.get<R<GrowthReferralCode[]>, R<GrowthReferralCode[]>>(`${base}/${activityId}/referral-codes`),

  /** 为参与者生成推荐码。 */
  generateReferralCode: (activityId: number, participantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referral-codes`, { participantId }),

  /** 查询推荐关系。 */
  listReferralRelations: (activityId: number) =>
    http.get<R<GrowthReferralRelation[]>, R<GrowthReferralRelation[]>>(`${base}/${activityId}/referrals`),

  /** 创建或更新推荐关系。 */
  upsertReferralRelation: (activityId: number, payload: GrowthReferralRelationCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referrals`, payload),

  /** 审核推荐关系是否达标，并可能触发邀请人与被邀请人奖励。 */
  qualifyReferral: (activityId: number, relationId: number, payload: GrowthReferralQualificationCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referrals/${relationId}/qualify`, payload),

  /** 查询任务定义。 */
  listTaskDefinitions: (activityId: number) =>
    http.get<R<GrowthTaskDefinition[]>, R<GrowthTaskDefinition[]>>(`${base}/${activityId}/tasks`),

  /** 创建或更新任务定义。 */
  upsertTaskDefinition: (activityId: number, payload: GrowthTaskDefinitionCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/tasks`, payload),

  /** 查询任务进度。 */
  listTaskProgress: (activityId: number) =>
    http.get<R<GrowthTaskProgress[]>, R<GrowthTaskProgress[]>>(`${base}/${activityId}/task-progress`),

  /** 记录任务进度事件，可能触发任务完成和奖励发放。 */
  recordTaskProgress: (activityId: number, payload: GrowthTaskProgressCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/task-progress`, payload),
}
