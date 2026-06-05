import type { PageResult, R } from '../types'
import http from './api'

export interface ExecutionRequestRow {
  id: string
  canvasId: number
  userId: string
  status: string
  attemptCount?: number
  lastError?: string
  updatedAt?: string
}

export interface MessageSendRecordRow {
  id: number
  executionId: string
  canvasId: number
  userId: string
  channel: string
  status: string
  requestPayload?: string
  externalMessageId?: string
  errorMessage?: string
  createdAt?: string
}

export const operatorApi = {
  executionRequests: (params: Record<string, unknown>) =>
    http.get<R<PageResult<ExecutionRequestRow>>, R<PageResult<ExecutionRequestRow>>>(
      '/canvas/execution-requests',
      { params },
    ),
  replayExecutionRequest: (id: string, reason?: string, force = false) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(
      `/canvas/execution-requests/${id}/replay`,
      null,
      { params: { reason, force } },
    ),
  messageSendRecords: (params: Record<string, unknown>) =>
    http.get<R<PageResult<MessageSendRecordRow>>, R<PageResult<MessageSendRecordRow>>>(
      '/canvas/message-send-records',
      { params },
    ),
  messageSendRecord: (id: number) =>
    http.get<R<MessageSendRecordRow>, R<MessageSendRecordRow>>(`/canvas/message-send-records/${id}`),
  policyState: (userId: string, channel: string) =>
    http.get<R<Record<string, unknown>>, R<Record<string, unknown>>>(
      '/canvas/policies/state',
      { params: { userId, channel } },
    ),
}
