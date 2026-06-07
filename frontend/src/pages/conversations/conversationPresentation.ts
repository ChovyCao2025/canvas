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
  id: number
  tenantId: number
  sessionId: number
  contactProfileId: number
  userId: string
  channel: string
  provider: string
  subject?: string
  status: string
  priority: string
  assignedTo?: string
  assignedTeam?: string
  source: string
  slaDueAt?: string
  nextFollowUpAt?: string
  lastCustomerMessageAt?: string
  lastOperatorActivityAt?: string
  tags: string[]
  attributes: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface ConversationContactProfile {
  id: number
  tenantId: number
  userId: string
  displayName?: string
  externalContactId?: string
  privateDomainSource?: string
  owner?: string
  lifecycleStage?: string
  tags: string[]
  attributes: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface ConversationSopTask {
  id: number
  tenantId: number
  workItemId: number
  taskKey: string
  title: string
  status: string
  assignee?: string
  dueAt?: string
  completedBy?: string
  completedAt?: string
  metadata: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface ConversationWorkItemAudit {
  id: number
  tenantId: number
  workItemId: number
  eventType: string
  actor: string
  oldValue: Record<string, unknown>
  newValue: Record<string, unknown>
  note?: string
  createdAt: string
}

export interface ConversationWorkspaceTimeline {
  workItem: ConversationWorkItem
  contactProfile: ConversationContactProfile
  session: ConversationSession
  messages: ConversationMessage[]
  tasks: ConversationSopTask[]
  audits: ConversationWorkItemAudit[]
}

export interface ConversationAiReplySuggestion {
  id: number
  tenantId: number
  workItemId: number
  sessionId: number
  sourceMessageId?: number
  promptContext: Record<string, unknown>
  suggestedReplyText: string
  tone?: string
  intent?: string
  confidence: number
  riskFlags: string[]
  groundingSnippets: string[]
  providerId?: number
  templateId?: number
  modelKey?: string
  providerStatus?: string
  fallbackUsed: boolean
  status: string
  generatedBy?: string
  reviewedBy?: string
  reviewedAt?: string
  reviewNote?: string
  createdAt: string
  updatedAt: string
}

export interface ConversationInboxParams {
  status?: string
  assignedTo?: string
  channel?: string
  limit?: number
}

export interface ConversationTimelineParams {
  messageLimit?: number
  auditLimit?: number
}

export interface ConversationAssignmentPayload {
  assignedTo?: string
  assignedTeam?: string
  note?: string
}

export interface ConversationWorkItemStatusPayload {
  status?: string
  priority?: string
  nextFollowUpAt?: string
  note?: string
}

export interface ConversationSopTaskPayload {
  taskKey: string
  title: string
  assignee?: string
  dueAt?: string
  metadata?: Record<string, unknown>
}

export interface ConversationSopTaskCompletionPayload {
  note?: string
}

export interface ConversationAiReplyGeneratePayload {
  providerId?: number
  templateId?: number
  modelKey?: string
  tone?: string
  intent?: string
  params?: Record<string, unknown>
  timeoutMs?: number
  operatorInstruction?: string
}

export interface ConversationAiReplyReviewPayload {
  decision: string
  note?: string
}

export interface ConversationAiReplySuggestionParams {
  status?: string
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

export function formatAiReplySuggestionStatus(status?: string | null) {
  const upper = (status ?? '').toUpperCase()
  const views: Record<string, { text: string; color: string }> = {
    DRAFT: { text: '待审核', color: 'blue' },
    ACCEPTED: { text: '已采纳', color: 'green' },
    REJECTED: { text: '已拒绝', color: 'red' },
  }
  return views[upper] ?? { text: status || '-', color: 'default' }
}

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

export function normalizeInboxFilters(filters: ConversationInboxParams = {}): ConversationInboxParams {
  const normalized: ConversationInboxParams = {
    limit: Math.max(1, Math.min(filters.limit ?? 50, 100)),
  }
  copyUpper(normalized, 'status', filters.status)
  copyText(normalized, 'assignedTo', filters.assignedTo)
  copyUpper(normalized, 'channel', filters.channel)
  return normalized
}

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

export function workItemTitle(row?: Partial<ConversationWorkItem> | null) {
  if (!row) return '会话工单'
  const title = row.subject?.trim() || row.userId || `会话 ${row.sessionId}`
  return `#${row.id} · ${title}`
}

export function timelineAuditLine(audit: Partial<ConversationWorkItemAudit>) {
  const parts = [
    audit.eventType || '-',
    audit.actor || '-',
    formatDateTime(audit.createdAt),
    audit.note,
  ].filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
  return parts.map(String).join(' · ')
}

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
