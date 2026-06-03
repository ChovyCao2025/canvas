# Frontend HTTP Client And Runtime Schemas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tested frontend HTTP client wrapper and runtime validation for critical canvas contracts.

**Architecture:** Put cancellation, dedupe, retry, response unwrap, and 401 navigation behind `httpClient.ts`. Keep existing `api.ts` exports stable while gradually routing calls through the wrapper, and validate graph/node registry data with Zod before editor hydration.

**Tech Stack:** TypeScript, Axios, React Router integration, Zod, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-007c-frontend-http-client-and-runtime-schemas.md`

## Current Code Facts

- `frontend/src/services/api.ts` creates a bare Axios instance and redirects 401 with `window.location.href = '/login'`.
- `frontend/package.json` has Axios but does not include Zod.
- Existing API tests live in `frontend/src/services/api.test.ts`.

## File Structure

- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/services/httpClient.ts`
- Create: `frontend/src/services/httpClient.test.ts`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/types/canvasSchemas.ts`
- Create: `frontend/src/types/canvasSchemas.test.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

### Task 1: HTTP Client Tests And Wrapper

**Files:**
- Create: `frontend/src/services/httpClient.test.ts`
- Create: `frontend/src/services/httpClient.ts`
- Modify: `frontend/src/services/api.ts`

- [ ] **Step 1: Write HTTP client tests**

Create `frontend/src/services/httpClient.test.ts`:

```ts
import axios from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { ApiHttpError, createHttpClient } from './httpClient'

describe('httpClient', () => {
  it('dedupes identical in-flight GET requests by requestKey', async () => {
    const adapter = vi.fn().mockResolvedValue({ status: 200, statusText: 'OK', headers: {}, config: {}, data: { code: 0, data: { ok: true } } })
    const client = createHttpClient(axios.create({ adapter }))

    const first = client.get('/api/canvas/1', { requestKey: 'canvas:1' })
    const second = client.get('/api/canvas/1', { requestKey: 'canvas:1' })

    await expect(Promise.all([first, second])).resolves.toEqual([{ ok: true }, { ok: true }])
    expect(adapter).toHaveBeenCalledTimes(1)
  })

  it('retries idempotent GET and does not retry POST by default', async () => {
    const adapter = vi.fn()
      .mockRejectedValueOnce({ code: 'ECONNRESET' })
      .mockResolvedValueOnce({ status: 200, statusText: 'OK', headers: {}, config: {}, data: { code: 0, data: { ok: true } } })
    const client = createHttpClient(axios.create({ adapter }), { maxGetRetries: 1 })

    await expect(client.get('/api/retry')).resolves.toEqual({ ok: true })
    expect(adapter).toHaveBeenCalledTimes(2)

    adapter.mockClear()
    adapter.mockRejectedValueOnce({ code: 'ECONNRESET' })
    await expect(client.post('/api/no-retry', {})).rejects.toBeInstanceOf(ApiHttpError)
    expect(adapter).toHaveBeenCalledTimes(1)
  })

  it('normalizes business errors and 401 navigation callback', async () => {
    const unauthorized = vi.fn()
    const adapter = vi.fn().mockRejectedValue({ response: { status: 401, data: { message: 'expired' } } })
    const client = createHttpClient(axios.create({ adapter }), { onUnauthorized: unauthorized })

    await expect(client.get('/api/protected', { intendedPath: '/canvas/1' })).rejects.toMatchObject({ status: 401 })
    expect(unauthorized).toHaveBeenCalledWith('/canvas/1')
  })
})
```

- [ ] **Step 2: Run HTTP tests and confirm red state**

Run:

```bash
cd frontend && npm test -- httpClient.test.ts
```

Expected: FAIL because `httpClient.ts` does not exist.

- [ ] **Step 3: Implement HTTP client wrapper**

Create `frontend/src/services/httpClient.ts`:

```ts
import type { AxiosInstance, AxiosRequestConfig } from 'axios'

export class ApiHttpError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly data?: unknown,
  ) {
    super(message)
    this.name = 'ApiHttpError'
  }
}

export interface RequestOptions extends AxiosRequestConfig {
  requestKey?: string
  intendedPath?: string
}

export interface HttpClientOptions {
  maxGetRetries?: number
  onUnauthorized?: (intendedPath?: string) => void
}

export function createHttpClient(instance: AxiosInstance, options: HttpClientOptions = {}) {
  const inFlight = new Map<string, Promise<unknown>>()

  async function request<T>(method: 'get' | 'post' | 'put' | 'delete', url: string, data?: unknown, config: RequestOptions = {}): Promise<T> {
    const key = method === 'get' ? config.requestKey : undefined
    if (key && inFlight.has(key)) return inFlight.get(key) as Promise<T>

    const run = async () => {
      const attempts = method === 'get' ? (options.maxGetRetries ?? 0) + 1 : 1
      for (let attempt = 1; attempt <= attempts; attempt += 1) {
        try {
          const response = await instance.request({ ...config, method, url, data })
          const payload = response.data
          if (payload && typeof payload === 'object' && 'code' in payload) {
            if (payload.code !== 0) throw new ApiHttpError(payload.message ?? 'Business error', Number(payload.code), payload.data)
            return payload.data as T
          }
          return payload as T
        } catch (error: any) {
          const status = error?.response?.status
          if (status === 401) {
            options.onUnauthorized?.(config.intendedPath)
          }
          if (attempt < attempts && !status) continue
          throw new ApiHttpError(error?.response?.data?.message ?? error?.message ?? 'Request failed', status, error?.response?.data)
        }
      }
      throw new ApiHttpError('Request failed')
    }

    const promise = run()
    if (key) {
      inFlight.set(key, promise)
      promise.finally(() => inFlight.delete(key))
    }
    return promise
  }

  return {
    get: <T>(url: string, config?: RequestOptions) => request<T>('get', url, undefined, config),
    post: <T>(url: string, data?: unknown, config?: RequestOptions) => request<T>('post', url, data, config),
    put: <T>(url: string, data?: unknown, config?: RequestOptions) => request<T>('put', url, data, config),
    delete: <T>(url: string, config?: RequestOptions) => request<T>('delete', url, undefined, config),
  }
}
```

- [ ] **Step 4: Refactor api.ts without breaking existing exports**

In `frontend/src/services/api.ts`, keep the Axios instance export for compatibility, but create a wrapper export:

```ts
export const apiClient = createHttpClient(http, {
  maxGetRetries: 2,
  onUnauthorized: intendedPath => {
    localStorage.removeItem('canvas_token')
    localStorage.removeItem('canvas_user')
    window.dispatchEvent(new CustomEvent('canvas:unauthorized', { detail: { intendedPath } }))
  },
})
```

Replace hard reload handling in the interceptor with the same event dispatch. App shell or auth guard can translate `canvas:unauthorized` to `navigate('/login', { state: { from: intendedPath } })`.

- [ ] **Step 5: Run HTTP and existing API tests**

Run:

```bash
cd frontend && npm test -- httpClient.test.ts api.test.ts
```

Expected: PASS.

### Task 2: Runtime Canvas Schemas

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/types/canvasSchemas.test.ts`
- Create: `frontend/src/types/canvasSchemas.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Add Zod dependency**

Run:

```bash
cd frontend && npm install zod
```

Expected: `package.json` and `package-lock.json` include `zod`.

- [ ] **Step 2: Write schema tests**

Create `frontend/src/types/canvasSchemas.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { canvasGraphSchema, parseCanvasGraph } from './canvasSchemas'

describe('canvasSchemas', () => {
  it('accepts a valid graph with node config and outlet schema', () => {
    const graph = parseCanvasGraph({
      nodes: [{ id: 'start', type: 'START', name: '开始', x: 0, y: 0, config: {}, outletSchema: '[]' }],
      edges: [],
    })

    expect(graph.nodes[0].id).toBe('start')
  })

  it('rejects invalid graph roots and nodes without id or type', () => {
    expect(() => parseCanvasGraph({})).toThrow('Invalid canvas graph')
    expect(() => parseCanvasGraph({ nodes: [{ id: 'n1' }], edges: [] })).toThrow('Invalid canvas graph')
  })

  it('rejects strict outlet target typos', () => {
    expect(() => canvasGraphSchema.parse({
      nodes: [{ id: 'wait', type: 'WAIT', name: '等待', x: 0, y: 0, config: {}, outletSchema: JSON.stringify([{ id: 'success', targetField: 'next_node_id' }]) }],
      edges: [],
    })).toThrow()
  })
})
```

- [ ] **Step 3: Run schema tests and confirm red state**

Run:

```bash
cd frontend && npm test -- canvasSchemas.test.ts
```

Expected: FAIL because `canvasSchemas.ts` does not exist.

- [ ] **Step 4: Implement canvas schemas**

Create `frontend/src/types/canvasSchemas.ts`:

```ts
import { z } from 'zod'

const outletTargetFieldSchema = z.enum(['nextNodeId', 'successNodeId', 'failNodeId', 'hitNextNodeId', 'missNextNodeId', 'timeoutNodeId'])

const outletSchemaString = z.string().superRefine((value, ctx) => {
  try {
    const parsed = JSON.parse(value)
    z.array(z.object({
      id: z.string().min(1),
      label: z.string().optional(),
      targetField: outletTargetFieldSchema.optional(),
    })).parse(parsed)
  } catch {
    ctx.addIssue({ code: 'custom', message: 'Invalid outlet schema' })
  }
}).optional()

export const canvasNodeSchema = z.object({
  id: z.string().min(1),
  type: z.string().min(1),
  name: z.string().min(1),
  category: z.string().optional(),
  x: z.number(),
  y: z.number(),
  config: z.record(z.string(), z.unknown()).default({}),
  bizConfig: z.record(z.string(), z.unknown()).optional(),
  outletSchema: outletSchemaString,
})

export const canvasGraphSchema = z.object({
  nodes: z.array(canvasNodeSchema),
  edges: z.array(z.unknown()).default([]),
})

export type CanvasGraph = z.infer<typeof canvasGraphSchema>

export function parseCanvasGraph(value: unknown): CanvasGraph {
  const result = canvasGraphSchema.safeParse(value)
  if (!result.success) {
    throw new Error('Invalid canvas graph: ' + result.error.issues.map(issue => issue.message).join(', '))
  }
  return result.data
}
```

- [ ] **Step 5: Validate graph hydration path**

In `canvas-editor/index.tsx`, parse backend graph JSON before mapping to React Flow nodes:

```ts
const rawGraph = JSON.parse(detail.graphJson || '{"nodes":[],"edges":[]}')
const graph = parseCanvasGraph(rawGraph)
```

Show the existing editor error state when parsing throws instead of passing invalid data into React Flow.

- [ ] **Step 6: Run schema and editor hydration tests**

Run:

```bash
cd frontend && npm test -- canvasSchemas.test.ts graphHydration.test.ts
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/services/httpClient.ts`
- Create: `frontend/src/types/canvasSchemas.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `docs/product-evolution/specs/p1-007c-frontend-http-client-and-runtime-schemas.md`
- Modify: `docs/product-evolution/plans/p1-007c-frontend-http-client-and-runtime-schemas-plan.md`

- [ ] **Step 1: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- httpClient.test.ts api.test.ts canvasSchemas.test.ts graphHydration.test.ts
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/services frontend/src/types frontend/src/pages/canvas-editor docs/product-evolution/specs/p1-007c-frontend-http-client-and-runtime-schemas.md docs/product-evolution/plans/p1-007c-frontend-http-client-and-runtime-schemas-plan.md
git commit -m "refactor: add frontend http client and runtime schemas"
```

Expected: commit contains only HTTP client, runtime schema, editor validation, tests, package metadata, and related docs.
