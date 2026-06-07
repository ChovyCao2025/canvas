import { describe, expect, it, vi } from 'vitest'

vi.mock('./api', () => ({
  default: {},
}))

import {
  buildCreateWriteKeyPayload,
  createCdpEventApi,
  buildWebhookSubscriptionPayload,
  normalizeDiscoveredAttributeRows,
  safeWriteKeyRows,
  type DiscoveredAttribute,
  type WriteKeyRow,
} from './cdpEventApi'

describe('cdp event api helpers', () => {
  it('builds write key payload with default platform and qps', () => {
    expect(buildCreateWriteKeyPayload({ name: ' Website ' })).toEqual({
      name: 'Website',
      platform: 'WEB',
      rateLimitQps: 100,
      dailyQuota: null,
      description: '',
    })
  })

  it('normalizes pending attributes first', () => {
    const rows: DiscoveredAttribute[] = [
      { id: 1, eventCode: 'OrderComplete', attrName: 'amount', attrType: 'NUMBER', status: 'APPROVED' },
      { id: 2, eventCode: 'OrderComplete', attrName: 'currency', attrType: 'STRING', status: 'PENDING_REVIEW' },
      { id: 3, eventCode: 'Signup', attrName: 'source', attrType: 'STRING', status: 'REJECTED' },
    ]

    expect(normalizeDiscoveredAttributeRows(rows).map(row => row.attrName)).toEqual(['currency', 'amount', 'source'])
  })

  it('drops secret-like fields from write key rows', () => {
    const rows = [{
      id: 1,
      name: 'Website',
      keyPrefix: 'ck_test',
      platform: 'WEB',
      status: 'ACTIVE',
      keyHash: 'hash',
      writeKey: 'raw',
    } as WriteKeyRow & { keyHash: string; writeKey: string }]

    expect(safeWriteKeyRows(rows)[0]).not.toHaveProperty('keyHash')
    expect(safeWriteKeyRows(rows)[0]).not.toHaveProperty('writeKey')
  })

  it('calls write key and discovered attribute endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
      delete: vi.fn().mockResolvedValue({ data: undefined }),
    }
    const api = createCdpEventApi(client as any)

    await api.listWriteKeys()
    await api.createWriteKey({ name: 'Website' })
    await api.disableWriteKey(7)
    await api.listDiscoveredAttributes()

    expect(client.get).toHaveBeenCalledWith('/cdp/write-keys')
    expect(client.post).toHaveBeenCalledWith('/cdp/write-keys', {
      name: 'Website',
      platform: 'WEB',
      rateLimitQps: 100,
      dailyQuota: null,
      description: '',
    })
    expect(client.delete).toHaveBeenCalledWith('/cdp/write-keys/7')
    expect(client.get).toHaveBeenCalledWith('/canvas/event-attributes/discovered')
  })

  it('builds webhook subscription payload with trimmed event types and default attempts', () => {
    expect(buildWebhookSubscriptionPayload({
      name: ' CRM ',
      callbackUrl: ' https://example.com/hook ',
      eventTypesText: 'cdp.event.ingested\n\nprofile.updated\ncdp.event.ingested',
    })).toEqual({
      name: 'CRM',
      callbackUrl: 'https://example.com/hook',
      eventTypes: ['cdp.event.ingested', 'profile.updated'],
      maxAttempts: 3,
    })
  })

  it('calls webhook subscription management endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
      delete: vi.fn().mockResolvedValue({ data: undefined }),
    }
    const api = createCdpEventApi(client as any)

    await api.listWebhookSubscriptions()
    await api.createWebhookSubscription({
      name: 'CRM',
      callbackUrl: 'https://example.com/hook',
      eventTypesText: 'cdp.event.ingested',
    })
    await api.updateWebhookSubscription(9, {
      name: 'CRM',
      callbackUrl: 'https://example.com/hook',
      eventTypesText: 'cdp.event.ingested',
      maxAttempts: 5,
    })
    await api.pauseWebhookSubscription(9)
    await api.resumeWebhookSubscription(9)
    await api.disableWebhookSubscription(9)
    await api.rotateWebhookSecret(9)
    await api.testWebhookDelivery(9)
    await api.listWebhookDeliveries(9)

    expect(client.get).toHaveBeenCalledWith('/cdp/webhooks')
    expect(client.post).toHaveBeenCalledWith('/cdp/webhooks', {
      name: 'CRM',
      callbackUrl: 'https://example.com/hook',
      eventTypes: ['cdp.event.ingested'],
      maxAttempts: 3,
    })
    expect(client.put).toHaveBeenCalledWith('/cdp/webhooks/9', {
      name: 'CRM',
      callbackUrl: 'https://example.com/hook',
      eventTypes: ['cdp.event.ingested'],
      maxAttempts: 5,
    })
    expect(client.put).toHaveBeenCalledWith('/cdp/webhooks/9/pause')
    expect(client.put).toHaveBeenCalledWith('/cdp/webhooks/9/resume')
    expect(client.delete).toHaveBeenCalledWith('/cdp/webhooks/9')
    expect(client.post).toHaveBeenCalledWith('/cdp/webhooks/9/rotate-secret', {})
    expect(client.post).toHaveBeenCalledWith('/cdp/webhooks/9/test', {})
    expect(client.get).toHaveBeenCalledWith('/cdp/webhooks/9/deliveries')
  })
})
