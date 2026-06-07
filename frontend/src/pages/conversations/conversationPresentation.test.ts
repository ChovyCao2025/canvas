import { describe, expect, it } from 'vitest'

import {
  conversationMessageLine,
  formatAiReplySuggestionStatus,
  formatConversationDuplicate,
  formatConversationStatus,
  formatDateTime,
  normalizeAiReplySuggestionParams,
  formatWorkItemStatus,
  normalizeInboxFilters,
  priorityColor,
  taskStatusColor,
  timelineAuditLine,
  WORK_ITEM_CHANNEL_OPTIONS,
  workItemTitle,
} from './conversationPresentation'

describe('conversationPresentation', () => {
  it('formats conversation statuses for operator lists', () => {
    expect(formatConversationStatus('ACTIVE')).toEqual({ text: '进行中', color: 'green' })
    expect(formatConversationStatus('TRANSFERRED')).toEqual({ text: '已转接', color: 'gold' })
    expect(formatConversationStatus('UNKNOWN')).toEqual({ text: 'UNKNOWN', color: 'default' })
  })

  it('formats duplicate and resume outcomes', () => {
    expect(formatConversationDuplicate({
      sessionId: 100,
      messageId: 200,
      status: 'RECORDED',
      duplicate: false,
      resumedWaitCount: 2,
    })).toBe('已记录并恢复 2 个等待')
    expect(formatConversationDuplicate({
      sessionId: 100,
      messageId: 200,
      status: 'RECORDED',
      duplicate: true,
      resumedWaitCount: 0,
    })).toBe('重复消息，未重复恢复等待')
  })

  it('builds stable message summary lines', () => {
    expect(conversationMessageLine({
      id: 200,
      tenantId: 7,
      sessionId: 100,
      direction: 'INBOUND',
      messageType: 'TEXT',
      externalMessageId: 'msg-1',
      text: 'yes please',
      intent: 'PRODUCT_A',
      content: { text: 'yes please' },
      createdAt: '2026-06-05T10:00:00',
    })).toBe('INBOUND · TEXT · PRODUCT_A · yes please')
    expect(conversationMessageLine({
      id: 201,
      tenantId: 7,
      sessionId: 100,
      direction: 'OUTBOUND',
      messageType: 'TEXT',
      content: {},
      createdAt: '2026-06-05T10:01:00',
    })).toBe('OUTBOUND · TEXT · -')
  })

  it('formats workspace status, priority, and task tags', () => {
    expect(formatWorkItemStatus('OPEN')).toEqual({ text: '待处理', color: 'green' })
    expect(formatWorkItemStatus('SNOOZED')).toEqual({ text: '已稍后跟进', color: 'blue' })
    expect(formatWorkItemStatus('UNKNOWN')).toEqual({ text: 'UNKNOWN', color: 'default' })
    expect(priorityColor('URGENT')).toBe('red')
    expect(priorityColor('HIGH')).toBe('orange')
    expect(priorityColor('LOW')).toBe('default')
    expect(taskStatusColor('TODO')).toBe('blue')
    expect(taskStatusColor('DONE')).toBe('green')
  })

  it('normalizes inbox filters for bounded workspace queries', () => {
    expect(normalizeInboxFilters({
      status: ' open ',
      assignedTo: ' alice ',
      channel: ' web_chat ',
      limit: 200,
    })).toEqual({
      status: 'OPEN',
      assignedTo: 'alice',
      channel: 'WEB_CHAT',
      limit: 100,
    })

    expect(normalizeInboxFilters({ limit: 0 })).toEqual({ limit: 1 })
  })

  it('keeps adapter-backed channels available for inbox filtering', () => {
    expect(WORK_ITEM_CHANNEL_OPTIONS).toEqual(expect.arrayContaining([
      'WEB_CHAT',
      'WHATSAPP',
      'SOCIAL_DM',
      'RCS',
      'SANDBOX',
    ]))
  })

  it('builds stable workspace titles and timeline lines', () => {
    expect(formatDateTime('2026-06-06T09:30:12')).toBe('2026-06-06 09:30:12')
    expect(workItemTitle({
      id: 501,
      tenantId: 7,
      sessionId: 100,
      contactProfileId: 301,
      userId: 'user-1',
      channel: 'WEB_CHAT',
      provider: 'WEB',
      subject: 'Pricing question',
      status: 'OPEN',
      priority: 'HIGH',
      source: 'INBOUND',
      tags: [],
      attributes: {},
      createdAt: '2026-06-06T09:00:00',
      updatedAt: '2026-06-06T09:00:00',
    })).toBe('#501 · Pricing question')
    expect(timelineAuditLine({
      id: 1,
      tenantId: 7,
      workItemId: 501,
      eventType: 'ASSIGNED',
      actor: 'alice',
      oldValue: {},
      newValue: { assignedTo: 'bob' },
      note: 'VIP',
      createdAt: '2026-06-06T09:30:12',
    })).toBe('ASSIGNED · alice · 2026-06-06 09:30:12 · VIP')
  })

  it('formats and normalizes AI reply suggestion controls', () => {
    expect(formatAiReplySuggestionStatus('DRAFT')).toEqual({ text: '待审核', color: 'blue' })
    expect(formatAiReplySuggestionStatus('ACCEPTED')).toEqual({ text: '已采纳', color: 'green' })
    expect(formatAiReplySuggestionStatus('REJECTED')).toEqual({ text: '已拒绝', color: 'red' })
    expect(formatAiReplySuggestionStatus('UNKNOWN')).toEqual({ text: 'UNKNOWN', color: 'default' })

    expect(normalizeAiReplySuggestionParams({ status: ' draft ', limit: 200 })).toEqual({
      status: 'DRAFT',
      limit: 100,
    })
    expect(normalizeAiReplySuggestionParams({ limit: 0 })).toEqual({ limit: 1 })
  })
})
