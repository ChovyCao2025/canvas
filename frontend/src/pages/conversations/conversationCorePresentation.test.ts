import { describe, expect, it } from 'vitest'

import {
  conversationMessageLine,
  formatConversationDuplicate,
  formatConversationStatus,
  formatDateTime,
} from './conversationCorePresentation'

describe('conversationCorePresentation', () => {
  it('formats P2-080 conversation status labels', () => {
    expect(formatConversationStatus('ACTIVE')).toEqual({ text: '进行中', color: 'green' })
    expect(formatConversationStatus('TRANSFERRED')).toEqual({ text: '已转接', color: 'gold' })
    expect(formatConversationStatus('UNKNOWN')).toEqual({ text: 'UNKNOWN', color: 'default' })
    expect(formatConversationStatus('')).toEqual({ text: '-', color: 'default' })
  })

  it('formats P2-080 duplicate and WAIT resume outcomes', () => {
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

  it('builds P2-080 message summary lines', () => {
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

  it('formats compact date-time values used by session inspection', () => {
    expect(formatDateTime('2026-06-06T09:30:12')).toBe('2026-06-06 09:30:12')
    expect(formatDateTime()).toBe('-')
  })
})
