/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ConversationsPage from './index'
import {
  assignConversationWorkItem,
  completeConversationSopTask,
  createConversationSopTask,
  generateConversationAiReplySuggestion,
  getConversationWorkspaceTimeline,
  listConversationInbox,
  listConversationAiReplySuggestions,
  reviewConversationAiReplySuggestion,
  updateConversationWorkItemStatus,
} from '../../services/conversationApi'

vi.mock('../../services/conversationApi', () => ({
  assignConversationWorkItem: vi.fn(),
  completeConversationSopTask: vi.fn(),
  createConversationSopTask: vi.fn(),
  generateConversationAiReplySuggestion: vi.fn(),
  getConversationWorkspaceTimeline: vi.fn(),
  listConversationInbox: vi.fn(),
  listConversationAiReplySuggestions: vi.fn(),
  reviewConversationAiReplySuggestion: vi.fn(),
  updateConversationWorkItemStatus: vi.fn(),
}))

const workItem = {
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
  assignedTo: 'alice',
  assignedTeam: 'sales',
  source: 'INBOUND',
  nextFollowUpAt: '2026-06-07T09:30:00',
  lastCustomerMessageAt: '2026-06-06T09:10:00',
  tags: ['vip'],
  attributes: {},
  createdAt: '2026-06-06T09:00:00',
  updatedAt: '2026-06-06T09:10:00',
}

const timeline = {
  workItem,
  contactProfile: {
    id: 301,
    tenantId: 7,
    userId: 'user-1',
    displayName: 'Alice Zhang',
    privateDomainSource: 'WEB_CHAT',
    owner: 'alice',
    lifecycleStage: 'LEAD',
    tags: ['vip'],
    attributes: { city: 'Shanghai' },
    createdAt: '2026-06-06T09:00:00',
    updatedAt: '2026-06-06T09:10:00',
  },
  session: {
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
  },
  messages: [{
    id: 801,
    tenantId: 7,
    sessionId: 100,
    direction: 'INBOUND',
    messageType: 'TEXT',
    text: 'I want pricing',
    intent: 'PRICING',
    content: {},
    createdAt: '2026-06-06T09:10:00',
  }],
  tasks: [{
    id: 900,
    tenantId: 7,
    workItemId: 501,
    taskKey: 'book_demo',
    title: 'Book a product demo',
    status: 'TODO',
    assignee: 'alice',
    dueAt: '2026-06-07T10:00:00',
    metadata: {},
    createdAt: '2026-06-06T09:12:00',
    updatedAt: '2026-06-06T09:12:00',
  }],
  audits: [{
    id: 1,
    tenantId: 7,
    workItemId: 501,
    eventType: 'ASSIGNED',
    actor: 'alice',
    oldValue: {},
    newValue: { assignedTo: 'alice' },
    note: 'VIP',
    createdAt: '2026-06-06T09:12:00',
  }],
}

const aiSuggestion = {
  id: 7001,
  tenantId: 7,
  workItemId: 501,
  sessionId: 100,
  sourceMessageId: 801,
  promptContext: { channel: 'WEB_CHAT' },
  suggestedReplyText: '您好，价格方案可以按团队规模配置，我可以先帮您预约一次产品演示。',
  tone: 'HELPFUL',
  intent: 'PRICING',
  confidence: 0.82,
  riskFlags: ['SENSITIVE_PAYMENT'],
  groundingSnippets: ['I want pricing'],
  providerId: 11,
  templateId: 22,
  modelKey: 'gpt-4.1-mini',
  providerStatus: 'OK',
  fallbackUsed: false,
  status: 'DRAFT',
  generatedBy: 'alice',
  createdAt: '2026-06-06T09:20:00',
  updatedAt: '2026-06-06T09:20:00',
}

describe('ConversationsPage', () => {
  beforeEach(() => {
    vi.mocked(listConversationInbox).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [workItem],
    })
    vi.mocked(getConversationWorkspaceTimeline).mockResolvedValue({
      code: 0,
      message: 'success',
      data: timeline,
    })
    vi.mocked(assignConversationWorkItem).mockResolvedValue({ code: 0, message: 'success', data: workItem })
    vi.mocked(updateConversationWorkItemStatus).mockResolvedValue({ code: 0, message: 'success', data: workItem })
    vi.mocked(createConversationSopTask).mockResolvedValue({ code: 0, message: 'success', data: timeline.tasks[0] })
    vi.mocked(completeConversationSopTask).mockResolvedValue({ code: 0, message: 'success', data: timeline.tasks[0] })
    vi.mocked(listConversationAiReplySuggestions).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [aiSuggestion],
    })
    vi.mocked(generateConversationAiReplySuggestion).mockResolvedValue({
      code: 0,
      message: 'success',
      data: aiSuggestion,
    })
    vi.mocked(reviewConversationAiReplySuggestion).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { ...aiSuggestion, status: 'ACCEPTED', reviewedBy: 'alice' },
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads operator inbox and opens customer timeline with action controls', async () => {
    render(<ConversationsPage />)

    await waitFor(() => expect(listConversationInbox).toHaveBeenCalledWith({ status: 'OPEN', limit: 50 }))
    expect(screen.getByRole('heading', { name: '会话工作台' })).toBeInTheDocument()
    expect(await screen.findByText(/Pricing question/)).toBeInTheDocument()
    expect(screen.getByText(/alice/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '详情' }))

    await waitFor(() => expect(getConversationWorkspaceTimeline).toHaveBeenCalledWith(501, {
      messageLimit: 50,
      auditLimit: 50,
    }))
    expect(screen.getByText('客户时间线')).toBeInTheDocument()
    expect(await screen.findByText(/Alice Zhang/)).toBeInTheDocument()
    expect(await screen.findByText(/I want pricing/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /指派/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /更新状态/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /新增任务/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /完成/ })).toBeInTheDocument()
  })

  it('loads, generates, and reviews AI reply suggestions from the timeline drawer', async () => {
    render(<ConversationsPage />)

    fireEvent.click(await screen.findByRole('button', { name: '详情' }))

    await waitFor(() => expect(listConversationAiReplySuggestions).toHaveBeenCalledWith(501, {
      status: 'DRAFT',
      limit: 20,
    }))
    expect(await screen.findByText(/价格方案可以按团队规模配置/)).toBeInTheDocument()
    expect(screen.getByText('SENSITIVE_PAYMENT')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /生成建议/ }))

    await waitFor(() => expect(generateConversationAiReplySuggestion).toHaveBeenCalledWith(501, {
      tone: 'HELPFUL',
      intent: 'PRICING',
      modelKey: 'gpt-4.1-mini',
      params: { source: 'operator_workspace' },
    }))

    fireEvent.click(screen.getByRole('button', { name: /采纳建议/ }))

    await waitFor(() => expect(reviewConversationAiReplySuggestion).toHaveBeenCalledWith(501, 7001, {
      decision: 'ACCEPTED',
      note: 'operator accepted suggestion',
    }))
  })

  it('filters AI reply suggestion history and rejects draft suggestions', async () => {
    render(<ConversationsPage />)

    fireEvent.click(await screen.findByRole('button', { name: '详情' }))
    await screen.findByText(/价格方案可以按团队规模配置/)

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '建议状态' }))
    fireEvent.click(await screen.findByText('已采纳'))

    await waitFor(() => expect(listConversationAiReplySuggestions).toHaveBeenCalledWith(501, {
      status: 'ACCEPTED',
      limit: 20,
    }))

    fireEvent.click(screen.getByRole('button', { name: /拒绝建议/ }))

    await waitFor(() => expect(reviewConversationAiReplySuggestion).toHaveBeenCalledWith(501, 7001, {
      decision: 'REJECTED',
      note: 'operator rejected suggestion',
    }))
  })
})
