/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import MessageDeliveryPage from './index'
import {
  canReplayDelivery,
  deliveryDetailTitle,
  deliveryStatusColor,
  normalizeDeliveryFilters,
  receiptLine,
} from './messageDeliveryPresentation'

const api = vi.hoisted(() => ({
  list: vi.fn(),
  detail: vi.fn(),
  receipts: vi.fn(),
  replay: vi.fn(),
  reconcile: vi.fn(),
}))

vi.mock('../../services/messageDeliveryApi', () => ({
  messageDeliveryApi: api,
}))

const deadRow = {
  id: 1,
  tenantId: 1,
  messageSendRecordId: 101,
  executionId: 'exec-1',
  canvasId: 20,
  userId: 'user-1',
  nodeId: 'send-1',
  channel: 'SMS',
  provider: 'REACH',
  idempotencyKey: 'idem-1',
  status: 'DEAD',
  attemptCount: 3,
  providerMessageId: 'msg-1',
  lastError: 'provider down',
  createdAt: '2026-06-04T10:00:00',
  updatedAt: '2026-06-04T10:05:00',
}

describe('message delivery presentation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.list.mockResolvedValue({ data: { list: [], total: 0 } })
    api.detail.mockResolvedValue({ data: deadRow })
    api.receipts.mockResolvedValue({ data: [] })
    api.replay.mockResolvedValue({ data: { outboxId: 1, status: 'PENDING' } })
    api.reconcile.mockResolvedValue({ data: { requeued: 0 } })
  })

  it('formats status, replay state, filters, and receipt lines', () => {
    expect(deliveryStatusColor('DEAD')).toBe('red')
    expect(canReplayDelivery({ status: 'DEAD' })).toBe(true)
    expect(canReplayDelivery({ status: 'SENT' })).toBe(false)
    expect(deliveryDetailTitle({ id: 9, status: 'RETRY' })).toBe('投递 #9 · RETRY')
    expect(receiptLine({
      receiptType: 'DELIVERED',
      providerMessageId: 'msg-1',
      receivedAt: '2026-06-04T11:12:13',
    })).toBe('DELIVERED · msg-1 · 2026-06-04 11:12:13')
    expect(normalizeDeliveryFilters({
      executionId: ' exec-1 ',
      channel: ' sms ',
      provider: ' reach ',
      status: ' dead ',
      page: 0,
      size: 200,
    })).toEqual({
      executionId: 'exec-1',
      channel: 'SMS',
      provider: 'REACH',
      status: 'DEAD',
      page: 1,
      size: 100,
    })
  })

  it('renders loading and empty states', async () => {
    render(<MessageDeliveryPage />)

    await waitFor(() => expect(api.list).toHaveBeenCalledWith({ page: 1, size: 20 }))
    expect(screen.getByText('投递监控')).toBeInTheDocument()
    expect(screen.getByText('暂无投递记录')).toBeInTheDocument()
  })

  it('filters, opens detail drawer, renders receipts, and replays dead letters', async () => {
    api.list.mockResolvedValue({ data: { list: [deadRow], total: 1 } })
    api.receipts.mockResolvedValueOnce({
      data: [{
        id: 7,
        outboxId: 1,
        provider: 'REACH',
        providerMessageId: 'msg-1',
        receiptType: 'DELIVERED',
        rawPayloadJson: '{"eventId":"evt-1"}',
        idempotencyKey: 'receipt-1',
        receivedAt: '2026-06-04T11:12:13',
      }],
    })

    render(<MessageDeliveryPage />)

    await screen.findByText('exec-1')

    fireEvent.change(screen.getByPlaceholderText('画布 ID'), { target: { value: '20' } })
    await userEvent.type(screen.getByPlaceholderText('执行 ID'), 'exec-1')
    await userEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => expect(api.list).toHaveBeenLastCalledWith({
      canvasId: 20,
      executionId: 'exec-1',
      page: 1,
      size: 20,
    }))

    await userEvent.click(screen.getByRole('button', { name: '详情' }))
    await screen.findByText('投递 #1 · DEAD')
    expect(screen.getByText('DELIVERED · msg-1 · 2026-06-04 11:12:13')).toBeInTheDocument()
    expect(screen.getByText('{"eventId":"evt-1"}')).toBeInTheDocument()

    const drawer = screen.getByRole('dialog')
    const replayButton = within(drawer).getByRole('button', { name: '重放死信' })
    expect(replayButton).toBeEnabled()
    await userEvent.click(replayButton)

    await waitFor(() => expect(api.replay).toHaveBeenCalledWith(1))
  })
})
