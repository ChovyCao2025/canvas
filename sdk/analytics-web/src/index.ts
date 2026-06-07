export interface AnalyticsConfig {
  writeKey: string
  serverUrl: string
  flushAt?: number
  disableClientPersistence?: boolean
  isComplianceEnabled?: boolean
}

export type FetchLike = (input: string, init?: RequestInit) => Promise<{ ok: boolean; status: number }>

export interface AnalyticsRuntimeOptions {
  fetchImpl?: FetchLike
  storage?: Storage
}

export interface OptOutOptions {
  clearPersistence?: boolean
}

export type EventType = 'track' | 'identify' | 'page' | 'group' | 'alias'

export interface AnalyticsEvent {
  messageId: string
  type: EventType
  event?: string
  userId?: string | null
  anonymousId: string
  idempotencyKey: string
  groupId?: string
  previousId?: string
  properties?: Record<string, unknown>
  traits?: Record<string, unknown>
  context: Record<string, unknown>
  timestamp: string
  sentAt?: string
}

interface EventInput {
  type: EventType
  event?: string
  userId?: string | null
  groupId?: string
  previousId?: string
  properties?: Record<string, unknown>
  traits?: Record<string, unknown>
}

const SDK_VERSION = '0.1.0'
const QUEUE_KEY = 'canvas_sdk_queue'
const ANON_KEY = 'canvas_anonymous_id'
const USER_KEY = 'canvas_user_id'
const OPTOUT_KEY = 'canvas_opted_out'

export class CanvasAnalytics {
  private config: AnalyticsConfig | null = null
  private queue: AnalyticsEvent[] = []
  private fetchImpl?: FetchLike
  private storage: Storage | null
  private anonymous = ''
  private user: string | null = null
  private optedOut = false
  private consentGranted = true

  constructor(options: AnalyticsRuntimeOptions = {}) {
    this.fetchImpl = options.fetchImpl || defaultFetch()
    this.storage = options.storage || defaultStorage()
    this.optedOut = this.storage?.getItem(OPTOUT_KEY) === '1'
    this.anonymous = this.storage?.getItem(ANON_KEY) || makeId('anon')
    this.user = this.storage?.getItem(USER_KEY) ?? null
    this.restoreQueue()
  }

  static load(config: AnalyticsConfig, options: AnalyticsRuntimeOptions = {}) {
    const analytics = new CanvasAnalytics(options)
    analytics.load(config)
    return analytics
  }

  load(config: AnalyticsConfig) {
    if (!config.writeKey || !config.serverUrl) {
      throw new Error('writeKey and serverUrl are required')
    }
    this.config = { flushAt: 20, ...config }
    this.consentGranted = !this.config.isComplianceEnabled
    this.persistIdentity()
    this.persistQueue()
    return this
  }

  track(event: string, properties: Record<string, unknown> = {}) {
    return this.enqueue({ type: 'track', event, properties })
  }

  identify(userId: string, traits: Record<string, unknown> = {}) {
    if (this.optedOut) return this
    this.user = requireText(userId, 'userId')
    this.persistIdentity()
    return this.enqueue({ type: 'identify', userId: this.user, traits, properties: traits })
  }

  page(name = documentTitle(), properties: Record<string, unknown> = {}) {
    return this.enqueue({ type: 'page', event: name, properties })
  }

  group(groupId: string, traits: Record<string, unknown> = {}) {
    return this.enqueue({
      type: 'group',
      groupId: requireText(groupId, 'groupId'),
      traits,
      properties: { groupId, ...traits },
    })
  }

  alias(newId: string, previousId = this.anonymous) {
    if (this.optedOut) return this
    const nextUserId = requireText(newId, 'newId')
    this.user = nextUserId
    this.persistIdentity()
    return this.enqueue({
      type: 'alias',
      userId: nextUserId,
      previousId,
      properties: { previousId },
    })
  }

  optIn() {
    this.optedOut = false
    this.consentGranted = true
    this.storage?.removeItem(OPTOUT_KEY)
    this.persistIdentity()
    this.persistQueue()
    return this
  }

  optOut(options: OptOutOptions = {}) {
    this.optedOut = true
    this.queue = []
    this.user = null
    this.storage?.setItem(OPTOUT_KEY, '1')
    this.storage?.removeItem(QUEUE_KEY)
    if (options.clearPersistence) {
      this.storage?.removeItem(ANON_KEY)
      this.storage?.removeItem(USER_KEY)
      this.anonymous = makeId('anon')
    }
    return this
  }

  hasOptedOut() {
    return this.optedOut
  }

  async flush() {
    if (this.optedOut || this.waitingForConsent() || this.queue.length === 0) {
      return
    }
    if (!this.config) {
      throw new Error('Canvas analytics must be loaded before flush')
    }
    if (!this.fetchImpl) {
      throw new Error('fetch is not available')
    }
    const sentAt = new Date().toISOString()
    const batch = this.queue.map(event => ({ ...event, sentAt }))
    const response = await this.fetchImpl(this.config.serverUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${base64Encode(`${this.config.writeKey}:`)}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ batch, sentAt }),
    })
    if (!response.ok) {
      throw new Error(`Canvas analytics flush failed with status ${response.status}`)
    }
    const sentIds = new Set(batch.map(event => event.messageId))
    this.queue = this.queue.filter(event => !sentIds.has(event.messageId))
    this.persistQueue()
  }

  reset() {
    this.queue = []
    this.user = null
    this.anonymous = makeId('anon')
    this.storage?.removeItem(QUEUE_KEY)
    this.storage?.removeItem(USER_KEY)
    this.persistIdentity()
    return this
  }

  queueSize() {
    return this.queue.length
  }

  peekQueue() {
    return this.queue.map(event => ({ ...event }))
  }

  userId() {
    return this.user
  }

  anonymousId() {
    return this.anonymous
  }

  private enqueue(input: EventInput) {
    if (this.optedOut) return this
    const messageId = makeId('msg')
    const event: AnalyticsEvent = {
      messageId,
      type: input.type,
      event: input.event,
      userId: input.userId ?? this.user,
      anonymousId: this.anonymous,
      idempotencyKey: messageId,
      groupId: input.groupId,
      previousId: input.previousId,
      properties: input.properties || {},
      traits: input.traits,
      context: buildContext(),
      timestamp: new Date().toISOString(),
    }
    this.queue.push(stripUndefined(event))
    this.persistQueue()
    if (this.config?.flushAt && this.queue.length >= this.config.flushAt) {
      void this.flush()
    }
    return this
  }

  private restoreQueue() {
    const raw = this.storage?.getItem(QUEUE_KEY)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw)
      this.queue = Array.isArray(parsed) ? parsed : []
    } catch {
      this.queue = []
    }
  }

  private persistIdentity() {
    if (this.persistenceDisabled()) return
    this.storage?.setItem(ANON_KEY, this.anonymous)
    if (this.user) {
      this.storage?.setItem(USER_KEY, this.user)
    }
  }

  private persistQueue() {
    if (this.persistenceDisabled()) return
    this.storage?.setItem(QUEUE_KEY, JSON.stringify(this.queue))
  }

  private persistenceDisabled() {
    return this.optedOut
      || this.config?.disableClientPersistence === true
      || this.waitingForConsent()
  }

  private waitingForConsent() {
    return this.config?.isComplianceEnabled === true && !this.consentGranted
  }
}

export function createAnalytics(options: AnalyticsRuntimeOptions = {}) {
  return new CanvasAnalytics(options)
}

function buildContext() {
  return {
    library: {
      name: '@canvas/analytics-web',
      version: SDK_VERSION,
    },
    page: pageContext(),
    campaign: campaignContext(),
    sessionId: sessionId(),
  }
}

function pageContext() {
  if (typeof window === 'undefined') {
    return {}
  }
  return {
    url: window.location.href,
    path: window.location.pathname,
    search: window.location.search,
    title: documentTitle(),
    referrer: typeof document === 'undefined' ? '' : document.referrer,
  }
}

function campaignContext() {
  if (typeof window === 'undefined') {
    return {}
  }
  const params = new URLSearchParams(window.location.search)
  const campaign: Record<string, string> = {}
  for (const key of ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content']) {
    const value = params.get(key)
    if (value) campaign[key] = value
  }
  return campaign
}

function sessionId() {
  const storage = defaultStorage('session')
  const key = 'canvas_session_id'
  const existing = storage?.getItem(key)
  if (existing) return existing
  const next = makeId('sess')
  storage?.setItem(key, next)
  return next
}

function documentTitle() {
  return typeof document === 'undefined' ? '' : document.title
}

function makeId(prefix: string) {
  const random = globalThis.crypto?.randomUUID?.()
    || `${Date.now().toString(36)}_${Math.random().toString(36).slice(2)}`
  return `${prefix}_${random}`
}

function base64Encode(value: string) {
  return btoa(value)
}

function requireText(value: string, fieldName: string) {
  if (!value || !value.trim()) {
    throw new Error(`${fieldName} is required`)
  }
  return value.trim()
}

function defaultStorage(kind: 'local' | 'session' = 'local') {
  if (typeof window === 'undefined') return null
  try {
    return kind === 'session' ? window.sessionStorage : window.localStorage
  } catch {
    return null
  }
}

function defaultFetch() {
  if (typeof fetch === 'undefined') return undefined
  return fetch.bind(globalThis) as FetchLike
}

function stripUndefined<T extends object>(value: T): T {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined)) as T
}
