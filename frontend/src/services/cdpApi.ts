/**
 * 服务职责：CDP 用户画像、标签和画布用户明细 API 封装。
 *
 * 维护说明：所有 userId/tagCode 路径参数在这里统一 encode，避免页面层遗漏特殊字符转义。
 */
import http from './api'
import type { R } from '../types'

/** CDP 用户基础画像，详情页顶部信息和用户列表共用。 */
export interface CdpUserDetail {
  /** 业务用户 ID，作为 CDP 侧主键和路径参数。 */
  userId: string

  /** 用户展示名，缺失时页面通常回退为 userId。 */
  displayName: string

  /** 手机号。 */
  phone?: string | null

  /** 邮箱。 */
  email?: string | null

  /** 用户状态，例如 ACTIVE / DISABLED 等后端定义值。 */
  status: string

  /** 扩展属性 JSON 字符串，详情页可做调试展示。 */
  propertiesJson?: string | null

  /** 首次进入 CDP 的时间。 */
  firstSeenAt?: string | null

  /** 最近一次活跃或同步时间。 */
  lastSeenAt?: string | null
}

/** 用户当前生效标签。 */
export interface CdpUserTag {
  /** 标签编码，和标签中心定义保持一致。 */
  tagCode: string

  /** 标签名称。 */
  tagName: string

  /** 标签值；枚举/文本/数字标签按字符串展示。 */
  tagValue?: string | null

  /** 标签值类型。 */
  valueType?: string | null

  /** 标签来源类型，例如人工、导入、画布节点。 */
  sourceType?: string | null

  /** 标签状态。 */
  status: string

  /** 生效时间。 */
  effectiveAt?: string | null

  /** 过期时间。 */
  expiresAt?: string | null

  /** 最近更新时间。 */
  updatedAt?: string | null
}

/** 用户标签变更历史，用于详情页审计追踪。 */
export interface CdpUserTagHistory {
  /** 变更的标签编码。 */
  tagCode: string

  /** 变更前标签值。 */
  oldValue?: string | null

  /** 变更后标签值。 */
  newValue?: string | null

  /** 操作类型，例如 SET / REMOVE。 */
  operation: string

  /** 来源类型。 */
  sourceType?: string | null

  /** 来源引用 ID，例如任务 ID 或导入批次 ID。 */
  sourceRefId?: string | null

  /** 操作原因。 */
  reason?: string | null

  /** 操作人。 */
  operator?: string | null

  /** 操作时间。 */
  operatedAt?: string | null
}

/** 用户在某个画布中的执行摘要。 */
export interface CdpUserCanvasSummary {
  /** 画布 ID。 */
  canvasId: number

  /** 画布名称。 */
  canvasName: string

  /** 总执行次数。 */
  executionCount: number

  /** 成功次数。 */
  successCount: number

  /** 失败次数。 */
  failedCount: number

  /** 最近一次执行状态。 */
  latestStatus: string

  /** 首次进入该画布时间。 */
  firstEnteredAt?: string | null

  /** 最近进入该画布时间。 */
  lastEnteredAt?: string | null
}

/** 用户洞察详情，聚合画像、标签和参与画布。 */
export interface CanvasUserDetail {
  /** 用户 ID。 */
  userId: string

  /** 用户基础画像。 */
  profile: CdpUserDetail

  /** 当前标签集合。 */
  tags: CdpUserTag[]

  /** 参与过的画布执行摘要。 */
  canvasRows: CdpUserCanvasSummary[]
}

/** 画布命中用户列表行。 */
export interface CanvasUserRow {
  /** 用户 ID。 */
  userId: string

  /** 用户展示名。 */
  displayName: string

  /** 在当前查询范围内的执行次数。 */
  executionCount: number

  /** 成功次数。 */
  successCount: number

  /** 失败次数。 */
  failedCount: number

  /** 最近一次执行状态。 */
  latestStatus: string

  /** 首次进入时间。 */
  firstEnteredAt?: string | null

  /** 最近进入时间。 */
  lastEnteredAt?: string | null

  /** 当前标签快照。 */
  tags: CdpUserTag[]
}

/** 用户在画布中的单次执行记录。 */
export interface CanvasExecutionRow {
  id: string
  tenantId?: number | null
  canvasId: number
  versionId?: number | null
  userId: string
  perfRunId?: string | null
  triggerType?: string | null
  status: number
  result?: string | null
  lastDedupKey?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

/** 单用户写标签请求体。 */
export interface TagWritePayload {
  /** 标签编码。 */
  tagCode: string

  /** 标签值；删除或无值标签可为空。 */
  tagValue?: string | null

  /** 打标原因。 */
  reason?: string | null

  /** 过期时间。 */
  expiresAt?: string | null

  /** 来源类型。 */
  sourceType?: string | null

  /** 来源引用 ID。 */
  sourceRefId?: string | null

  /** 操作人。 */
  operator?: string | null

  /** 幂等键，防止重复提交导致重复历史。 */
  idempotencyKey?: string | null
}

/** 批量打标/批量移除标签请求体。 */
export interface BatchTagPayload {
  /** 批处理类型。 */
  operationType: 'BATCH_SET' | 'BATCH_REMOVE'

  /** 标签编码。 */
  tagCode: string

  /** 标签值，批量移除时通常为空。 */
  tagValue?: string | null

  /** 目标用户 ID 列表。 */
  userIds: string[]

  /** 操作原因。 */
  reason?: string | null

  /** 操作人。 */
  operator?: string | null
}

/** 批量标签操作任务摘要。 */
export interface CdpTagOperation {
  /** 操作记录 ID。 */
  id: number

  /** 操作类型。 */
  operationType: string

  /** 标签编码。 */
  tagCode: string

  /** 标签值。 */
  tagValue?: string | null

  /** 总用户数。 */
  totalCount: number

  /** 成功数量。 */
  successCount: number

  /** 失败数量。 */
  failCount: number

  /** 当前处理状态。 */
  status: string

  /** 失败摘要。 */
  errorMsg?: string | null

  /** 创建人。 */
  createdBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

export interface ComputedProfileAttributePayload {
  attrCode: string
  displayName: string
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'
  computeType: 'RULE' | 'EXPR' | 'SQL'
  expressionJson: string
  refreshMode: 'MANUAL' | 'EVENT'
}

export interface ComputedProfileAttributeRow extends ComputedProfileAttributePayload {
  id: number
  tenantId?: number | null
  status: string
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ComputedProfilePreviewSample {
  userId: string
  oldValue?: string | null
  newValue?: string | null
}

export interface ComputedProfilePreviewResult {
  scannedCount: number
  matchedCount: number
  changedCount: number
  unchangedCount: number
  samples: ComputedProfilePreviewSample[]
}

export interface ComputedProfileRunResult {
  runId?: number | null
  status: string
  scannedCount: number
  matchedCount: number
  changedCount: number
  unchangedCount: number
}

export interface ComputedProfileRunRow extends ComputedProfileRunResult {
  id: number
  attrId: number
  sourceEventId?: string | null
  errorMessage?: string | null
  startedAt?: string | null
  finishedAt?: string | null
}

export interface ComputedProfileChangeLogRow {
  id: number
  attrCode: string
  userId: string
  oldValue?: string | null
  newValue?: string | null
  sourceRunId: number
  changedAt?: string | null
}

export interface ComputedTagPayload {
  tagCode: string
  displayName: string
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'
  computeType: 'RULE' | 'EXPR' | 'SQL'
  expressionJson: string
  refreshMode: 'MANUAL' | 'EVENT'
  dependencies: string[]
}

export interface ComputedTagRow extends ComputedTagPayload {
  id: number
  tenantId?: number | null
  status: string
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ComputedTagPreviewSample {
  userId: string
  tagValue?: string | null
}

export interface ComputedTagPreviewResult {
  scannedCount: number
  matchedCount: number
  samples: ComputedTagPreviewSample[]
}

export interface ComputedTagRunSummary {
  runId?: number | null
  status: string
  scannedCount: number
  matchedCount: number
  updatedCount: number
  skippedCount: number
  failedCount: number
  cyclePath?: string | null
}

export interface ComputedTagRunRow extends ComputedTagRunSummary {
  id: number
  tagCode: string
  errorMessage?: string | null
  startedAt?: string | null
  finishedAt?: string | null
}

export interface LineageImpact {
  objectType: string
  objectId?: string | number | null
  objectName?: string | null
  referencePath: string
}

export interface ImpactCheck {
  allowed: boolean
  reason?: string | null
  impacts: LineageImpact[]
}

export interface RealtimeAudienceEventPayload {
  sourceEventId: string
  userId: string
  eventTime?: string | null
  properties?: Record<string, unknown>
  removeOnNoMatch?: boolean
}

export interface RealtimeAudienceEventResult {
  status: string
  operation?: string | null
  audienceId?: number | string | null
  userId?: string | null
  sourceEventId?: string | null
}

export interface AudienceOverlapResult {
  leftCount: number
  rightCount: number
  intersectionCount: number
  leftPercentage: number
  rightPercentage: number
}

export interface AudienceSetOperationResult {
  status: string
  reason?: string | null
  resultSize?: number | null
  safeLimit?: number | null
  resultAudienceId?: number | string | null
}

export interface AudienceSnapshotRow {
  id: number
  tenantId?: number | string | null
  audienceId: number | string
  estimatedSize: number
  bitmapKey?: string | null
  snapshotSource: string
  createdBy?: string | null
  createdAt?: string | null
}

export interface AudienceSnapshotResult {
  audienceId: number | string
  estimatedSize: number
  bitmapKey: string
  snapshotSource: string
}

/** CDP 用户、标签和画布用户洞察接口集合。 */
export function createCdpApi(client = http) {
  return {
  /** 查询 CDP 用户列表，可按关键字搜索 userId、名称或联系方式。 */
  listUsers: (keyword?: string) =>
    client.get<R<CanvasUserRow[]>, R<CanvasUserRow[]>>('/cdp/users', { params: keyword ? { keyword } : undefined }),

  /** 查询用户基础画像。 */
  getUser: (userId: string) =>
    client.get<R<CdpUserDetail>, R<CdpUserDetail>>(`/cdp/users/${encodeURIComponent(userId)}`),

  /** 查询用户完整洞察：画像、标签、参与画布。 */
  getUserInsight: (userId: string) =>
    client.get<R<CanvasUserDetail>, R<CanvasUserDetail>>(`/cdp/users/${encodeURIComponent(userId)}/insight`),

  /** 查询用户当前标签集合。 */
  listUserTags: (userId: string) =>
    client.get<R<CdpUserTag[]>, R<CdpUserTag[]>>(`/cdp/users/${encodeURIComponent(userId)}/tags`),

  /** 查询用户标签变更历史。 */
  listUserTagHistory: (userId: string) =>
    client.get<R<CdpUserTagHistory[]>, R<CdpUserTagHistory[]>>(`/cdp/users/${encodeURIComponent(userId)}/tag-history`),

  /** 给单个用户新增或更新标签。 */
  addUserTag: (userId: string, body: TagWritePayload) =>
    client.post<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags`, body),

  /** 删除单个用户的指定标签。 */
  removeUserTag: (userId: string, tagCode: string) =>
    client.delete<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags/${encodeURIComponent(tagCode)}`),

  /** 创建批量标签操作。 */
  createBatchTagOperation: (body: BatchTagPayload) =>
    client.post<R<CdpTagOperation>, R<CdpTagOperation>>('/cdp/tag-operations', body),

  /** 查询最近的批量标签操作。 */
  listTagOperations: (limit = 20) =>
    client.get<R<CdpTagOperation[]>, R<CdpTagOperation[]>>('/cdp/tag-operations', { params: { limit } }),

  /** 查询单个批量标签操作详情。 */
  getBatchTagOperation: (id: number) =>
    client.get<R<CdpTagOperation>, R<CdpTagOperation>>(`/cdp/tag-operations/${id}`),

  /** 仅重试批量操作中的失败用户。 */
  retryFailedTagOperation: (id: number) =>
    client.post<R<CdpTagOperation>, R<CdpTagOperation>>(`/cdp/tag-operations/${id}/retry-failed`),

  /** 查询某个画布命中过的用户列表。 */
  listCanvasUsers: (canvasId: number) =>
    client.get<R<CanvasUserRow[]>, R<CanvasUserRow[]>>(`/canvas/${canvasId}/users`),

  /** 查询某个画布下单个用户的汇总行。 */
  getCanvasUser: (canvasId: number, userId: string) =>
    client.get<R<CanvasUserRow>, R<CanvasUserRow>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}`),

  /** 查询某个用户在某个画布下的执行明细。 */
  listCanvasUserExecutions: (canvasId: number, userId: string) =>
    client.get<R<CanvasExecutionRow[]>, R<CanvasExecutionRow[]>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}/executions`),

  computedProfiles: {
    list: () =>
      client.get<R<ComputedProfileAttributeRow[]>, R<ComputedProfileAttributeRow[]>>('/cdp/computed-profile-attributes'),
    create: (payload: ComputedProfileAttributePayload) =>
      client.post<R<ComputedProfileAttributeRow>, R<ComputedProfileAttributeRow>>('/cdp/computed-profile-attributes', payload),
    preview: (id: number) =>
      client.post<R<ComputedProfilePreviewResult>, R<ComputedProfilePreviewResult>>(`/cdp/computed-profile-attributes/${id}/preview`),
    activate: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/computed-profile-attributes/${id}/activate`),
    pause: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/computed-profile-attributes/${id}/pause`),
    run: (id: number) =>
      client.post<R<ComputedProfileRunResult>, R<ComputedProfileRunResult>>(`/cdp/computed-profile-attributes/${id}/run`),
    runs: (id: number) =>
      client.get<R<ComputedProfileRunRow[]>, R<ComputedProfileRunRow[]>>(`/cdp/computed-profile-attributes/${id}/runs`),
    changes: (id: number, userId?: string) =>
      client.get<R<ComputedProfileChangeLogRow[]>, R<ComputedProfileChangeLogRow[]>>(
        `/cdp/computed-profile-attributes/${id}/changes`,
        userId ? { params: { userId } } : undefined,
      ),
  },
  computedTags: {
    list: () =>
      client.get<R<ComputedTagRow[]>, R<ComputedTagRow[]>>('/cdp/computed-tags'),
    create: (payload: ComputedTagPayload) =>
      client.post<R<ComputedTagRow>, R<ComputedTagRow>>('/cdp/computed-tags', payload),
    preview: (tagCode: string) =>
      client.post<R<ComputedTagPreviewResult>, R<ComputedTagPreviewResult>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/preview`),
    activate: (tagCode: string) =>
      client.post<R<void>, R<void>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/activate`),
    pause: (tagCode: string) =>
      client.post<R<void>, R<void>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/pause`),
    run: (tagCode: string) =>
      client.post<R<ComputedTagRunSummary>, R<ComputedTagRunSummary>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/run`),
    runs: (tagCode: string) =>
      client.get<R<ComputedTagRunRow[]>, R<ComputedTagRunRow[]>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/runs`),
    lineage: (tagCode: string) =>
      client.get<R<LineageImpact[]>, R<LineageImpact[]>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/lineage`),
    impactCheck: (tagCode: string, oldValueType: string, newValueType: string) =>
      client.post<R<ImpactCheck>, R<ImpactCheck>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/impact-check`, {
        oldValueType,
        newValueType,
      }),
  },
  realtimeAudiences: {
    processEvent: (audienceId: number | string, payload: RealtimeAudienceEventPayload) =>
      client.post<R<RealtimeAudienceEventResult>, R<RealtimeAudienceEventResult>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/events`,
        payload,
      ),
    createSnapshot: (audienceId: number | string) =>
      client.post<R<AudienceSnapshotResult>, R<AudienceSnapshotResult>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/snapshot`,
      ),
    overlap: (leftId: number | string, rightId: number | string) =>
      client.get<R<AudienceOverlapResult>, R<AudienceOverlapResult>>(
        `/cdp/audiences/${encodeURIComponent(String(leftId))}/overlap/${encodeURIComponent(String(rightId))}`,
      ),
    merge: (leftId: number | string, rightId: number | string) =>
      client.post<R<AudienceSetOperationResult>, R<AudienceSetOperationResult>>(
        '/cdp/audiences/merge',
        null,
        { params: { leftId, rightId } },
      ),
    exclude: (baseId: number | string, excludedId: number | string) =>
      client.post<R<AudienceSetOperationResult>, R<AudienceSetOperationResult>>(
        '/cdp/audiences/exclude',
        null,
        { params: { baseId, excludedId } },
      ),
    snapshots: (audienceId: number | string) =>
      client.get<R<AudienceSnapshotRow[]>, R<AudienceSnapshotRow[]>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/snapshots`,
      ),
  },
  }
}

export const cdpApi = createCdpApi()
