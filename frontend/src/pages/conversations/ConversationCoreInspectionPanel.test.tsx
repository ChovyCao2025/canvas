/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ConversationCoreInspectionPanel from './ConversationCoreInspectionPanel'
import {
  listConversationMessages,
  listConversationSessions,
} from '../../services/conversationCoreApi'

vi.mock('../../services/conversationCoreApi', () => ({
  listConversationMessages: vi.fn(),
  listConversationSessions: vi.fn(),
}))

const session = {
  id: 100,
  tenantId: 7,
  userId: 'user-1',
  channel: 'WEB_CHAT',
  provider: 'WEB',
  status: 'ACTIVE',
  turnCount: 2,
  context: {},
  lastMessageAt: '2026-06-06T09:10:00',
  createdAt: '2026-06-06T09:00:00',
  updatedAt: '2026-06-06T09:10:00',
}

const messageRow = {
  id: 801,
  tenantId: 7,
  sessionId: 100,
  direction: 'INBOUND',
  messageType: 'TEXT',
  text: 'I want pricing',
  intent: 'PRICING',
  content: {},
  processed: false,
  createdAt: '2026-06-06T09:10:00',
}

describe('ConversationCoreInspectionPanel', () => {
  beforeEach(() => {
    vi.mocked(listConversationSessions).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [session],
    })
    vi.mocked(listConversationMessages).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [messageRow],
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads P2-080 conversation sessions and opens message inspection', async () => {
    render(<ConversationCoreInspectionPanel />)

    await waitFor(() => expect(listConversationSessions).toHaveBeenCalledWith({ limit: 25 }))
    expect(screen.getByRole('heading', { name: '会话检查' })).toBeInTheDocument()
    expect(await screen.findByText('user-1')).toBeInTheDocument()
    expect(screen.getByText('WEB_CHAT / WEB')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '查看消息' }))

    await waitFor(() => expect(listConversationMessages).toHaveBeenCalledWith(100, { limit: 50 }))
    expect(await screen.findByText('INBOUND · TEXT · PRICING · I want pricing')).toBeInTheDocument()
  })
})
