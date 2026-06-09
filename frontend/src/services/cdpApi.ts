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
  /** 执行记录 ID。 */
  id: string

  /** 租户 ID。 */
  tenantId?: number | null

  /** 画布 ID。 */
  canvasId: number

  /** 画布版本 ID。 */
  versionId?: number | null

  /** 用户 ID。 */
  userId: string

  /** 性能压测运行 ID，普通执行通常为空。 */
  perfRunId?: string | null

  /** 触发类型，例如事件触发、手工触发或定时触发。 */
  triggerType?: string | null

  /** 执行状态码，按后端执行引擎定义解释。 */
  status: number

  /** 执行结果摘要。 */
  result?: string | null

  /** 最近一次去重键，用于排查幂等命中。 */
  lastDedupKey?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
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
  /** 计算画像属性编码。 */
  attrCode: string

  /** 页面展示名。 */
  displayName: string

  /** 计算结果值类型。 */
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'

  /** 计算方式：规则、表达式或 SQL。 */
  computeType: 'RULE' | 'EXPR' | 'SQL'

  /** 计算表达式 JSON，具体结构由后端解析。 */
  expressionJson: string

  /** 刷新模式：手动运行或事件触发。 */
  refreshMode: 'MANUAL' | 'EVENT'
}

export interface ComputedProfileAttributeRow extends ComputedProfileAttributePayload {
  /** 属性记录 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId?: number | null

  /** 属性状态，例如 DRAFT / ACTIVE / PAUSED。 */
  status: string

  /** 创建人。 */
  createdBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

export interface ComputedProfilePreviewSample {
  /** 样本用户 ID。 */
  userId: string

  /** 预览前画像值。 */
  oldValue?: string | null

  /** 按当前规则计算出的新画像值。 */
  newValue?: string | null
}

export interface ComputedProfilePreviewResult {
  /** 扫描用户数。 */
  scannedCount: number

  /** 命中规则用户数。 */
  matchedCount: number

  /** 值会发生变化的用户数。 */
  changedCount: number

  /** 值保持不变的用户数。 */
  unchangedCount: number

  /** 预览样本。 */
  samples: ComputedProfilePreviewSample[]
}

export interface ComputedProfileRunResult {
  /** 运行记录 ID。 */
  runId?: number | null

  /** 运行状态。 */
  status: string

  /** 扫描用户数。 */
  scannedCount: number

  /** 命中规则用户数。 */
  matchedCount: number

  /** 实际更新用户数。 */
  changedCount: number

  /** 未变化用户数。 */
  unchangedCount: number
}

export interface ComputedProfileRunRow extends ComputedProfileRunResult {
  /** 运行记录主键。 */
  id: number

  /** 画像属性 ID。 */
  attrId: number

  /** 触发本次运行的源事件 ID，手动运行时通常为空。 */
  sourceEventId?: string | null

  /** 运行失败摘要。 */
  errorMessage?: string | null

  /** 开始时间。 */
  startedAt?: string | null

  /** 结束时间。 */
  finishedAt?: string | null
}

export interface ComputedProfileChangeLogRow {
  /** 变更日志 ID。 */
  id: number

  /** 画像属性编码。 */
  attrCode: string

  /** 用户 ID。 */
  userId: string

  /** 变更前值。 */
  oldValue?: string | null

  /** 变更后值。 */
  newValue?: string | null

  /** 来源运行 ID。 */
  sourceRunId: number

  /** 变更时间。 */
  changedAt?: string | null
}

export interface ComputedTagPayload {
  /** 计算标签编码。 */
  tagCode: string

  /** 标签展示名。 */
  displayName: string

  /** 标签值类型。 */
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'

  /** 计算方式：规则、表达式或 SQL。 */
  computeType: 'RULE' | 'EXPR' | 'SQL'

  /** 计算表达式 JSON。 */
  expressionJson: string

  /** 刷新模式：手动运行或事件触发。 */
  refreshMode: 'MANUAL' | 'EVENT'

  /** 依赖的事件、画像或标签编码，用于血缘和影响检查。 */
  dependencies: string[]
}

export interface ComputedTagRow extends ComputedTagPayload {
  /** 标签记录 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId?: number | null

  /** 标签状态。 */
  status: string

  /** 创建人。 */
  createdBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

export interface ComputedTagPreviewSample {
  /** 样本用户 ID。 */
  userId: string

  /** 按当前规则计算出的标签值。 */
  tagValue?: string | null
}

export interface ComputedTagPreviewResult {
  /** 扫描用户数。 */
  scannedCount: number

  /** 命中规则用户数。 */
  matchedCount: number

  /** 预览样本。 */
  samples: ComputedTagPreviewSample[]
}

export interface ComputedTagRunSummary {
  /** 运行记录 ID。 */
  runId?: number | null

  /** 运行状态。 */
  status: string

  /** 扫描用户数。 */
  scannedCount: number

  /** 命中规则用户数。 */
  matchedCount: number

  /** 已更新标签数。 */
  updatedCount: number

  /** 跳过用户数。 */
  skippedCount: number

  /** 失败用户数。 */
  failedCount: number

  /** 依赖环路路径，存在时表示本次运行被循环依赖保护拦截。 */
  cyclePath?: string | null
}

export interface ComputedTagRunRow extends ComputedTagRunSummary {
  /** 运行记录主键。 */
  id: number

  /** 标签编码。 */
  tagCode: string

  /** 运行失败摘要。 */
  errorMessage?: string | null

  /** 开始时间。 */
  startedAt?: string | null

  /** 结束时间。 */
  finishedAt?: string | null
}

export interface LineageImpact {
  /** 受影响对象类型，例如 TAG / AUDIENCE / CANVAS。 */
  objectType: string

  /** 受影响对象 ID。 */
  objectId?: string | number | null

  /** 受影响对象名称。 */
  objectName?: string | null

  /** 引用路径，用于说明依赖链。 */
  referencePath: string
}

export interface ImpactCheck {
  /** 是否允许执行变更。 */
  allowed: boolean

  /** 禁止或警告原因。 */
  reason?: string | null

  /** 受影响对象列表。 */
  impacts: LineageImpact[]
}

export interface RealtimeAudienceEventPayload {
  /** 源事件 ID，用于实时人群事件幂等。 */
  sourceEventId: string

  /** 用户 ID。 */
  userId: string

  /** 事件发生时间。 */
  eventTime?: string | null

  /** 事件属性，用于实时人群规则匹配。 */
  properties?: Record<string, unknown>

  /** 不再匹配规则时是否移出人群。 */
  removeOnNoMatch?: boolean
}

export interface RealtimeAudienceEventResult {
  /** 处理状态。 */
  status: string

  /** 人群成员操作，例如 ADD / REMOVE / NOOP。 */
  operation?: string | null

  /** 人群 ID。 */
  audienceId?: number | string | null

  /** 用户 ID。 */
  userId?: string | null

  /** 源事件 ID。 */
  sourceEventId?: string | null
}

export interface AudienceOverlapResult {
  /** 左侧人群规模。 */
  leftCount: number

  /** 右侧人群规模。 */
  rightCount: number

  /** 交集人数。 */
  intersectionCount: number

  /** 交集占左侧人群比例。 */
  leftPercentage: number

  /** 交集占右侧人群比例。 */
  rightPercentage: number
}

export interface AudienceSetOperationResult {
  /** 集合运算状态。 */
  status: string

  /** 未生成结果人群时的原因。 */
  reason?: string | null

  /** 运算结果规模。 */
  resultSize?: number | null

  /** 安全规模上限，超过时后端会拒绝或截断。 */
  safeLimit?: number | null

  /** 结果人群 ID。 */
  resultAudienceId?: number | string | null
}

export interface AudienceSnapshotRow {
  /** 快照 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId?: number | string | null

  /** 人群 ID。 */
  audienceId: number | string

  /** 快照估算规模。 */
  estimatedSize: number

  /** 位图存储 key，用于后端快速集合运算。 */
  bitmapKey?: string | null

  /** 快照来源，例如 MANUAL / SCHEDULED。 */
  snapshotSource: string

  /** 创建人。 */
  createdBy?: string | null

  /** 创建时间。 */
  createdAt?: string | null
}

export interface AudienceSnapshotResult {
  /** 人群 ID。 */
  audienceId: number | string

  /** 估算规模。 */
  estimatedSize: number

  /** 位图存储 key。 */
  bitmapKey: string

  /** 快照来源。 */
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
    /** 查询计算画像属性。 */
    list: () =>
      client.get<R<ComputedProfileAttributeRow[]>, R<ComputedProfileAttributeRow[]>>('/cdp/computed-profile-attributes'),
    /** 创建计算画像属性，表达式保持 JSON 字符串透传给后端解析。 */
    create: (payload: ComputedProfileAttributePayload) =>
      client.post<R<ComputedProfileAttributeRow>, R<ComputedProfileAttributeRow>>('/cdp/computed-profile-attributes', payload),
    /** 预览计算画像影响范围，不落库更新用户画像。 */
    preview: (id: number) =>
      client.post<R<ComputedProfilePreviewResult>, R<ComputedProfilePreviewResult>>(`/cdp/computed-profile-attributes/${id}/preview`),
    /** 启用计算画像属性，使手动或事件刷新可生效。 */
    activate: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/computed-profile-attributes/${id}/activate`),
    /** 暂停计算画像属性，保留配置但停止刷新。 */
    pause: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/computed-profile-attributes/${id}/pause`),
    /** 手动运行计算画像属性并写入变更结果。 */
    run: (id: number) =>
      client.post<R<ComputedProfileRunResult>, R<ComputedProfileRunResult>>(`/cdp/computed-profile-attributes/${id}/run`),
    /** 查询计算画像运行历史。 */
    runs: (id: number) =>
      client.get<R<ComputedProfileRunRow[]>, R<ComputedProfileRunRow[]>>(`/cdp/computed-profile-attributes/${id}/runs`),
    /** 查询画像变更日志，可按用户 ID 收窄排查范围。 */
    changes: (id: number, userId?: string) =>
      client.get<R<ComputedProfileChangeLogRow[]>, R<ComputedProfileChangeLogRow[]>>(
        `/cdp/computed-profile-attributes/${id}/changes`,
        userId ? { params: { userId } } : undefined,
      ),
  },
  computedTags: {
    /** 查询计算标签。 */
    list: () =>
      client.get<R<ComputedTagRow[]>, R<ComputedTagRow[]>>('/cdp/computed-tags'),
    /** 创建计算标签，包含依赖声明以支持血缘检查。 */
    create: (payload: ComputedTagPayload) =>
      client.post<R<ComputedTagRow>, R<ComputedTagRow>>('/cdp/computed-tags', payload),
    /** 预览计算标签命中样本，不写入用户标签。 */
    preview: (tagCode: string) =>
      client.post<R<ComputedTagPreviewResult>, R<ComputedTagPreviewResult>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/preview`),
    /** 启用计算标签。 */
    activate: (tagCode: string) =>
      client.post<R<void>, R<void>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/activate`),
    /** 暂停计算标签刷新。 */
    pause: (tagCode: string) =>
      client.post<R<void>, R<void>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/pause`),
    /** 手动运行计算标签，后端负责循环依赖保护和失败计数。 */
    run: (tagCode: string) =>
      client.post<R<ComputedTagRunSummary>, R<ComputedTagRunSummary>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/run`),
    /** 查询计算标签运行历史。 */
    runs: (tagCode: string) =>
      client.get<R<ComputedTagRunRow[]>, R<ComputedTagRunRow[]>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/runs`),
    /** 查询标签血缘影响对象。 */
    lineage: (tagCode: string) =>
      client.get<R<LineageImpact[]>, R<LineageImpact[]>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/lineage`),
    /** 变更标签值类型前做影响检查，防止破坏下游人群或画布依赖。 */
    impactCheck: (tagCode: string, oldValueType: string, newValueType: string) =>
      client.post<R<ImpactCheck>, R<ImpactCheck>>(`/cdp/computed-tags/${encodeURIComponent(tagCode)}/impact-check`, {
        oldValueType,
        newValueType,
      }),
  },
  realtimeAudiences: {
    /** 处理单个实时事件，驱动用户入群、出群或保持不变。 */
    processEvent: (audienceId: number | string, payload: RealtimeAudienceEventPayload) =>
      client.post<R<RealtimeAudienceEventResult>, R<RealtimeAudienceEventResult>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/events`,
        payload,
      ),
    /** 创建实时人群快照，产出位图 key 供集合运算使用。 */
    createSnapshot: (audienceId: number | string) =>
      client.post<R<AudienceSnapshotResult>, R<AudienceSnapshotResult>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/snapshot`,
      ),
    /** 计算两个人群交集和占比。 */
    overlap: (leftId: number | string, rightId: number | string) =>
      client.get<R<AudienceOverlapResult>, R<AudienceOverlapResult>>(
        `/cdp/audiences/${encodeURIComponent(String(leftId))}/overlap/${encodeURIComponent(String(rightId))}`,
      ),
    /** 合并两个人群并按后端安全上限生成结果人群。 */
    merge: (leftId: number | string, rightId: number | string) =>
      client.post<R<AudienceSetOperationResult>, R<AudienceSetOperationResult>>(
        '/cdp/audiences/merge',
        null,
        { params: { leftId, rightId } },
      ),
    /** 从基础人群中排除另一人群，生成差集结果。 */
    exclude: (baseId: number | string, excludedId: number | string) =>
      client.post<R<AudienceSetOperationResult>, R<AudienceSetOperationResult>>(
        '/cdp/audiences/exclude',
        null,
        { params: { baseId, excludedId } },
      ),
    /** 查询实时人群历史快照。 */
    snapshots: (audienceId: number | string) =>
      client.get<R<AudienceSnapshotRow[]>, R<AudienceSnapshotRow[]>>(
        `/cdp/realtime-audiences/${encodeURIComponent(String(audienceId))}/snapshots`,
      ),
  },
  }
}

export const cdpApi = createCdpApi()
