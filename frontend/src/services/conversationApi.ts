import type { R } from '../types'
import type {
  ConversationAiReplyGeneratePayload,
  ConversationAiReplyReviewPayload,
  ConversationAiReplySuggestion,
  ConversationAiReplySuggestionParams,
  ConversationAssignmentPayload,
  ConversationInboxParams,
  ConversationIngressPayload,
  ConversationIngressResponse,
  ConversationMessage,
  ConversationSopTask,
  ConversationSopTaskCompletionPayload,
  ConversationSopTaskPayload,
  ConversationSession,
  ConversationTimelineParams,
  ConversationWorkItem,
  ConversationWorkItemStatusPayload,
  ConversationWorkspaceTimeline,
} from '../pages/conversations/conversationPresentation'
import http from './api'

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

export const ensureConversationWorkItem = (sessionId: number) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/sessions/${sessionId}/work-item`,
  )

export const listConversationInbox = (params?: ConversationInboxParams) =>
  http.get<R<ConversationWorkItem[]>, R<ConversationWorkItem[]>>(
    '/canvas/conversations/workspace/inbox',
    { params },
  )

export const assignConversationWorkItem = (workItemId: number, payload: ConversationAssignmentPayload) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/assign`,
    payload,
  )

export const updateConversationWorkItemStatus = (workItemId: number, payload: ConversationWorkItemStatusPayload) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/status`,
    payload,
  )

export const createConversationSopTask = (workItemId: number, payload: ConversationSopTaskPayload) =>
  http.post<R<ConversationSopTask>, R<ConversationSopTask>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/tasks`,
    payload,
  )

export const completeConversationSopTask = (taskId: number, payload: ConversationSopTaskCompletionPayload) =>
  http.post<R<ConversationSopTask>, R<ConversationSopTask>>(
    `/canvas/conversations/workspace/tasks/${taskId}/complete`,
    payload,
  )

export const getConversationWorkspaceTimeline = (workItemId: number, params?: ConversationTimelineParams) =>
  http.get<R<ConversationWorkspaceTimeline>, R<ConversationWorkspaceTimeline>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/timeline`,
    { params },
  )

export const generateConversationAiReplySuggestion = (
  workItemId: number,
  payload: ConversationAiReplyGeneratePayload,
) =>
  http.post<R<ConversationAiReplySuggestion>, R<ConversationAiReplySuggestion>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions/generate`,
    payload,
  )

export const reviewConversationAiReplySuggestion = (
  workItemId: number,
  suggestionId: number,
  payload: ConversationAiReplyReviewPayload,
) =>
  http.post<R<ConversationAiReplySuggestion>, R<ConversationAiReplySuggestion>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions/${suggestionId}/review`,
    payload,
  )

export const listConversationAiReplySuggestions = (
  workItemId: number,
  params?: ConversationAiReplySuggestionParams,
) =>
  http.get<R<ConversationAiReplySuggestion[]>, R<ConversationAiReplySuggestion[]>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions`,
    { params },
  )
