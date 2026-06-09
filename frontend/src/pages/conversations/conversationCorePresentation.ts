/** 标准会话入站事件展示模型。 */
export interface ConversationIngressPayload {
  /** 关联画布 ID，用于说明回复会恢复哪个画布。 */
  canvasId?: number

  /** 关联画布版本 ID。 */
  versionId?: number

  /** 执行 ID，用于精确恢复等待节点。 */
  executionId?: string

  /** 客户业务 ID。 */
  userId: string

  /** 会话渠道。 */
  channel: string

  /** 渠道供应商。 */
  provider?: string

  /** 渠道侧消息 ID，用于幂等展示。 */
  externalMessageId?: string

  /** 渠道侧事件 ID。 */
  eventId?: string

  /** 消息类型。 */
  messageType?: string

  /** 文本内容。 */
  text?: string

  /** 意图识别结果。 */
  intent?: string

  /** 渠道扩展属性。 */
  attributes?: Record<string, unknown>

  /** 事件发生时间。 */
  occurredAt?: string
}

/** 入站事件处理结果展示模型。 */
export interface ConversationIngressResponse {
  /** 会话 ID。 */
  sessionId: number

  /** 消息 ID。 */
  messageId: number

  /** 会话处理后的状态。 */
  status: string

  /** 是否命中幂等重复。 */
  duplicate: boolean

  /** 已恢复的等待节点数量。 */
  resumedWaitCount: number
}

/** 会话列表展示模型。 */
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

  /** 会话状态。 */
  status: string

  /** 会话轮次。 */
  turnCount: number

  /** 会话上下文。 */
  context: Record<string, unknown>

  /** 最近消息时间。 */
  lastMessageAt: string

  /** 会话过期时间。 */
  expiresAt?: string

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string
}

/** 会话消息展示模型。 */
export interface ConversationMessage {
  /** 消息 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number

  /** 所属会话 ID。 */
  sessionId: number

  /** 消息方向。 */
  direction: string

  /** 消息类型。 */
  messageType: string

  /** 渠道侧消息 ID。 */
  externalMessageId?: string

  /** 文本内容，兼容旧字段。 */
  text?: string

  /** 文本内容，兼容后端新字段。 */
  textContent?: string

  /** 意图识别结果。 */
  intent?: string

  /** 结构化消息内容。 */
  content: Record<string, unknown>

  /** 创建时间。 */
  createdAt?: string
}

/** 将后端会话状态转换成页面标签文案和颜色。 */
export function formatConversationStatus(status: string) {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '进行中', color: 'green' },
    COMPLETED: { text: '已完成', color: 'blue' },
    EXPIRED: { text: '已过期', color: 'red' },
    TRANSFERRED: { text: '已转接', color: 'gold' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

/** 统一把 ISO 时间裁剪成秒级展示，空值用占位符兜底。 */
export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

/** 解释入站回复处理结果，突出重复消息和等待节点恢复数量。 */
export function formatConversationDuplicate(resp: ConversationIngressResponse) {
  if (resp.duplicate) return '重复消息，未重复恢复等待'
  return `已记录并恢复 ${resp.resumedWaitCount} 个等待`
}

/** 拼接单条会话消息摘要，兼容 text 与 textContent 两种后端字段。 */
export function conversationMessageLine(message: ConversationMessage) {
  const parts = [
    message.direction || '-',
    message.messageType || '-',
    message.intent ?? '-',
    message.text ?? message.textContent,
  ].filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
  // 方向和类型之外没有更多业务内容时补占位，避免消息行过短造成误读。
  if (parts.length <= 2) parts.push('-')
  return parts.map(String).join(' · ')
}
