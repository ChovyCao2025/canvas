import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import {
  ingestConversationAdapterReply,
  ingestConversationReply,
  listConversationMessages,
  listConversationSessions,
} from './conversationCoreApi'

describe('conversationCoreApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls P2-080 conversation ingress, session, and message endpoints only', async () => {
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
    await ingestConversationAdapterReply('whatsapp cloud', { userId: 'user-1' })
    await listConversationSessions({ userId: 'user-1', channel: 'WHATSAPP', limit: 25 })
    await listConversationMessages(100, { limit: 10 })

    expect(post).toHaveBeenCalledWith('/canvas/conversations/ingress', payload)
    expect(post).toHaveBeenCalledWith('/canvas/conversations/adapters/whatsapp%20cloud/ingress', {
      userId: 'user-1',
    })
    expect(get).toHaveBeenCalledWith('/canvas/conversations', {
      params: { userId: 'user-1', channel: 'WHATSAPP', limit: 25 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/conversations/100/messages', {
      params: { limit: 10 },
    })
  })
})
