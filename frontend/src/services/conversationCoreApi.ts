import type { R } from '../types'
import http from './api'

/** 标准会话入站事件，承接渠道回调或页面调试提交。 */
export interface ConversationIngressPayload {
  /** 关联画布 ID，用于恢复等待中的画布执行。 */
  canvasId?: number

  /** 关联画布版本 ID。 */
  versionId?: number

  /** 关联执行 ID，精确恢复某次等待节点。 */
  executionId?: string

  /** 客户业务 ID。 */
  userId: string

  /** 会话渠道，例如 WEB_CHAT / WECHAT / EMAIL。 */
  channel: string

  /** 渠道供应商或适配器名称。 */
  provider?: string

  /** 外部消息 ID，用于消息幂等。 */
  externalMessageId?: string

  /** 外部事件 ID，用于事件幂等和审计。 */
  eventId?: string

  /** 消息类型，例如 TEXT / IMAGE / EVENT。 */
  messageType?: string

  /** 文本消息正文。 */
  text?: string

  /** 意图识别结果，可驱动画布分支或工单分派。 */
  intent?: string

  /** 渠道原始属性或扩展上下文。 */
  attributes?: Record<string, unknown>

  /** 事件发生时间，缺省由后端以接收时间兜底。 */
  occurredAt?: string
}

/** 会话入站处理结果。 */
export interface ConversationIngressResponse {
  /** 被创建或命中的会话 ID。 */
  sessionId: number

  /** 入站消息记录 ID。 */
  messageId: number

  /** 处理后的会话状态。 */
  status: string

  /** 是否因幂等键命中被识别为重复消息。 */
  duplicate: boolean

  /** 本次回复恢复的等待节点数量，重复消息通常为 0。 */
  resumedWaitCount: number
}

/** 会话主记录，表示某客户在某渠道的一段连续互动。 */
export interface ConversationSession {
  /** 会话 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 关联画布 ID。 */
  canvasId?: number

  /** 关联画布版本 ID。 */
  versionId?: number

  /** 关联执行 ID。 */
  executionId?: string

  /** 客户业务 ID。 */
  userId: string

  /** 会话渠道。 */
  channel: string

  /** 渠道供应商。 */
  provider: string

  /** 会话状态，例如 ACTIVE / COMPLETED / EXPIRED / TRANSFERRED。 */
  status: string

  /** 已发生的客户/系统轮次计数。 */
  turnCount: number

  /** 会话上下文快照，用于画布恢复和人工工作台展示。 */
  context: Record<string, unknown>

  /** 最近一条消息时间。 */
  lastMessageAt?: string

  /** 创建时间。 */
  createdAt?: string

  /** 更新时间。 */
  updatedAt?: string
}

/** 会话消息记录。 */
export interface ConversationMessage {
  /** 消息 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属会话 ID。 */
  sessionId: number

  /** 消息方向，例如 INBOUND / OUTBOUND。 */
  direction: string

  /** 消息类型。 */
  messageType: string

  /** 外部消息 ID，用于幂等和渠道排查。 */
  externalMessageId?: string

  /** 文本内容。 */
  textContent?: string

  /** 意图识别结果。 */
  intent?: string

  /** 原始消息内容或结构化内容。 */
  content: Record<string, unknown>

  /** 是否已被画布等待恢复逻辑处理。 */
  processed: boolean

  /** 创建时间。 */
  createdAt?: string
}

/** 会话列表筛选参数。 */
export interface ConversationSessionParams {
  /** 客户业务 ID。 */
  userId?: string

  /** 会话渠道。 */
  channel?: string

  /** 返回条数上限。 */
  limit?: number
}

/** 会话消息查询参数。 */
export interface ConversationMessageParams {
  /** 返回最近消息条数上限。 */
  limit?: number
}

/** 接收标准客户回复事件，触发会话状态更新和等待节点恢复。 */
export const ingestConversationReply = (payload: ConversationIngressPayload) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    '/canvas/conversations/ingress',
    payload,
  )

/** 接收渠道适配器原始事件，adapterKey 会作为路径参数安全编码。 */
export const ingestConversationAdapterReply = (adapterKey: string, payload: Record<string, unknown>) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    `/canvas/conversations/adapters/${encodeURIComponent(adapterKey)}/ingress`,
    payload,
  )

/** 查询会话列表。 */
export const listConversationSessions = (params?: ConversationSessionParams) =>
  http.get<R<ConversationSession[]>, R<ConversationSession[]>>('/canvas/conversations', { params })

/** 查询指定会话的消息流水。 */
export const listConversationMessages = (sessionId: number, params?: ConversationMessageParams) =>
  http.get<R<ConversationMessage[]>, R<ConversationMessage[]>>(
    `/canvas/conversations/${sessionId}/messages`,
    { params },
  )
