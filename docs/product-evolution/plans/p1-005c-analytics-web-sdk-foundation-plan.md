# Analytics Web SDK Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a minimal TypeScript browser analytics SDK that sends governed batches to `/cdp/events/track` and enforces queue, identity, consent, flush, and reset behavior.

**Architecture:** Keep the first SDK package self-contained under `sdk/analytics-web`. Build a small `CanvasAnalytics` class around an in-memory queue with optional localStorage persistence, enrich every event with library/session/page context, and send batches through `fetch` using Basic Auth `base64(writeKey + ":")`.

**Tech Stack:** TypeScript, Vitest, browser `fetch`, browser `localStorage`, Web Crypto fallback-free UUID generation, npm scripts.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005c-analytics-web-sdk-foundation.md`
- Depends on: `docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md`
- Depends on: `docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md`

## File Structure

**SDK package**
- Create: `sdk/analytics-web/package.json`
- Create: `sdk/analytics-web/tsconfig.json`
- Create: `sdk/analytics-web/src/index.ts`
- Create: `sdk/analytics-web/src/index.test.ts`
- Create: `sdk/analytics-web/README.md`

### Task 1: Package Skeleton And Tests

**Files:**
- Create: `sdk/analytics-web/package.json`
- Create: `sdk/analytics-web/tsconfig.json`
- Create: `sdk/analytics-web/src/index.test.ts`

- [ ] **Step 1: Create package metadata**

Create `sdk/analytics-web/package.json`:

```json
{
  "name": "@canvas/analytics-web",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "scripts": {
    "test": "vitest run",
    "build": "tsc -p tsconfig.json"
  },
  "devDependencies": {
    "typescript": "^5.4.5",
    "vitest": "^3.2.4"
  }
}
```

Create `sdk/analytics-web/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ES2020",
    "moduleResolution": "Bundler",
    "declaration": true,
    "outDir": "dist",
    "strict": true,
    "lib": ["ES2020", "DOM"],
    "skipLibCheck": true
  },
  "include": ["src"]
}
```

- [ ] **Step 2: Write SDK behavior tests**

Create `sdk/analytics-web/src/index.test.ts`:

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CanvasAnalytics, createAnalytics } from './index'

function okFetch() {
  return vi.fn().mockResolvedValue({ ok: true, status: 200 })
}

describe('CanvasAnalytics', () => {
  beforeEach(() => {
    localStorage.clear()
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
    })
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
```

- [ ] **Step 3: Run tests and confirm red state**

Run:

```bash
cd sdk/analytics-web && npm test -- index.test.ts
```

Expected: FAIL because `src/index.ts` does not exist.

### Task 2: SDK Implementation

**Files:**
- Create: `sdk/analytics-web/src/index.ts`
- Test: `sdk/analytics-web/src/index.test.ts`

- [ ] **Step 1: Implement SDK public API**

Create `sdk/analytics-web/src/index.ts`:

```ts
export interface AnalyticsConfig {
  writeKey: string
  serverUrl: string
  flushAt?: number
  disableClientPersistence?: boolean
  isComplianceEnabled?: boolean
}

export interface AnalyticsRuntimeOptions {
  fetchImpl?: typeof fetch
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
  groupId?: string
  previousId?: string
  properties?: Record<string, unknown>
  traits?: Record<string, unknown>
  context: Record<string, unknown>
  timestamp: string
  sentAt?: string
}

const QUEUE_KEY = 'canvas_sdk_queue'
const ANON_KEY = 'canvas_anonymous_id'
const USER_KEY = 'canvas_user_id'
const OPTOUT_KEY = 'canvas_opted_out'

export class CanvasAnalytics {
  private config: AnalyticsConfig | null = null
  private queue: AnalyticsEvent[] = []
  private fetchImpl: typeof fetch
  private storage: Storage | null
  private anonymous = ''
  private user: string | null = null
  private optedOut = false

  constructor(options: AnalyticsRuntimeOptions = {}) {
    this.fetchImpl = options.fetchImpl || fetch
    this.storage = options.storage || (typeof localStorage === 'undefined' ? null : localStorage)
    this.optedOut = this.storage?.getItem(OPTOUT_KEY) === '1'
    this.anonymous = this.storage?.getItem(ANON_KEY) || makeId('anon')
    this.user = this.storage?.getItem(USER_KEY)
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
    if (!config.disableClientPersistence && !this.optedOut) {
      this.storage?.setItem(ANON_KEY, this.anonymous)
      if (this.user) this.storage?.setItem(USER_KEY, this.user)
    }
    return this
  }

  track(event: string, properties: Record<string, unknown> = {}) {
    this.enqueue({ type: 'track', event, properties })
  }

  identify(userId: string, traits: Record<string, unknown> = {}) {
    this.user = userId
    if (!this.persistenceDisabled()) this.storage?.setItem(USER_KEY, userId)
    this.enqueue({ type: 'identify', userId, traits })
  }

  page(name = documentTitle(), properties: Record<string, unknown> = {}) {
    this.enqueue({ type: 'page', event: name, properties })
  }

  group(groupId: string, traits: Record<string, unknown> = {}) {
    this.enqueue({ type: 'group', groupId, traits })
  }

  alias(newId: string, previousId = this.anonymous) {
    this.user = newId
    if (!this.persistenceDisabled()) this.storage?.setItem(USER_KEY, newId)
    this.enqueue({ type: 'alias', userId: newId, previousId })
  }

  optIn() {
    this.optedOut = false
    this.storage?.removeItem(OPTOUT_KEY)
    if (!this.persistenceDisabled()) this.persistQueue()
  }

  optOut(options: OptOutOptions = {}) {
    this.optedOut = true
    this.queue = []
    this.storage?.setItem(OPTOUT_KEY, '1')
    if (options.clearPersistence) {
      this.storage?.removeItem(QUEUE_KEY)
      this.storage?.removeItem(ANON_KEY)
      this.storage?.removeItem(USER_KEY)
    }
  }

  hasOptedOut() {
    return this.optedOut
  }

  async flush() {
    if (this.optedOut || this.queue.length === 0) return
    if (!this.config) throw new Error('CanvasAnalytics is not loaded')
    const batch = this.queue.map(event => ({ ...event, sentAt: new Date().toISOString() }))
    const response = await this.fetchImpl(this.config.serverUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${base64(`${this.config.writeKey}:`)}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ batch, sentAt: new Date().toISOString() }),
    })
    if (!response.ok) {
      throw new Error(`Canvas analytics flush failed with status ${response.status}`)
    }
    this.queue = []
    this.persistQueue()
  }

  reset() {
    this.queue = []
    this.user = null
    this.anonymous = makeId('anon')
    this.storage?.removeItem(QUEUE_KEY)
    this.storage?.removeItem(USER_KEY)
    if (!this.persistenceDisabled()) this.storage?.setItem(ANON_KEY, this.anonymous)
  }

  queueSize() {
    return this.queue.length
  }

  peekQueue() {
    return [...this.queue]
  }

  anonymousId() {
    return this.anonymous
  }

  userId() {
    return this.user
  }

  private enqueue(input: Partial<AnalyticsEvent> & { type: EventType }) {
    if (this.optedOut) return
    const event: AnalyticsEvent = {
      messageId: makeId('msg'),
      type: input.type,
      event: input.event,
      userId: input.userId ?? this.user,
      anonymousId: this.anonymous,
      groupId: input.groupId,
      previousId: input.previousId,
      properties: input.properties,
      traits: input.traits,
      context: buildContext(),
      timestamp: new Date().toISOString(),
    }
    this.queue.push(event)
    this.persistQueue()
    if (this.config?.flushAt && this.queue.length >= this.config.flushAt) {
      void this.flush()
    }
  }

  private restoreQueue() {
    if (this.optedOut) return
    const raw = this.storage?.getItem(QUEUE_KEY)
    if (!raw) return
    try {
      this.queue = JSON.parse(raw)
    } catch {
      this.queue = []
    }
  }

  private persistQueue() {
    if (this.persistenceDisabled()) return
    this.storage?.setItem(QUEUE_KEY, JSON.stringify(this.queue))
    this.storage?.setItem(ANON_KEY, this.anonymous)
  }

  private persistenceDisabled() {
    return this.optedOut || this.config?.disableClientPersistence === true
  }
}

export function createAnalytics(options: AnalyticsRuntimeOptions = {}) {
  return new CanvasAnalytics(options)
}

function buildContext() {
  const locationObj = typeof location === 'undefined' ? null : location
  return {
    library: { name: '@canvas/analytics-web', version: '0.1.0' },
    page: locationObj ? {
      url: locationObj.href,
      path: locationObj.pathname,
      search: locationObj.search,
      hash: locationObj.hash,
      title: documentTitle(),
    } : {},
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    locale: typeof navigator === 'undefined' ? undefined : navigator.language,
  }
}

function documentTitle() {
  return typeof document === 'undefined' ? '' : document.title
}

function makeId(prefix: string) {
  const random = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : Math.random().toString(16).slice(2) + Date.now().toString(16)
  return `${prefix}_${random}`
}

function base64(value: string) {
  if (typeof btoa !== 'undefined') return btoa(value)
  throw new Error('base64 encoding is not available in this environment')
}
```

- [ ] **Step 2: Run SDK tests**

Run:

```bash
cd sdk/analytics-web && npm test -- index.test.ts
```

Expected: PASS.

- [ ] **Step 3: Run SDK type build**

Run:

```bash
cd sdk/analytics-web && npm run build
```

Expected: PASS and `dist/index.d.ts` is generated.

- [ ] **Step 4: Commit SDK implementation**

Run:

```bash
git add sdk/analytics-web/package.json sdk/analytics-web/tsconfig.json sdk/analytics-web/src
git commit -m "feat: add analytics web SDK foundation"
```

Expected: commit contains only SDK package metadata, source, and tests.

### Task 3: README And Protocol Examples

**Files:**
- Create: `sdk/analytics-web/README.md`

- [ ] **Step 1: Write README**

Create `sdk/analytics-web/README.md`:

````markdown
# @canvas/analytics-web

Minimal browser analytics SDK for Canvas CDP event ingestion.

## Immediate Load

```ts
import { CanvasAnalytics } from '@canvas/analytics-web'

const analytics = CanvasAnalytics.load({
  writeKey: 'ck_xxx',
  serverUrl: 'https://canvas.example.com/cdp/events/track',
})

analytics.track('OrderComplete', {
  orderId: 'ORD-001',
  amount: 99.9,
  currency: 'CNY',
})
```

## Delayed Consent

```ts
import { createAnalytics } from '@canvas/analytics-web'

const analytics = createAnalytics()

function onConsentAccepted() {
  analytics.optIn()
  analytics.load({
    writeKey: 'ck_xxx',
    serverUrl: 'https://canvas.example.com/cdp/events/track',
  })
}

function onConsentRejected() {
  analytics.optOut({ clearPersistence: true })
}
```

## Identity

```ts
analytics.identify('user-123', {
  vipLevel: 'gold',
  registeredAt: '2026-01-01',
})

analytics.alias('user-123')
```

## Page And Group

```ts
analytics.page('ProductDetail', { productId: 'P001' })
analytics.group('company-001', { industry: 'SaaS' })
```

## Flush And Reset

```ts
await analytics.flush()
analytics.reset()
```

## Opt Out

```ts
analytics.optOut({ clearPersistence: true })
analytics.hasOptedOut()
```

## Request Payload

The SDK sends:

```json
{
  "batch": [
    {
      "messageId": "msg_uuid",
      "type": "track",
      "event": "OrderComplete",
      "userId": "user-123",
      "anonymousId": "anon_uuid",
      "properties": { "amount": 99.9 },
      "context": {
        "library": { "name": "@canvas/analytics-web", "version": "0.1.0" },
        "page": { "url": "https://example.com/cart", "path": "/cart" },
        "timezone": "Asia/Shanghai",
        "locale": "zh-CN"
      },
      "timestamp": "2026-06-03T10:00:00.000Z",
      "sentAt": "2026-06-03T10:00:01.000Z"
    }
  ],
  "sentAt": "2026-06-03T10:00:01.000Z"
}
```

Authentication uses:

```http
Authorization: Basic base64(writeKey + ":")
```
````

- [ ] **Step 2: Verify README contains required examples**

Run:

```bash
rg -n "Immediate Load|Delayed Consent|track|identify|page|optOut|flush|reset|Authorization" sdk/analytics-web/README.md
```

Expected: each term appears at least once.

- [ ] **Step 3: Commit README**

Run:

```bash
git add sdk/analytics-web/README.md
git commit -m "docs: add analytics web SDK usage"
```

Expected: commit contains only SDK README.

### Task 4: Focused Verification

**Files:**
- Modify: `docs/product-evolution/specs/p1-005c-analytics-web-sdk-foundation.md`
- Modify: `docs/product-evolution/plans/p1-005c-analytics-web-sdk-foundation-plan.md`

- [ ] **Step 1: Run SDK verification**

Run:

```bash
cd sdk/analytics-web && npm test -- index.test.ts
cd sdk/analytics-web && npm run build
```

Expected: PASS.

- [ ] **Step 2: Verify docs contract**

Run:

```bash
rg -n "writeKey|/cdp/events/track|messageId|anonymousId|optOut|reset|Basic" sdk/analytics-web/README.md
```

Expected: all protocol terms are present.

- [ ] **Step 3: Commit docs plan**

Run:

```bash
git add docs/product-evolution/specs/p1-005c-analytics-web-sdk-foundation.md \
  docs/product-evolution/plans/p1-005c-analytics-web-sdk-foundation-plan.md
git commit -m "docs: add analytics web SDK implementation plan"
```

Expected: commit contains only P1-005C spec and plan if implementation was committed in previous tasks.
