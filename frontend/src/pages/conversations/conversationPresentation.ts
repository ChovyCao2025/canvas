import type {
  ConversationMessage,
  ConversationSession,
} from './conversationCorePresentation'
import { formatDateTime } from './conversationCorePresentation'

export type {
  ConversationIngressPayload,
  ConversationIngressResponse,
  ConversationMessage,
  ConversationSession,
} from './conversationCorePresentation'

export {
  conversationMessageLine,
  formatConversationDuplicate,
  formatConversationStatus,
  formatDateTime,
} from './conversationCorePresentation'

export interface ConversationWorkItem {
  /** 工单 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 关联会话 ID。 */
  sessionId: number

  /** 关联客户资料 ID。 */
  contactProfileId: number

  /** 客户业务 ID。 */
  userId: string

  /** 会话渠道。 */
  channel: string

  /** 渠道供应商。 */
  provider: string

  /** 工单标题，缺失时用客户或会话信息兜底。 */
  subject?: string

  /** 工单状态，例如 OPEN / PENDING / SNOOZED / RESOLVED。 */
  status: string

  /** 工单优先级。 */
  priority: string

  /** 当前处理人。 */
  assignedTo?: string

  /** 当前处理团队。 */
  assignedTeam?: string

  /** 工单来源，例如自动升级、人工创建或渠道转入。 */
  source: string

  /** SLA 截止时间。 */
  slaDueAt?: string

  /** 下次跟进时间。 */
  nextFollowUpAt?: string

  /** 最近客户消息时间。 */
  lastCustomerMessageAt?: string

  /** 最近坐席操作时间。 */
  lastOperatorActivityAt?: string

  /** 工单标签。 */
  tags: string[]

  /** 工单扩展属性。 */
  attributes: Record<string, unknown>

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string
}

export interface ConversationContactProfile {
  /** 客户资料 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 客户业务 ID。 */
  userId: string

  /** 客户展示名。 */
  displayName?: string

  /** 外部联系人 ID。 */
  externalContactId?: string

  /** 私域来源，例如企微、公众号或社群。 */
  privateDomainSource?: string

  /** 客户归属人。 */
  owner?: string

  /** 客户生命周期阶段。 */
  lifecycleStage?: string

  /** 客户标签。 */
  tags: string[]

  /** 客户扩展属性。 */
  attributes: Record<string, unknown>

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string
}

export interface ConversationSopTask {
  /** SOP 任务 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属工单 ID。 */
  workItemId: number

  /** SOP 任务编码，用于识别标准动作。 */
  taskKey: string

  /** 任务标题。 */
  title: string

  /** 任务状态，例如 TODO / DONE / CANCELLED。 */
  status: string

  /** 任务处理人。 */
  assignee?: string

  /** 任务截止时间。 */
  dueAt?: string

  /** 完成人。 */
  completedBy?: string

  /** 完成时间。 */
  completedAt?: string

  /** SOP 扩展参数。 */
  metadata: Record<string, unknown>

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string
}

export interface ConversationWorkItemAudit {
  /** 审计事件 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属工单 ID。 */
  workItemId: number

  /** 事件类型，例如 ASSIGNED / STATUS_CHANGED。 */
  eventType: string

  /** 操作人。 */
  actor: string

  /** 变更前值。 */
  oldValue: Record<string, unknown>

  /** 变更后值。 */
  newValue: Record<string, unknown>

  /** 操作备注。 */
  note?: string

  /** 事件时间。 */
  createdAt: string
}

export interface ConversationWorkspaceTimeline {
  /** 工单主信息。 */
  workItem: ConversationWorkItem

  /** 客户资料快照。 */
  contactProfile: ConversationContactProfile

  /** 关联会话。 */
  session: ConversationSession

  /** 会话消息流。 */
  messages: ConversationMessage[]

  /** SOP 任务列表。 */
  tasks: ConversationSopTask[]

  /** 工单审计事件。 */
  audits: ConversationWorkItemAudit[]
}

export interface ConversationAiReplySuggestion {
  /** AI 回复建议 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属工单 ID。 */
  workItemId: number

  /** 所属会话 ID。 */
  sessionId: number

  /** 来源客户消息 ID。 */
  sourceMessageId?: number

  /** 生成提示词上下文快照。 */
  promptContext: Record<string, unknown>

  /** AI 建议回复正文。 */
  suggestedReplyText: string

  /** 回复语气。 */
  tone?: string

  /** 回复意图。 */
  intent?: string

  /** 模型置信度。 */
  confidence: number

  /** 风险标记，例如合规或敏感承诺风险。 */
  riskFlags: string[]

  /** 依据片段，用于人工审核溯源。 */
  groundingSnippets: string[]

  /** 供应商配置 ID。 */
  providerId?: number

  /** 模板 ID。 */
  templateId?: number

  /** 模型编码。 */
  modelKey?: string

  /** 供应商调用状态。 */
  providerStatus?: string

  /** 是否使用兜底生成或静态模板。 */
  fallbackUsed: boolean

  /** 审核状态，例如 DRAFT / ACCEPTED / REJECTED。 */
  status: string

  /** 生成操作者。 */
  generatedBy?: string

  /** 审核人。 */
  reviewedBy?: string

  /** 审核时间。 */
  reviewedAt?: string

  /** 审核备注。 */
  reviewNote?: string

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string
}

export interface ConversationInboxParams {
  /** 工单状态筛选。 */
  status?: string

  /** 处理人筛选。 */
  assignedTo?: string

  /** 渠道筛选。 */
  channel?: string

  /** 返回条数上限。 */
  limit?: number
}

export interface ConversationTimelineParams {
  /** 消息数量上限。 */
  messageLimit?: number

  /** 审计事件数量上限。 */
  auditLimit?: number
}

export interface ConversationAssignmentPayload {
  /** 新处理人。 */
  assignedTo?: string

  /** 新处理团队。 */
  assignedTeam?: string

  /** 指派备注。 */
  note?: string
}

export interface ConversationWorkItemStatusPayload {
  /** 新工单状态。 */
  status?: string

  /** 新优先级。 */
  priority?: string

  /** 下次跟进时间。 */
  nextFollowUpAt?: string

  /** 状态变更备注。 */
  note?: string
}

export interface ConversationSopTaskPayload {
  /** SOP 任务编码。 */
  taskKey: string

  /** 任务标题。 */
  title: string

  /** 任务处理人。 */
  assignee?: string

  /** 任务截止时间。 */
  dueAt?: string

  /** SOP 扩展参数。 */
  metadata?: Record<string, unknown>
}

export interface ConversationSopTaskCompletionPayload {
  /** 完成备注。 */
  note?: string
}

export interface ConversationAiReplyGeneratePayload {
  /** AI 供应商配置 ID。 */
  providerId?: number

  /** 回复模板 ID。 */
  templateId?: number

  /** 模型编码。 */
  modelKey?: string

  /** 期望回复语气。 */
  tone?: string

  /** 期望回复意图。 */
  intent?: string

  /** 生成参数，例如温度或上下文开关。 */
  params?: Record<string, unknown>

  /** 生成超时时间。 */
  timeoutMs?: number

  /** 坐席附加指令。 */
  operatorInstruction?: string
}

export interface ConversationAiReplyReviewPayload {
  /** 审核决策，例如 ACCEPTED / REJECTED。 */
  decision: string

  /** 审核备注。 */
  note?: string
}

export interface ConversationAiReplySuggestionParams {
  /** 审核状态筛选。 */
  status?: string

  /** 返回条数上限。 */
  limit?: number
}

export const WORK_ITEM_STATUS_OPTIONS = ['OPEN', 'PENDING', 'SNOOZED', 'RESOLVED']
export const WORK_ITEM_PRIORITY_OPTIONS = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
export const WORK_ITEM_CHANNEL_OPTIONS = [
  'WEB_CHAT',
  'WHATSAPP',
  'SOCIAL_DM',
  'RCS',
  'SANDBOX',
  'WECHAT',
  'SMS',
  'EMAIL',
  'PUSH',
]

/** 将工单状态转换为中文标签文案和颜色。 */
export function formatWorkItemStatus(status?: string | null) {
  const upper = (status ?? '').toUpperCase()
  const views: Record<string, { text: string; color: string }> = {
    OPEN: { text: '待处理', color: 'green' },
    PENDING: { text: '等待中', color: 'gold' },
    SNOOZED: { text: '已稍后跟进', color: 'blue' },
    RESOLVED: { text: '已解决', color: 'default' },
  }
  return views[upper] ?? { text: status || '-', color: 'default' }
}

/** 将 AI 回复建议审核状态转换为中文标签文案和颜色。 */
export function formatAiReplySuggestionStatus(status?: string | null) {
  const upper = (status ?? '').toUpperCase()
  const views: Record<string, { text: string; color: string }> = {
    DRAFT: { text: '待审核', color: 'blue' },
    ACCEPTED: { text: '已采纳', color: 'green' },
    REJECTED: { text: '已拒绝', color: 'red' },
  }
  return views[upper] ?? { text: status || '-', color: 'default' }
}

/** 根据工单优先级选择标签颜色，突出紧急和高优先级会话。 */
export function priorityColor(priority?: string | null) {
  switch ((priority ?? '').toUpperCase()) {
    case 'URGENT':
      return 'red'
    case 'HIGH':
      return 'orange'
    case 'NORMAL':
      return 'blue'
    default:
      return 'default'
  }
}

/** 根据 SOP 任务状态选择标签颜色。 */
export function taskStatusColor(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'DONE':
      return 'green'
    case 'TODO':
      return 'blue'
    case 'CANCELLED':
      return 'default'
    default:
      return 'default'
  }
}

/** 归一化收件箱筛选条件：状态和渠道大写，limit 限制在后端可接受范围。 */
export function normalizeInboxFilters(filters: ConversationInboxParams = {}): ConversationInboxParams {
  const normalized: ConversationInboxParams = {
    limit: Math.max(1, Math.min(filters.limit ?? 50, 100)),
  }
  // 空输入不写入查询参数，避免后端把空字符串当作精确筛选。
  copyUpper(normalized, 'status', filters.status)
  copyText(normalized, 'assignedTo', filters.assignedTo)
  copyUpper(normalized, 'channel', filters.channel)
  return normalized
}

/** 归一化 AI 回复建议查询参数，确保状态筛选和分页上限稳定。 */
export function normalizeAiReplySuggestionParams(
  params: ConversationAiReplySuggestionParams = {},
): ConversationAiReplySuggestionParams {
  const normalized: ConversationAiReplySuggestionParams = {
    limit: Math.max(1, Math.min(params.limit ?? 20, 100)),
  }
  const status = params.status?.trim()
  if (status) normalized.status = status.toUpperCase()
  return normalized
}

/** 生成工单标题，按主题、用户 ID、会话 ID 逐级兜底。 */
export function workItemTitle(row?: Partial<ConversationWorkItem> | null) {
  if (!row) return '会话工单'
  const title = row.subject?.trim() || row.userId || `会话 ${row.sessionId}`
  return `#${row.id} · ${title}`
}

/** 拼接审计事件摘要，压缩展示事件类型、操作人、时间和备注。 */
export function timelineAuditLine(audit: Partial<ConversationWorkItemAudit>) {
  const parts = [
    audit.eventType || '-',
    audit.actor || '-',
    formatDateTime(audit.createdAt),
    audit.note,
  ].filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
  return parts.map(String).join(' · ')
}

/** 生成客户资料标题，优先使用客户展示名。 */
export function contactProfileTitle(profile?: Pick<ConversationContactProfile, 'displayName' | 'userId'> | null) {
  if (!profile) return '客户资料'
  return profile.displayName?.trim() || profile.userId || '客户资料'
}

function copyText(target: ConversationInboxParams, key: keyof ConversationInboxParams, value?: string) {
  const text = value?.trim()
  if (text) {
    target[key] = text as never
  }
}

function copyUpper(target: ConversationInboxParams, key: keyof ConversationInboxParams, value?: string) {
  const text = value?.trim()
  if (text) {
    target[key] = text.toUpperCase() as never
  }
}
