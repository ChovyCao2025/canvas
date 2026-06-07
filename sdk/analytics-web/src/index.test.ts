import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CanvasAnalytics, createAnalytics } from './index'

function okFetch() {
  return vi.fn().mockResolvedValue({ ok: true, status: 200 })
}

class MemoryStorage implements Storage {
  private values = new Map<string, string>()

  get length() {
    return this.values.size
  }

  clear() {
    this.values.clear()
  }

  getItem(key: string) {
    return this.values.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.values.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.values.delete(key)
  }

  setItem(key: string, value: string) {
    this.values.set(key, value)
  }
}

describe('CanvasAnalytics', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'localStorage', { value: new MemoryStorage(), configurable: true })
    Object.defineProperty(window, 'sessionStorage', { value: new MemoryStorage(), configurable: true })
    vi.restoreAllMocks()
  })

  it('loads with write key and queues track events', () => {
    const analytics = createAnalytics()
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })

    analytics.track('OrderComplete', { amount: 99 })

    expect(analytics.queueSize()).toBe(1)
    expect(analytics.peekQueue()[0]).toMatchObject({
      type: 'track',
      event: 'OrderComplete',
      properties: { amount: 99 },
    })
    expect(analytics.peekQueue()[0].messageId).toMatch(/^msg_/)
    expect(analytics.peekQueue()[0].context.library).toMatchObject({ name: '@canvas/analytics-web' })
  })

  it('identify sets user id and sends traits event', () => {
    const analytics = createAnalytics()
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })

    analytics.identify('user-1', { vipLevel: 'gold' })

    expect(analytics.userId()).toBe('user-1')
    expect(analytics.peekQueue()[0]).toMatchObject({
      type: 'identify',
      userId: 'user-1',
      traits: { vipLevel: 'gold' },
      properties: { vipLevel: 'gold' },
    })
  })

  it('page group and alias use the shared event envelope', () => {
    const analytics = createAnalytics()
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })

    analytics.page('ProductDetail', { sku: 'sku-1' })
    analytics.group('company-1', { tier: 'enterprise' })
    analytics.alias('user-2', 'anon-old')

    expect(analytics.peekQueue().map(event => event.type)).toEqual(['page', 'group', 'alias'])
    expect(analytics.peekQueue()[0]).toMatchObject({ event: 'ProductDetail', properties: { sku: 'sku-1' } })
    expect(analytics.peekQueue()[1]).toMatchObject({ groupId: 'company-1', properties: { tier: 'enterprise' } })
    expect(analytics.peekQueue()[2]).toMatchObject({ userId: 'user-2', previousId: 'anon-old' })
  })

  it('flush sends Basic Auth batch and clears successful events', async () => {
    const fetchMock = okFetch()
    const analytics = createAnalytics({ fetchImpl: fetchMock })
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })
    analytics.track('OrderComplete', { amount: 99 })

    await analytics.flush()

    expect(fetchMock).toHaveBeenCalledWith(
      'https://example.test/cdp/events/track',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: `Basic ${btoa('ck_test_123:')}`,
          'Content-Type': 'application/json',
        }),
      }),
    )
    const body = JSON.parse(fetchMock.mock.calls[0][1].body)
    expect(body.batch[0]).toMatchObject({ type: 'track', event: 'OrderComplete' })
    expect(body.batch[0].sentAt).toBeTruthy()
    expect(analytics.queueSize()).toBe(0)
  })

  it('does not clear queue when flush fails', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 500 })
    const analytics = createAnalytics({ fetchImpl: fetchMock })
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })
    analytics.track('OrderComplete')

    await expect(analytics.flush()).rejects.toThrow('Canvas analytics flush failed with status 500')

    expect(analytics.queueSize()).toBe(1)
  })

  it('delayed consent keeps events in memory until opt in', async () => {
    const fetchMock = okFetch()
    const analytics = createAnalytics({ fetchImpl: fetchMock })
    analytics.load({
      writeKey: 'ck_test_123',
      serverUrl: 'https://example.test/cdp/events/track',
      isComplianceEnabled: true,
    })
    analytics.track('BeforeConsent')

    await analytics.flush()
    expect(fetchMock).not.toHaveBeenCalled()
    expect(localStorage.getItem('canvas_sdk_queue')).toBeNull()

    analytics.optIn()
    await analytics.flush()
    expect(fetchMock).toHaveBeenCalledOnce()
    expect(analytics.queueSize()).toBe(0)
  })

  it('optOut clears persistence and prevents sending', async () => {
    const fetchMock = okFetch()
    const analytics = createAnalytics({ fetchImpl: fetchMock })
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })
    analytics.track('BeforeOptOut')

    analytics.optOut({ clearPersistence: true })
    analytics.track('AfterOptOut')
    await analytics.flush()

    expect(analytics.hasOptedOut()).toBe(true)
    expect(analytics.queueSize()).toBe(0)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('reset clears user anonymous id and queue', () => {
    const analytics = new CanvasAnalytics()
    analytics.load({ writeKey: 'ck_test_123', serverUrl: 'https://example.test/cdp/events/track' })
    analytics.identify('user-1')
    const oldAnonymousId = analytics.anonymousId()

    analytics.reset()

    expect(analytics.userId()).toBe(null)
    expect(analytics.anonymousId()).not.toBe(oldAnonymousId)
    expect(analytics.queueSize()).toBe(0)
  })
})
