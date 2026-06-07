import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import {
  assignConversationWorkItem,
  completeConversationSopTask,
  createConversationSopTask,
  generateConversationAiReplySuggestion,
  ensureConversationWorkItem,
  getConversationWorkspaceTimeline,
  ingestConversationAdapterReply,
  ingestConversationReply,
  listConversationAiReplySuggestions,
  listConversationMessages,
  listConversationInbox,
  listConversationSessions,
  reviewConversationAiReplySuggestion,
  updateConversationWorkItemStatus,
} from './conversationApi'

describe('conversationApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls conversation ingress, sessions, and messages endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })
    const payload = {
      canvasId: 10,
      versionId: 20,
      executionId: 'exec-1',
      userId: 'user-1',
      channel: 'WHATSAPP',
      provider: 'TWILIO',
      externalMessageId: 'msg-1',
      eventId: 'evt-1',
      messageType: 'TEXT',
      text: 'yes please',
      intent: 'PRODUCT_A',
      attributes: { locale: 'en-US' },
      occurredAt: '2026-06-05T10:00:00',
    }

    await ingestConversationReply(payload)
    await listConversationSessions({ userId: 'user-1', channel: 'WHATSAPP', limit: 25 })
    await listConversationMessages(100, { limit: 10 })

    expect(post).toHaveBeenCalledWith('/canvas/conversations/ingress', payload)
    expect(get).toHaveBeenCalledWith('/canvas/conversations', {
      params: { userId: 'user-1', channel: 'WHATSAPP', limit: 25 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/conversations/100/messages', {
      params: { limit: 10 },
    })
  })

  it('calls internal adapter ingress endpoint for typed provider payloads', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({
      code: 0,
      message: 'success',
      data: { sessionId: 100, messageId: 200, status: 'RECORDED', duplicate: false, resumedWaitCount: 1 },
    })
    const payload = {
      canvasId: 10,
      versionId: 20,
      executionId: 'exec-1',
      userId: 'rcs:+15551234567',
      provider: 'google_rcs',
      externalMessageId: 'rcs-msg-1',
      eventId: 'rcs-event-1',
      text: 'hello',
      intent: 'GREETING',
      attributes: { locale: 'en-US' },
    }

    await ingestConversationAdapterReply('rcs', payload)

    expect(post).toHaveBeenCalledWith('/canvas/conversations/adapters/rcs/ingress', payload)
  })

  it('calls conversation workspace endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })

    await ensureConversationWorkItem(100)
    await listConversationInbox({ status: 'OPEN', assignedTo: 'alice', channel: 'WEB_CHAT', limit: 25 })
    await assignConversationWorkItem(501, {
      assignedTo: 'alice',
      assignedTeam: 'sales',
      note: 'VIP pricing request',
    })
    await updateConversationWorkItemStatus(501, {
      status: 'SNOOZED',
      priority: 'HIGH',
      nextFollowUpAt: '2026-06-07T09:30:00',
      note: 'Follow up after demo slot confirmation',
    })
    await createConversationSopTask(501, {
      taskKey: 'book_demo',
      title: 'Book a product demo',
      assignee: 'alice',
      dueAt: '2026-06-07T10:00:00',
      metadata: { playbook: 'sales_handoff' },
    })
    await completeConversationSopTask(900, { note: 'Demo booked' })
    await getConversationWorkspaceTimeline(501, { messageLimit: 20, auditLimit: 10 })

    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/sessions/100/work-item')
    expect(get).toHaveBeenCalledWith('/canvas/conversations/workspace/inbox', {
      params: { status: 'OPEN', assignedTo: 'alice', channel: 'WEB_CHAT', limit: 25 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/assign', {
      assignedTo: 'alice',
      assignedTeam: 'sales',
      note: 'VIP pricing request',
    })
    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/status', {
      status: 'SNOOZED',
      priority: 'HIGH',
      nextFollowUpAt: '2026-06-07T09:30:00',
      note: 'Follow up after demo slot confirmation',
    })
    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/tasks', {
      taskKey: 'book_demo',
      title: 'Book a product demo',
      assignee: 'alice',
      dueAt: '2026-06-07T10:00:00',
      metadata: { playbook: 'sales_handoff' },
    })
    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/tasks/900/complete', {
      note: 'Demo booked',
    })
    expect(get).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/timeline', {
      params: { messageLimit: 20, auditLimit: 10 },
    })
  })

  it('calls AI reply suggestion workspace endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })

    await generateConversationAiReplySuggestion(501, {
      modelKey: 'gpt-4.1-mini',
      tone: 'HELPFUL',
      intent: 'PRICING',
      operatorInstruction: 'Keep it concise',
      params: { locale: 'zh-CN' },
      timeoutMs: 8000,
    })
    await listConversationAiReplySuggestions(501, { status: 'DRAFT', limit: 10 })
    await reviewConversationAiReplySuggestion(501, 7001, {
      decision: 'ACCEPTED',
      note: 'Ready to use',
    })

    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/ai-reply-suggestions/generate', {
      modelKey: 'gpt-4.1-mini',
      tone: 'HELPFUL',
      intent: 'PRICING',
      operatorInstruction: 'Keep it concise',
      params: { locale: 'zh-CN' },
      timeoutMs: 8000,
    })
    expect(get).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/ai-reply-suggestions', {
      params: { status: 'DRAFT', limit: 10 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/conversations/workspace/work-items/501/ai-reply-suggestions/7001/review', {
      decision: 'ACCEPTED',
      note: 'Ready to use',
    })
  })
})
