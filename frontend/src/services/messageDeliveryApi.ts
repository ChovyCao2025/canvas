/**
 * API contract for delivery outbox, receipts, reconciliation, and replay.
 */
import type { PageResult, R } from '../types'
import http from './api'

export interface DeliveryOutbox {
  id: number
  tenantId?: number | null
  messageSendRecordId: number
  executionId: string
  canvasId: number
  userId: string
  nodeId: string
  channel: string
  provider: string
  payloadJson?: string | null
  idempotencyKey: string
  status: string
  attemptCount: number
  nextRetryAt?: string | null
  lockedBy?: string | null
  lockedAt?: string | null
  providerMessageId?: string | null
  providerResponseJson?: string | null
  lastError?: string | null
  createdAt: string
  updatedAt: string
  duplicate?: boolean
}

export interface DeliveryReceiptLog {
  id?: number
  tenantId?: number | null
  outboxId: number
  provider: string
  providerMessageId: string
  receiptType: string
  rawPayloadJson?: string | null
  idempotencyKey: string
  receivedAt: string
  createdAt?: string | null
}

export interface DeliverySearchParams {
  tenantId?: number
  canvasId?: number
  executionId?: string
  userId?: string
  channel?: string
  provider?: string
  status?: string
  providerMessageId?: string
  page?: number
  size?: number
}

export interface DeliveryReplayResult {
  outboxId: number
  status: string
}

export interface DeliveryReconcileResult {
  requeued: number
}

export function createMessageDeliveryApi(client = http) {
  return {
    list: (params: DeliverySearchParams) =>
      client.get<R<PageResult<DeliveryOutbox>>, R<PageResult<DeliveryOutbox>>>('/message-deliveries', { params }),
    detail: (id: number) =>
      client.get<R<DeliveryOutbox>, R<DeliveryOutbox>>(`/message-deliveries/${id}`),
    receipts: (id: number) =>
      client.get<R<DeliveryReceiptLog[]>, R<DeliveryReceiptLog[]>>(`/message-deliveries/${id}/receipts`),
    replay: (id: number) =>
      client.post<R<DeliveryReplayResult>, R<DeliveryReplayResult>>(`/message-deliveries/${id}/replay`),
    reconcile: () =>
      client.post<R<DeliveryReconcileResult>, R<DeliveryReconcileResult>>('/message-deliveries/reconcile'),
  }
}

export const messageDeliveryApi = createMessageDeliveryApi()
