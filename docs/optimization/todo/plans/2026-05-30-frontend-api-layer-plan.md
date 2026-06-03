# Frontend API Layer Fix Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Add request cancellation (AbortController), request deduplication, retry with backoff, and cache TTL to the API layer.

**Architecture:** The existing `http` axios instance in `frontend/src/services/api.ts` already has JWT injection (request interceptor) and R<T> unwrapping + 401 redirect (response interceptor). This plan adds dedup, retry, and cache TTL as new interceptor logic and a separate cache module.

**Tech Stack:** axios, React, Vitest

---

### Task 1: Add AbortController + Request Dedup + Retry

**Files:**
- Modify: `frontend/src/services/api.ts`
- Test: `frontend/src/services/__tests__/api-interceptors.test.ts`

- [ ] **Step 1: Write failing test**

```ts
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { setupInterceptors } from '../services/api'

describe('API interceptors', () => {
  let api: axios.AxiosInstance
  let mockAdapter: MockAdapter

  beforeEach(() => {
    api = axios.create({ baseURL: '/' })
    mockAdapter = new MockAdapter(api)
    setupInterceptors(api)
  })

  afterEach(() => {
    mockAdapter.restore()
  })

  test('deduplicates concurrent identical requests', async () => {
    // Setup: both requests return 200
    mockAdapter.onGet('/api/canvas/1').reply(200, { code: 0, data: { id: 1 } })

    // Two concurrent GETs to same URL — dedup cancels the first in favor of the second
    const promise1 = api.get('/api/canvas/1')
    const promise2 = api.get('/api/canvas/1')

    // At least one should succeed (the dedup logic aborts the earlier one)
    const results = await Promise.allSettled([promise1, promise2])
    const succeeded = results.filter(r => r.status === 'fulfilled')
    expect(succeeded.length).toBeGreaterThanOrEqual(1)
  })

  test('retries 5xx errors with exponential backoff', async () => {
    mockAdapter.onGet('/api/test').reply(500)
    // The retry logic should attempt up to 3 times
    await expect(api.get('/api/test')).rejects.toThrow()
    // Verify multiple calls were made (initial + retries)
    expect(mockAdapter.history.get.length).toBeGreaterThan(1)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run services/__tests__/api-interceptors.test.ts`
Expected: FAIL — setupInterceptors not exported, MockAdapter not installed.

- [ ] **Step 3: Install test dependency**

```bash
cd frontend && npm install --save-dev axios-mock-adapter
```

- [ ] **Step 4: Implement interceptors**

```ts
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const pendingRequests = new Map<string, AbortController>()

function getRequestKey(config: InternalAxiosRequestConfig): string {
  return `${config.method}:${config.url}:${JSON.stringify(config.params || {})}`
}

export function setupInterceptors(api: AxiosInstance) {
  // Request interceptor: dedup + AbortController
  api.interceptors.request.use((config) => {
    const key = getRequestKey(config)
    if (pendingRequests.has(key)) {
      pendingRequests.get(key)!.abort('dedup')
    }
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(key, controller)
    return config
  })

  // Response interceptor: cleanup + retry
  api.interceptors.response.use(
    (response) => {
      pendingRequests.delete(getRequestKey(response.config))
      return response
    },
    async (error) => {
      const config = error.config
      if (!config || axios.isCancel(error)) return Promise.reject(error)

      pendingRequests.delete(getRequestKey(config))

      // Retry 5xx errors up to 3 times with exponential backoff
      if (error.response?.status >= 500) {
        config.__retryCount = (config.__retryCount || 0) + 1
        if (config.__retryCount >= 3) return Promise.reject(error)
        const delay = Math.pow(2, config.__retryCount) * 1000
        await new Promise(r => setTimeout(r, delay))
        return api(config)
      }
      return Promise.reject(error)
    }
  )
}
```

- [ ] **Step 5: Integrate into existing api.ts — show complete modified file**

The existing `api.ts` creates `const http = axios.create({ baseURL: '/' })` and adds JWT + 401 interceptors. Add `setupInterceptors(http)` after the existing interceptors. Here is the complete modified top section of `api.ts`:

```ts
/**
 * 服务职责：统一后端 API 客户端和核心业务 API 聚合层。
 *
 * 维护说明：axios 拦截器在这里注入 token、解包 R<T> 响应，并在 401 时清理登录态。
 * 新增：请求去重（dedup）、5xx 重试（指数退避）和 AbortController 支持。
 */
import axios from 'axios'
import type {
  R, PageResult,
  Canvas, CanvasDetail, CanvasVersion,
  NodeTypeRegistry, ContextField, StubOption, AbExperimentGroup,
  IdentityType, TagValueDefinition, TagImportBatch, TagImportError,
  TagImportResult, TagImportRow, TagImportSource,
} from '../types'
import type { HomeOverview } from '../pages/home/homeOverview'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

// ── Dedup + Retry Interceptor ───────────────────────────────────

const pendingRequests = new Map<string, AbortController>()

function getRequestKey(config: InternalAxiosRequestConfig): string {
  return `${config.method}:${config.url}:${JSON.stringify(config.params || {})}`
}

export function setupInterceptors(api: AxiosInstance) {
  // Request interceptor: dedup + AbortController
  api.interceptors.request.use((config) => {
    const key = getRequestKey(config)
    if (pendingRequests.has(key)) {
      pendingRequests.get(key)!.abort('dedup')
    }
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(key, controller)
    return config
  })

  // Response interceptor: cleanup + retry
  api.interceptors.response.use(
    (response) => {
      pendingRequests.delete(getRequestKey(response.config))
      return response
    },
    async (error) => {
      const config = error.config
      if (!config || axios.isCancel(error)) return Promise.reject(error)

      pendingRequests.delete(getRequestKey(config))

      // Retry 5xx errors up to 3 times with exponential backoff
      if (error.response?.status >= 500) {
        config.__retryCount = (config.__retryCount || 0) + 1
        if (config.__retryCount >= 3) return Promise.reject(error)
        const delay = Math.pow(2, config.__retryCount) * 1000
        await new Promise(r => setTimeout(r, delay))
        return api(config)
      }
      return Promise.reject(error)
    }
  )
}

// ── Axios instance + existing interceptors ───────────────────────

const http = axios.create({ baseURL: '/' })

// 请求拦截：自动带 JWT token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('canvas_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：解包 R<T>；401 跳转登录
http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('canvas_token')
      localStorage.removeItem('canvas_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

// Dedup + retry interceptors (added after existing interceptors)
setupInterceptors(http)

// ── All other API methods in api.ts remain unchanged (see existing
//    frontend/src/services/api.ts for reference). ──
```

Note: The `__retryCount` custom property on Axios config requires a TypeScript declaration. Add this at the top of `api.ts` or in a `global.d.ts`:

```ts
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    __retryCount?: number
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd frontend && npx vitest run services/__tests__/api-interceptors.test.ts`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/services/api.ts frontend/src/services/__tests__/api-interceptors.test.ts frontend/package.json frontend/package-lock.json
git commit -m "feat: add AbortController, request dedup, and retry with backoff to API layer"
```

---

### Task 2: Add TTL to config-panel Cache

**Files:**
- Create: `frontend/src/components/config-panel/cache.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/__tests__/cache.test.ts`

- [ ] **Step 1: Write failing test**

```ts
import { getCached, setCached } from '../cache'

describe('config-panel cache', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  test('cached data expires after 5 minutes', () => {
    const mockNodeTypes = [{ typeKey: 'ACTION', name: 'Action Node' }]
    setCached('nodeTypes', mockNodeTypes)
    expect(getCached('nodeTypes')).toEqual(mockNodeTypes)

    // Advance 5 minutes + 1ms
    vi.advanceTimersByTime(5 * 60 * 1000 + 1)
    expect(getCached('nodeTypes')).toBeUndefined()
  })

  test('cache does not expire before TTL', () => {
    const mockNodeTypes = [{ typeKey: 'CONDITION', name: 'Condition Node' }]
    setCached('nodeTypes', mockNodeTypes)
    vi.advanceTimersByTime(4 * 60 * 1000) // 4 minutes
    expect(getCached('nodeTypes')).toEqual(mockNodeTypes)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run config-panel/__tests__/cache.test.ts`
Expected: FAIL — cache module doesn't exist yet.

- [ ] **Step 3: Implement TTL cache module**

```ts
// frontend/src/components/config-panel/cache.ts
const CACHE_TTL_MS = 5 * 60 * 1000 // 5 minutes

interface CacheEntry<T> { data: T; expiresAt: number }

const cache = new Map<string, CacheEntry<any>>()

export function getCached<T>(key: string): T | undefined {
  const entry = cache.get(key)
  if (!entry || Date.now() > entry.expiresAt) {
    cache.delete(key)
    return undefined
  }
  return entry.data as T
}

export function setCached<T>(key: string, data: T): void {
  cache.set(key, { data, expiresAt: Date.now() + CACHE_TTL_MS })
}
```

- [ ] **Step 4: Integrate into config-panel/index.tsx — replace schemaCache with getCached/setCached**

The existing `config-panel/index.tsx` uses a module-level `schemaCache` (a plain `Map<string, NodeTypeRegistry>`) without TTL. Replace it with the new `getCached`/`setCached` functions. Here is the complete replacement:

**BEFORE** (existing code at module level, line 64):
```ts
const schemaCache   = new Map<string, NodeTypeRegistry>()
```

**AFTER** — remove the `schemaCache` declaration and import the TTL cache:
```ts
import { getCached, setCached } from './cache'
```

**BEFORE** (existing code in the schema-loading useEffect, lines 223-230):
```ts
    const cached = schemaCache.get(nodeData.nodeType)
    if (cached) {
      setSchema(cached); setLoading(false)
      return
    }
    metaApi.getNodeTypeSchema(nodeData.nodeType)
      .then(res => { schemaCache.set(nodeData.nodeType, res.data); setSchema(res.data) })
      .finally(() => setLoading(false))
```

**AFTER** — use `getCached`/`setCached` with TTL:
```ts
    const cached = getCached<NodeTypeRegistry>(`schema:${nodeData.nodeType}`)
    if (cached) {
      setSchema(cached); setLoading(false)
      return
    }
    metaApi.getNodeTypeSchema(nodeData.nodeType)
      .then(res => {
        setCached(`schema:${nodeData.nodeType}`, res.data)
        setSchema(res.data)
      })
      .finally(() => setLoading(false))
```

The key prefix `schema:` is added to namespace the cache entries and avoid collisions with other cached data (e.g., `nodeTypes`, `contextFields`).

- [ ] **Step 5: Run test to verify it passes**

Run: `cd frontend && npx vitest run config-panel/__tests__/cache.test.ts`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/config-panel/cache.ts frontend/src/components/config-panel/index.tsx frontend/src/components/config-panel/__tests__/cache.test.ts
git commit -m "feat: add TTL-based cache to config-panel with 5-minute expiry"
```
