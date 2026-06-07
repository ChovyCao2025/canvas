/**
 * CDP event ingestion operator API helpers.
 */
import type { R } from '../types'
import http from './api'

export interface WriteKeyCreateForm {
  name: string
  platform?: string
  rateLimitQps?: number
  dailyQuota?: number | null
  description?: string
}

export interface WriteKeyRow {
  id: number
  name: string
  keyPrefix: string
  platform: string
  status: string
  rateLimitQps?: number
  dailyQuota?: number | null
  description?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface WriteKeyCreateResp {
  id: number
  name: string
  writeKey: string
  keyPrefix: string
  platform: string
  rateLimitQps: number
  dailyQuota?: number | null
}

export interface DiscoveredAttribute {
  id: number
  eventCode: string
  attrName: string
  attrType: string
  status: string
  sampleValue?: string | null
  firstSeenAt?: string | null
  lastSeenAt?: string | null
}

export interface WebhookSubscriptionForm {
  name: string
  callbackUrl: string
  eventTypesText: string
  maxAttempts?: number
}

export interface WebhookSubscriptionPayload {
  name: string
  callbackUrl: string
  eventTypes: string[]
  maxAttempts: number
}

export interface WebhookSubscriptionRow {
  id: number
  name: string
  callbackUrl: string
  secretPrefix: string
  eventTypes: string[]
  status: string
  maxAttempts: number
  createdAt?: string | null
  updatedAt?: string | null
}

export interface WebhookDeliveryRow {
  id: number
  deliveryId: string
  eventType: string
  attempt: number
  httpStatus?: number | null
  status: string
  nextRetryAt?: string | null
  errorMessage?: string | null
  terminalReason?: string | null
  createdAt?: string | null
}

export interface WebhookRotateSecretResp {
  subscriptionId: number
  secret: string
  secretPrefix: string
}

export function buildCreateWriteKeyPayload(form: WriteKeyCreateForm) {
  return {
    name: form.name.trim(),
    platform: form.platform || 'WEB',
    rateLimitQps: form.rateLimitQps || 100,
    dailyQuota: form.dailyQuota ?? null,
    description: form.description || '',
  }
}

export function buildWebhookSubscriptionPayload(form: WebhookSubscriptionForm): WebhookSubscriptionPayload {
  const eventTypes = Array.from(new Set(form.eventTypesText
    .split(/[\n,]/)
    .map(value => value.trim())
    .filter(Boolean)))
  return {
    name: form.name.trim(),
    callbackUrl: form.callbackUrl.trim(),
    eventTypes,
    maxAttempts: form.maxAttempts && form.maxAttempts > 0 ? form.maxAttempts : 3,
  }
}

const ATTRIBUTE_STATUS_ORDER: Record<string, number> = {
  PENDING_REVIEW: 0,
  APPROVED: 1,
  REJECTED: 2,
}

export function normalizeDiscoveredAttributeRows(rows: DiscoveredAttribute[]) {
  return [...rows].sort((a, b) => {
    const statusOrder = (ATTRIBUTE_STATUS_ORDER[a.status] ?? 99) - (ATTRIBUTE_STATUS_ORDER[b.status] ?? 99)
    if (statusOrder !== 0) return statusOrder
    const eventOrder = a.eventCode.localeCompare(b.eventCode)
    if (eventOrder !== 0) return eventOrder
    return a.attrName.localeCompare(b.attrName)
  })
}

export function safeWriteKeyRows(rows: WriteKeyRow[]) {
  return rows.map(row => ({
    id: row.id,
    name: row.name,
    keyPrefix: row.keyPrefix,
    platform: row.platform,
    status: row.status,
    rateLimitQps: row.rateLimitQps,
    dailyQuota: row.dailyQuota,
    description: row.description,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
  }))
}

export function createCdpEventApi(client = http) {
  return {
    listWriteKeys: () =>
      client.get<R<WriteKeyRow[]>, R<WriteKeyRow[]>>('/cdp/write-keys'),
    createWriteKey: (form: WriteKeyCreateForm) =>
      client.post<R<WriteKeyCreateResp>, R<WriteKeyCreateResp>>('/cdp/write-keys', buildCreateWriteKeyPayload(form)),
    disableWriteKey: (id: number) =>
      client.delete<R<void>, R<void>>(`/cdp/write-keys/${id}`),
    listDiscoveredAttributes: (status?: string) =>
      status
        ? client.get<R<DiscoveredAttribute[]>, R<DiscoveredAttribute[]>>(
          '/canvas/event-attributes/discovered',
          { params: { status } },
        )
        : client.get<R<DiscoveredAttribute[]>, R<DiscoveredAttribute[]>>('/canvas/event-attributes/discovered'),
    listWebhookSubscriptions: () =>
      client.get<R<WebhookSubscriptionRow[]>, R<WebhookSubscriptionRow[]>>('/cdp/webhooks'),
    createWebhookSubscription: (form: WebhookSubscriptionForm) =>
      client.post<R<WebhookSubscriptionRow>, R<WebhookSubscriptionRow>>(
        '/cdp/webhooks',
        buildWebhookSubscriptionPayload(form),
      ),
    updateWebhookSubscription: (id: number, form: WebhookSubscriptionForm) =>
      client.put<R<WebhookSubscriptionRow>, R<WebhookSubscriptionRow>>(
        `/cdp/webhooks/${id}`,
        buildWebhookSubscriptionPayload(form),
      ),
    pauseWebhookSubscription: (id: number) =>
      client.put<R<void>, R<void>>(`/cdp/webhooks/${id}/pause`),
    resumeWebhookSubscription: (id: number) =>
      client.put<R<void>, R<void>>(`/cdp/webhooks/${id}/resume`),
    disableWebhookSubscription: (id: number) =>
      client.delete<R<void>, R<void>>(`/cdp/webhooks/${id}`),
    rotateWebhookSecret: (id: number) =>
      client.post<R<WebhookRotateSecretResp>, R<WebhookRotateSecretResp>>(`/cdp/webhooks/${id}/rotate-secret`, {}),
    testWebhookDelivery: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/webhooks/${id}/test`, {}),
    listWebhookDeliveries: (id: number) =>
      client.get<R<WebhookDeliveryRow[]>, R<WebhookDeliveryRow[]>>(`/cdp/webhooks/${id}/deliveries`),
  }
}

export const cdpEventApi = createCdpEventApi()
