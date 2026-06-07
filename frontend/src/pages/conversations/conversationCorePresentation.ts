export interface ConversationIngressPayload {
  canvasId?: number
  versionId?: number
  executionId?: string
  userId: string
  channel: string
  provider?: string
  externalMessageId?: string
  eventId?: string
  messageType?: string
  text?: string
  intent?: string
  attributes?: Record<string, unknown>
  occurredAt?: string
}

export interface ConversationIngressResponse {
  sessionId: number
  messageId: number
  status: string
  duplicate: boolean
  resumedWaitCount: number
}

export interface ConversationSession {
  id: number
  tenantId: number
  canvasId?: number
  versionId?: number
  executionId?: string
  userId: string
  channel: string
  provider: string
  status: string
  turnCount: number
  context: Record<string, unknown>
  lastMessageAt: string
  expiresAt?: string
  createdAt: string
  updatedAt: string
}

export interface ConversationMessage {
  id: number
  tenantId: number
  sessionId: number
  direction: string
  messageType: string
  externalMessageId?: string
  text?: string
  intent?: string
  content: Record<string, unknown>
  createdAt: string
}

export function formatConversationStatus(status: string) {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '进行中', color: 'green' },
    COMPLETED: { text: '已完成', color: 'blue' },
    EXPIRED: { text: '已过期', color: 'red' },
    TRANSFERRED: { text: '已转接', color: 'gold' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatConversationDuplicate(resp: ConversationIngressResponse) {
  if (resp.duplicate) return '重复消息，未重复恢复等待'
  return `已记录并恢复 ${resp.resumedWaitCount} 个等待`
}

export function conversationMessageLine(message: ConversationMessage) {
  const parts = [
    message.direction || '-',
    message.messageType || '-',
    message.intent ?? '-',
    message.text,
  ].filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
  if (parts.length <= 2) parts.push('-')
  return parts.map(String).join(' · ')
}
