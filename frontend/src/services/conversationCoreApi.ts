import type { R } from '../types'
import http from './api'

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
  lastMessageAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface ConversationMessage {
  id: number
  tenantId: number
  sessionId: number
  direction: string
  messageType: string
  externalMessageId?: string
  textContent?: string
  intent?: string
  content: Record<string, unknown>
  processed: boolean
  createdAt?: string
}

export interface ConversationSessionParams {
  userId?: string
  channel?: string
  limit?: number
}

export interface ConversationMessageParams {
  limit?: number
}

export const ingestConversationReply = (payload: ConversationIngressPayload) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    '/canvas/conversations/ingress',
    payload,
  )

export const ingestConversationAdapterReply = (adapterKey: string, payload: Record<string, unknown>) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    `/canvas/conversations/adapters/${encodeURIComponent(adapterKey)}/ingress`,
    payload,
  )

export const listConversationSessions = (params?: ConversationSessionParams) =>
  http.get<R<ConversationSession[]>, R<ConversationSession[]>>('/canvas/conversations', { params })

export const listConversationMessages = (sessionId: number, params?: ConversationMessageParams) =>
  http.get<R<ConversationMessage[]>, R<ConversationMessage[]>>(
    `/canvas/conversations/${sessionId}/messages`,
    { params },
  )
