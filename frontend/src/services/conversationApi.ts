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

/** 会话列表筛选参数。 */
export interface ConversationSessionParams {
  /** CDP/业务用户 ID，用于定位单个客户的会话。 */
  userId?: string

  /** 会话渠道，例如 WEB_CHAT / WECHAT / EMAIL。 */
  channel?: string

  /** 返回条数上限。 */
  limit?: number
}

/** 会话消息查询参数。 */
export interface ConversationMessageParams {
  /** 返回最近消息条数上限。 */
  limit?: number
}

/** 接收客户回复事件，驱动画布等待节点恢复和会话工作台更新。 */
export const ingestConversationReply = (payload: ConversationIngressPayload) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    '/canvas/conversations/ingress',
    payload,
  )

/** 接收渠道适配器原始回调，adapterKey 统一编码后交由后端适配成标准会话事件。 */
export const ingestConversationAdapterReply = (adapterKey: string, payload: Record<string, unknown>) =>
  http.post<R<ConversationIngressResponse>, R<ConversationIngressResponse>>(
    `/canvas/conversations/adapters/${encodeURIComponent(adapterKey)}/ingress`,
    payload,
  )

/** 查询会话列表，支持按用户和渠道过滤。 */
export const listConversationSessions = (params?: ConversationSessionParams) =>
  http.get<R<ConversationSession[]>, R<ConversationSession[]>>('/canvas/conversations', { params })

/** 查询单个会话消息流，用于会话详情和调试客户回复链路。 */
export const listConversationMessages = (sessionId: number, params?: ConversationMessageParams) =>
  http.get<R<ConversationMessage[]>, R<ConversationMessage[]>>(
    `/canvas/conversations/${sessionId}/messages`,
    { params },
  )

/** 确保会话存在工作台工单，常用于从实时会话升级到人工跟进。 */
export const ensureConversationWorkItem = (sessionId: number) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/sessions/${sessionId}/work-item`,
  )

/** 查询会话工作台收件箱，按状态、处理人和渠道聚合待办。 */
export const listConversationInbox = (params?: ConversationInboxParams) =>
  http.get<R<ConversationWorkItem[]>, R<ConversationWorkItem[]>>(
    '/canvas/conversations/workspace/inbox',
    { params },
  )

/** 指派会话工单，记录处理人、团队和操作备注。 */
export const assignConversationWorkItem = (workItemId: number, payload: ConversationAssignmentPayload) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/assign`,
    payload,
  )

/** 更新会话工单状态、优先级或下次跟进时间。 */
export const updateConversationWorkItemStatus = (workItemId: number, payload: ConversationWorkItemStatusPayload) =>
  http.post<R<ConversationWorkItem>, R<ConversationWorkItem>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/status`,
    payload,
  )

/** 在工单下创建 SOP 任务，拆分人工跟进动作。 */
export const createConversationSopTask = (workItemId: number, payload: ConversationSopTaskPayload) =>
  http.post<R<ConversationSopTask>, R<ConversationSopTask>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/tasks`,
    payload,
  )

/** 完成 SOP 任务，并把完成备注写入工单时间线。 */
export const completeConversationSopTask = (taskId: number, payload: ConversationSopTaskCompletionPayload) =>
  http.post<R<ConversationSopTask>, R<ConversationSopTask>>(
    `/canvas/conversations/workspace/tasks/${taskId}/complete`,
    payload,
  )

/** 查询工单时间线，聚合客户资料、会话消息、SOP 任务和审计事件。 */
export const getConversationWorkspaceTimeline = (workItemId: number, params?: ConversationTimelineParams) =>
  http.get<R<ConversationWorkspaceTimeline>, R<ConversationWorkspaceTimeline>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/timeline`,
    { params },
  )

/** 生成 AI 回复建议，失败时后端可通过 fallbackUsed 标记兜底来源。 */
export const generateConversationAiReplySuggestion = (
  workItemId: number,
  payload: ConversationAiReplyGeneratePayload,
) =>
  http.post<R<ConversationAiReplySuggestion>, R<ConversationAiReplySuggestion>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions/generate`,
    payload,
  )

/** 审核 AI 回复建议，将 DRAFT 转为 ACCEPTED 或 REJECTED。 */
export const reviewConversationAiReplySuggestion = (
  workItemId: number,
  suggestionId: number,
  payload: ConversationAiReplyReviewPayload,
) =>
  http.post<R<ConversationAiReplySuggestion>, R<ConversationAiReplySuggestion>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions/${suggestionId}/review`,
    payload,
  )

/** 查询工单下 AI 回复建议，支持按审核状态筛选。 */
export const listConversationAiReplySuggestions = (
  workItemId: number,
  params?: ConversationAiReplySuggestionParams,
) =>
  http.get<R<ConversationAiReplySuggestion[]>, R<ConversationAiReplySuggestion[]>>(
    `/canvas/conversations/workspace/work-items/${workItemId}/ai-reply-suggestions`,
    { params },
  )
