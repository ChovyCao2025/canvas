# OpenAPI Runtime API Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/api-docs` auto-register endpoints by reading `/v3/api-docs` at runtime while keeping the current searchable Chinese docs UI.

**Architecture:** Keep `apiDocs.ts` as the shared model and filtering module. Add an OpenAPI adapter that converts `paths` operations to `ApiDocEndpoint[]`, then merge a small curated override table for important Chinese titles, summaries, examples, and explicit internal flags. Update the page to load the adapter asynchronously and render loading/error states.

**Tech Stack:** React 18, TypeScript, Vite, Vitest, Ant Design, browser `fetch`, Springdoc OpenAPI `/v3/api-docs`.

---

## File Structure

- Modify: `frontend/src/pages/api-docs/apiDocs.ts`
  - Keep API docs types, category constants, JSON formatting, endpoint ID helpers, and filtering helpers.
  - First add a transitional filter overload so the old page can keep compiling.
  - After the page reads OpenAPI, remove the static `API_DOCS` list and the old filter overload.

- Create: `frontend/src/pages/api-docs/apiDocOverrides.ts`
  - Store curated endpoint metadata keyed by `METHOD path`.
  - Export reusable `success()` and `page()` example helpers.

- Create: `frontend/src/pages/api-docs/openApiDocs.ts`
  - Define minimal OpenAPI types.
  - Fetch `/v3/api-docs`.
  - Convert OpenAPI operations to `ApiDocEndpoint[]`.
  - Classify endpoint categories and internal/external visibility.
  - Merge overrides.

- Modify: `frontend/src/pages/api-docs/index.tsx`
  - Load OpenAPI endpoints on mount.
  - Render loading, retryable error, warning count, and normal endpoint states.
  - Keep existing card, search, category, and internal toggle UI.

- Modify: `frontend/src/pages/api-docs/apiDocs.test.ts`
  - Update helper tests to use local fixtures instead of static `API_DOCS`.

- Create: `frontend/src/pages/api-docs/openApiDocs.test.ts`
  - Unit test OpenAPI parsing, classification, overrides, internal visibility, and malformed specs.

- Modify: `frontend/vite.config.ts`
  - Add `/v3` proxy to the backend for local development.

## Task 1: Make API Docs Helpers Data-Source Agnostic

**Files:**
- Modify: `frontend/src/pages/api-docs/apiDocs.ts`
- Modify: `frontend/src/pages/api-docs/apiDocs.test.ts`

- [ ] **Step 1: Replace static-data helper tests with fixture-based tests**

Update `frontend/src/pages/api-docs/apiDocs.test.ts` to remove `API_DOCS` imports and use an explicit fixture:

```ts
import { describe, expect, it } from 'vitest'

import {
  API_DOC_CATEGORIES,
  endpointId,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'
import type { ApiDocEndpoint } from './apiDocs'

const endpoints: ApiDocEndpoint[] = [
  {
    id: endpointId('POST', '/canvas/events/report'),
    title: '上报业务事件',
    method: 'POST',
    path: '/canvas/events/report',
    category: 'external-trigger',
    summary: '业务系统上报事件编码和用户属性。',
    auth: 'bearer',
    responseExample: { code: 0, message: 'success', data: { accepted: true } },
  },
  {
    id: endpointId('GET', '/canvas/api-definitions'),
    title: 'API 定义列表',
    method: 'GET',
    path: '/canvas/api-definitions',
    category: 'configuration',
    summary: '分页查询后台 API 配置。',
    auth: 'bearer',
    internal: true,
    responseExample: { code: 0, message: 'success', data: { total: 0, list: [] } },
  },
  {
    id: endpointId('GET', '/admin/users'),
    title: '用户列表',
    method: 'GET',
    path: '/admin/users',
    category: 'users',
    summary: '分页查询后台用户。',
    auth: 'bearer',
    internal: true,
    responseExample: { code: 0, message: 'success', data: [] },
  },
]

describe('api docs data helpers', () => {
  it('hides internal endpoints by default', () => {
    const visible = filterApiDocEndpoints(endpoints, { showInternal: false })

    expect(visible.every(endpoint => endpoint.internal !== true)).toBe(true)
    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/events/report')
    expect(visible.map(endpoint => endpoint.path)).not.toContain('/canvas/api-definitions')
  })

  it('reveals internal endpoints when requested', () => {
    const visible = filterApiDocEndpoints(endpoints, { showInternal: true })

    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/api-definitions')
    expect(visible.map(endpoint => endpoint.path)).toContain('/admin/users')
  })

  it('filters by path title summary and category title', () => {
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: 'events/report' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: '用户管理' })
      .map(endpoint => endpoint.category)).toContain('users')
  })

  it('builds category summaries from visible endpoints', () => {
    const summaries = getApiDocCategorySummaries(filterApiDocEndpoints(endpoints, { showInternal: false }))

    expect(summaries[0]).toMatchObject({
      key: API_DOC_CATEGORIES[1].key,
      title: API_DOC_CATEGORIES[1].title,
    })
    expect(summaries.every(summary => summary.count > 0)).toBe(true)
  })

  it('formats JSON examples with two-space indentation', () => {
    expect(formatJsonExample({ code: 0, message: 'success', data: { ok: true } })).toBe(
      '{\n  "code": 0,\n  "message": "success",\n  "data": {\n    "ok": true\n  }\n}',
    )
  })

  it('formats undefined examples with a safe string fallback', () => {
    expect(formatJsonExample(undefined)).toBe('undefined')
  })

  it('builds stable endpoint ids', () => {
    expect(endpointId('GET', '/canvas/{id}/versions/{versionId}')).toBe('get-canvas-id-versions-versionId')
  })
})
```

- [ ] **Step 2: Run tests to confirm current helpers fail**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/apiDocs.test.ts
```

Expected: TypeScript/Vitest fails because `endpointId` is not exported and `filterApiDocEndpoints` still accepts only the filter object.

- [ ] **Step 3: Update `apiDocs.ts` with a transitional helper overload**

Modify `frontend/src/pages/api-docs/apiDocs.ts` without deleting `API_DOCS` yet. Change the existing private `endpointId` helper to an exported function:

```ts
export function endpointId(method: ApiDocMethod, path: string) {
  return `${method.toLowerCase()}-${path.replace(/[{}]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '')}`
}
```

Then replace the existing `filterApiDocEndpoints(filter: ApiDocFilter)` implementation with this overload. Keep the existing `API_DOCS` export in place so `index.tsx` still compiles before Task 4:

```ts
export function filterApiDocEndpoints(endpoints: ApiDocEndpoint[], filter: ApiDocFilter): ApiDocEndpoint[]
export function filterApiDocEndpoints(filter: ApiDocFilter): ApiDocEndpoint[]
export function filterApiDocEndpoints(
  endpointsOrFilter: ApiDocEndpoint[] | ApiDocFilter,
  maybeFilter?: ApiDocFilter,
): ApiDocEndpoint[] {
  const endpoints = Array.isArray(endpointsOrFilter) ? endpointsOrFilter : API_DOCS
  const filter = Array.isArray(endpointsOrFilter) ? maybeFilter : endpointsOrFilter

  if (!filter) {
    return endpoints
  }

  const keyword = filter.keyword?.trim().toLowerCase()

  return endpoints.filter(endpoint => {
    if (!filter.showInternal && endpoint.internal) {
      return false
    }

    if (filter.category && endpoint.category !== filter.category) {
      return false
    }

    if (!keyword) {
      return true
    }

    const category = categoriesByKey.get(endpoint.category)
    return [
      endpoint.title,
      endpoint.path,
      endpoint.method,
      endpoint.summary,
      category?.title,
      category?.description,
    ].some(value => value?.toLowerCase().includes(keyword))
  })
}
```

Do not change `getApiDocCategorySummaries()` or `formatJsonExample()` in this task.

- [ ] **Step 4: Run helper tests**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/apiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit helper refactor**

Run:

```bash
git add frontend/src/pages/api-docs/apiDocs.ts frontend/src/pages/api-docs/apiDocs.test.ts
git commit -m "refactor: make api docs helpers data-source agnostic"
```

Only stage these two files. Do not stage unrelated dirty worktree changes.

## Task 2: Add Curated API Doc Overrides

**Files:**
- Create: `frontend/src/pages/api-docs/apiDocOverrides.ts`
- Test through: `frontend/src/pages/api-docs/openApiDocs.test.ts` in Task 3

- [ ] **Step 1: Create the override module**

Create `frontend/src/pages/api-docs/apiDocOverrides.ts`:

```ts
import type { ApiDocAuth, ApiDocMethod, ApiDocParam } from './apiDocs'

export interface ApiDocOverride {
  title?: string
  category?: string
  summary?: string
  auth?: ApiDocAuth
  internal?: boolean
  params?: ApiDocParam[]
  requestExample?: unknown
  responseExample?: unknown
}

export const success = (data: unknown) => ({ code: 0, message: 'success', data })
export const page = (list: unknown[]) => success({ total: list.length, list })

export const bodyParam = (name: string, desc: string): ApiDocParam => ({
  name,
  in: 'body',
  required: true,
  desc,
})

export const pathParam = (name: string, desc: string): ApiDocParam => ({
  name,
  in: 'path',
  required: true,
  desc,
})

export const overrideKey = (method: ApiDocMethod, path: string) => `${method} ${path}`

export const API_DOC_OVERRIDES: Record<string, ApiDocOverride> = {
  [overrideKey('POST', '/auth/login')]: {
    title: '账号登录',
    category: 'auth',
    summary: '使用账号密码登录后台并换取 Bearer 访问令牌。',
    auth: 'none',
    internal: false,
    params: [bodyParam('username', '登录账号'), bodyParam('password', '登录密码')],
    requestExample: { username: 'admin@example.com', password: 'password' },
    responseExample: success({ token: 'eyJhbGciOi...', userId: 1, username: 'admin', displayName: '管理员', role: 'ADMIN' }),
  },
  [overrideKey('POST', '/auth/logout')]: {
    title: '退出登录',
    category: 'auth',
    summary: '注销当前登录会话。',
    internal: false,
    responseExample: success(null),
  },
  [overrideKey('GET', '/auth/me')]: {
    title: '当前用户',
    category: 'auth',
    summary: '读取当前 Bearer 令牌对应的用户身份和权限。',
    internal: false,
    responseExample: success({ userId: 1, username: 'admin', displayName: '管理员', role: 'ADMIN' }),
  },
  [overrideKey('POST', '/canvas/events/report')]: {
    title: '上报业务事件',
    category: 'external-trigger',
    summary: '业务系统上报事件编码和用户属性，触发匹配画布执行。',
    internal: false,
    params: [bodyParam('eventCode', '事件编码'), bodyParam('userId', '业务用户 ID')],
    requestExample: { eventCode: 'ORDER_PAID', userId: 'user_10001', attributes: { orderId: 'ord_202605230001', amount: 199 } },
    responseExample: success({ accepted: true, executionId: 'exec_202605230001' }),
  },
  [overrideKey('POST', '/canvas/trigger/behavior')]: {
    title: '触发行为策略',
    category: 'external-trigger',
    summary: '按行为策略类型提交业务行为，触发已发布画布。',
    internal: false,
    params: [bodyParam('strategyType', '行为策略类型'), bodyParam('userId', '业务用户 ID')],
    requestExample: { strategyType: 'RETENTION_COUPON', userId: 'user_10001', payload: { scene: 'checkout' } },
    responseExample: success({ accepted: true, matchedCanvasIds: [42] }),
  },
  [overrideKey('POST', '/canvas/execute/direct/{canvasId}')]: {
    title: '直接执行画布',
    category: 'external-trigger',
    summary: '按画布 ID 直接发起一次线上执行，适合业务系统主动调用。',
    internal: false,
    params: [pathParam('canvasId', '画布 ID'), bodyParam('userId', '业务用户 ID'), bodyParam('idempotencyKey', '幂等键'), bodyParam('inputParams', '输入参数')],
    requestExample: { userId: 'user_10001', idempotencyKey: 'idem_202605230001', inputParams: { source: 'crm', couponType: 'WELCOME' } },
    responseExample: success({ executionId: 'exec_202605230002', status: 'RUNNING' }),
  },
  [overrideKey('POST', '/canvas/execute/dry-run/{canvasId}')]: {
    title: '试运行画布',
    category: 'external-trigger',
    summary: '使用指定输入参数模拟执行画布，不产生真实业务副作用。',
    internal: false,
    params: [pathParam('canvasId', '画布 ID'), bodyParam('userId', '业务用户 ID'), bodyParam('inputParams', '输入参数')],
    requestExample: { userId: 'user_10001', inputParams: { source: 'debug' } },
    responseExample: success({ traceId: 'trace_dry_run_001', result: 'PASSED' }),
  },
  [overrideKey('POST', '/canvas/execution/{executionId}/approve')]: {
    title: '审批通过',
    category: 'approval',
    summary: '人工审批节点回调通过结果，推动执行继续流转。',
    internal: false,
    params: [pathParam('executionId', '执行 ID'), bodyParam('comment', '审批意见')],
    requestExample: { comment: '同意发放权益' },
    responseExample: success({ executionId: 'exec_202605230001', approved: true }),
  },
  [overrideKey('POST', '/canvas/execution/{executionId}/reject')]: {
    title: '审批拒绝',
    category: 'approval',
    summary: '人工审批节点回调拒绝结果，终止或转入拒绝分支。',
    internal: false,
    params: [pathParam('executionId', '执行 ID'), bodyParam('reason', '拒绝原因')],
    requestExample: { reason: '用户不满足风控条件' },
    responseExample: success({ executionId: 'exec_202605230001', rejected: true }),
  },
  [overrideKey('GET', '/meta/node-types')]: {
    title: '节点类型列表',
    category: 'metadata',
    summary: '获取前端编排可使用的节点类型。',
    responseExample: success([{ typeKey: 'API_CALL', title: 'API 调用' }]),
  },
  [overrideKey('GET', '/meta/context-fields')]: {
    title: '上下文字段',
    category: 'metadata',
    summary: '获取外部调用和节点编排可引用的上下文字段。',
    responseExample: success([{ key: 'userId', title: '用户 ID', type: 'string' }]),
  },
  [overrideKey('GET', '/canvas/api-definitions')]: {
    title: 'API 定义列表',
    category: 'configuration',
    summary: '分页查询后台 API 配置。',
    internal: true,
    responseExample: page([{ id: 1, name: '发券接口' }]),
  },
  [overrideKey('GET', '/canvas/dlq')]: {
    title: '死信列表',
    category: 'observability',
    summary: '分页查询进入死信队列的触发消息。',
    internal: true,
    responseExample: page([{ id: 1, reason: 'handler timeout' }]),
  },
  [overrideKey('GET', '/admin/users')]: {
    title: '用户列表',
    category: 'users',
    summary: '分页查询后台用户。',
    internal: true,
    responseExample: page([{ id: 1, name: '管理员', email: 'admin@example.com' }]),
  },
}
```

- [ ] **Step 2: Commit override module**

Run:

```bash
git add frontend/src/pages/api-docs/apiDocOverrides.ts
git commit -m "feat: add curated api docs overrides"
```

## Task 3: Add OpenAPI Runtime Adapter

**Files:**
- Create: `frontend/src/pages/api-docs/openApiDocs.ts`
- Create: `frontend/src/pages/api-docs/openApiDocs.test.ts`

- [ ] **Step 1: Write adapter tests**

Create `frontend/src/pages/api-docs/openApiDocs.test.ts`:

```ts
import { describe, expect, it } from 'vitest'

import {
  classifyOpenApiPath,
  inferOpenApiInternalFlag,
  parseOpenApiEndpoints,
} from './openApiDocs'
import { overrideKey } from './apiDocOverrides'

const spec = {
  openapi: '3.0.1',
  paths: {
    '/canvas/events/report': {
      post: {
        summary: 'report event',
        parameters: [{ name: 'source', in: 'query', required: false, description: '来源' }],
        requestBody: { required: true, content: { 'application/json': { schema: { type: 'object' } } } },
        responses: { '200': { description: 'OK' } },
      },
    },
    '/canvas/{id}/versions/{versionId}': {
      get: {
        parameters: [
          { name: 'id', in: 'path', required: true, description: '画布 ID' },
          { name: 'versionId', in: 'path', required: true },
        ],
        responses: { '200': { description: 'OK' } },
      },
    },
    '/admin/users': {
      get: { responses: { '200': { description: 'OK' } } },
    },
    '/canvas/mq-trigger-rejected/{id}': {
      get: { responses: { '200': { description: 'OK' } } },
    },
    '/canvas/data-sources': {
      put: {
        parameters: [{ name: 'keyword', in: 'query', required: false, description: '关键字' }],
        requestBody: { required: true, content: { 'application/json': { schema: { type: 'object' } } } },
        responses: { '200': { description: 'OK' } },
      },
      delete: { responses: { '200': { description: 'OK' } } },
      trace: { responses: { '200': { description: 'ignored' } } },
    },
  },
}

describe('open api docs adapter', () => {
  it('parses supported OpenAPI operations into endpoints', () => {
    const result = parseOpenApiEndpoints(spec)

    expect(result.warnings).toEqual([])
    expect(result.endpoints.map(endpoint => `${endpoint.method} ${endpoint.path}`)).toEqual([
      'GET /admin/users',
      'PUT /canvas/data-sources',
      'DELETE /canvas/data-sources',
      'POST /canvas/events/report',
      'GET /canvas/mq-trigger-rejected/{id}',
      'GET /canvas/{id}/versions/{versionId}',
    ])
  })

  it('extracts query path and body params', () => {
    const dataSources = parseOpenApiEndpoints(spec).endpoints.find(endpoint => endpoint.path === '/canvas/data-sources' && endpoint.method === 'PUT')
    const version = parseOpenApiEndpoints(spec).endpoints.find(endpoint => endpoint.path === '/canvas/{id}/versions/{versionId}')

    expect(dataSources?.params).toEqual([
      { name: 'keyword', in: 'query', required: false, desc: '关键字' },
      { name: 'body', in: 'body', required: true, desc: '请求体' },
    ])
    expect(version?.params).toEqual([
      { name: 'id', in: 'path', required: true, desc: '画布 ID' },
      { name: 'versionId', in: 'path', required: true, desc: 'versionId 路径参数' },
    ])
  })

  it('applies curated overrides', () => {
    const report = parseOpenApiEndpoints(spec).endpoints.find(endpoint => endpoint.path === '/canvas/events/report')

    expect(report).toMatchObject({
      title: '上报业务事件',
      category: 'external-trigger',
      internal: false,
    })
    expect(report?.requestExample).toEqual({
      eventCode: 'ORDER_PAID',
      userId: 'user_10001',
      attributes: { orderId: 'ord_202605230001', amount: 199 },
    })
  })

  it('classifies known path families', () => {
    expect(classifyOpenApiPath('/auth/login')).toBe('auth')
    expect(classifyOpenApiPath('/canvas/execution/{executionId}/approve')).toBe('approval')
    expect(classifyOpenApiPath('/canvas/mq-trigger-rejected')).toBe('observability')
    expect(classifyOpenApiPath('/canvas/templates')).toBe('operations')
    expect(classifyOpenApiPath('/admin/users')).toBe('users')
    expect(classifyOpenApiPath('/meta/options')).toBe('metadata')
    expect(classifyOpenApiPath('/canvas/list')).toBe('canvas')
  })

  it('infers conservative internal flags', () => {
    expect(inferOpenApiInternalFlag('POST', '/auth/login')).toBe(false)
    expect(inferOpenApiInternalFlag('GET', '/auth/me')).toBe(false)
    expect(inferOpenApiInternalFlag('POST', '/canvas/events/report')).toBe(false)
    expect(inferOpenApiInternalFlag('GET', '/meta/options')).toBe(true)
    expect(inferOpenApiInternalFlag('GET', '/canvas/api-definitions')).toBe(true)
    expect(inferOpenApiInternalFlag('GET', '/cdp/users')).toBe(true)
  })

  it('supports explicit override keys', () => {
    expect(overrideKey('GET', '/admin/users')).toBe('GET /admin/users')
  })

  it('returns a warning for missing paths without crashing', () => {
    const result = parseOpenApiEndpoints({ openapi: '3.0.1' })

    expect(result.endpoints).toEqual([])
    expect(result.warnings).toEqual(['OpenAPI document does not contain a paths object.'])
  })
})
```

- [ ] **Step 2: Run adapter tests to confirm they fail**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/openApiDocs.test.ts
```

Expected: FAIL because `openApiDocs.ts` does not exist.

- [ ] **Step 3: Implement OpenAPI adapter**

Create `frontend/src/pages/api-docs/openApiDocs.ts`:

```ts
import { API_DOC_OVERRIDES, overrideKey, success } from './apiDocOverrides'
import { endpointId, type ApiDocEndpoint, type ApiDocMethod, type ApiDocParam } from './apiDocs'

type OpenApiParameterLocation = 'path' | 'query' | 'header' | 'cookie'

interface OpenApiParameter {
  name?: string
  in?: OpenApiParameterLocation
  required?: boolean
  description?: string
}

interface OpenApiRequestBody {
  required?: boolean
  description?: string
}

interface OpenApiOperation {
  summary?: string
  description?: string
  operationId?: string
  parameters?: OpenApiParameter[]
  requestBody?: OpenApiRequestBody
  responses?: Record<string, unknown>
}

interface OpenApiPathItem {
  get?: OpenApiOperation
  post?: OpenApiOperation
  put?: OpenApiOperation
  delete?: OpenApiOperation
}

export interface OpenApiDocument {
  openapi?: string
  paths?: Record<string, OpenApiPathItem | undefined>
}

export interface ParsedOpenApiEndpoints {
  endpoints: ApiDocEndpoint[]
  warnings: string[]
}

const SUPPORTED_METHODS: ApiDocMethod[] = ['GET', 'POST', 'PUT', 'DELETE']

export async function fetchOpenApiSpec(fetcher: typeof fetch = fetch): Promise<OpenApiDocument> {
  const response = await fetcher('/v3/api-docs', {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new Error(`GET /v3/api-docs failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<OpenApiDocument>
}

export function parseOpenApiEndpoints(spec: OpenApiDocument): ParsedOpenApiEndpoints {
  if (!spec.paths || typeof spec.paths !== 'object') {
    return { endpoints: [], warnings: ['OpenAPI document does not contain a paths object.'] }
  }

  const warnings: string[] = []
  const endpoints: ApiDocEndpoint[] = []

  Object.entries(spec.paths).forEach(([path, pathItem]) => {
    if (!pathItem || typeof pathItem !== 'object') {
      warnings.push(`Skipped malformed path item: ${path}`)
      return
    }

    SUPPORTED_METHODS.forEach(method => {
      const operation = pathItem[method.toLowerCase() as Lowercase<ApiDocMethod>]
      if (!operation) {
        return
      }

      endpoints.push(toEndpoint(method, path, operation))
    })
  })

  endpoints.sort((left, right) => {
    const pathCompare = left.path.localeCompare(right.path)
    if (pathCompare !== 0) return pathCompare
    return SUPPORTED_METHODS.indexOf(left.method) - SUPPORTED_METHODS.indexOf(right.method)
  })

  return { endpoints, warnings }
}

function toEndpoint(method: ApiDocMethod, path: string, operation: OpenApiOperation): ApiDocEndpoint {
  const override = API_DOC_OVERRIDES[overrideKey(method, path)]
  const category = override?.category ?? classifyOpenApiPath(path)
  const title = override?.title ?? operation.summary ?? buildFallbackTitle(method, path)
  const summary = override?.summary ?? operation.description ?? operation.summary ?? 'OpenAPI 自动发现的接口。'
  const params = override?.params ?? extractParams(path, operation)

  return {
    id: endpointId(method, path),
    title,
    method,
    path,
    category,
    summary,
    auth: override?.auth ?? (path === '/auth/login' ? 'none' : 'bearer'),
    internal: override?.internal ?? inferOpenApiInternalFlag(method, path),
    params,
    requestExample: override?.requestExample,
    responseExample: override?.responseExample ?? success({ id: 'demo_id', status: 'ok' }),
  }
}

function extractParams(path: string, operation: OpenApiOperation): ApiDocParam[] {
  const params: ApiDocParam[] = []
  const seen = new Set<string>()

  operation.parameters?.forEach(param => {
    if (!param.name || (param.in !== 'path' && param.in !== 'query')) {
      return
    }

    const key = `${param.in}-${param.name}`
    seen.add(key)
    params.push({
      name: param.name,
      in: param.in,
      required: param.required,
      desc: param.description || (param.in === 'path' ? `${param.name} 路径参数` : `${param.name} 查询参数`),
    })
  })

  Array.from(path.matchAll(/\{([^}]+)\}/g)).forEach(match => {
    const name = match[1]
    const key = `path-${name}`
    if (seen.has(key)) {
      return
    }

    params.push({
      name,
      in: 'path',
      required: true,
      desc: `${name} 路径参数`,
    })
  })

  if (operation.requestBody) {
    params.push({
      name: 'body',
      in: 'body',
      required: operation.requestBody.required,
      desc: operation.requestBody.description || '请求体',
    })
  }

  return params
}

export function classifyOpenApiPath(path: string): string {
  if (path.startsWith('/auth')) return 'auth'
  if (path === '/canvas/events/report' || path === '/canvas/trigger/behavior' || path.startsWith('/canvas/execute/')) return 'external-trigger'
  if (path.startsWith('/canvas/execution/') && (path.endsWith('/approve') || path.endsWith('/reject'))) return 'approval'
  if (
    path.includes('/execution/') ||
    path.includes('/executions') ||
    path.includes('/stats') ||
    path.includes('/funnel') ||
    path.includes('/trend') ||
    path.startsWith('/canvas/dlq') ||
    path.startsWith('/canvas/execution-requests') ||
    path.startsWith('/canvas/mq-trigger-rejected')
  ) return 'observability'
  if (
    path.startsWith('/ops') ||
    path.startsWith('/canvas/templates') ||
    path.startsWith('/canvas/pending-reviews') ||
    path.startsWith('/canvas/async-tasks') ||
    path.startsWith('/canvas/notifications')
  ) return 'operations'
  if (path.startsWith('/admin/users')) return 'users'
  if (
    path.startsWith('/admin/system-options') ||
    path.includes('-definitions') ||
    path.startsWith('/canvas/audiences') ||
    path.startsWith('/canvas/data-sources') ||
    path.startsWith('/canvas/identity-types') ||
    path.startsWith('/canvas/tag-imports') ||
    path.startsWith('/canvas/tag-import-sources') ||
    path.startsWith('/cdp')
  ) return 'configuration'
  if (path.startsWith('/meta')) return 'metadata'
  return 'canvas'
}

export function inferOpenApiInternalFlag(method: ApiDocMethod, path: string): boolean {
  if (
    path.startsWith('/auth') ||
    path === '/canvas/events/report' ||
    path === '/canvas/trigger/behavior' ||
    path.startsWith('/canvas/execute/') ||
    (path.startsWith('/canvas/execution/') && method === 'POST' && (path.endsWith('/approve') || path.endsWith('/reject')))
  ) {
    return false
  }

  if (
    path.startsWith('/admin') ||
    path.startsWith('/meta') ||
    path.startsWith('/cdp') ||
    path.startsWith('/ops') ||
    path.includes('-definitions') ||
    path.startsWith('/canvas/audiences') ||
    path.startsWith('/canvas/data-sources') ||
    path.startsWith('/canvas/identity-types') ||
    path.startsWith('/canvas/tag-imports') ||
    path.startsWith('/canvas/tag-import-sources') ||
    path.startsWith('/canvas/notifications') ||
    path.startsWith('/canvas/async-tasks') ||
    path.startsWith('/canvas/mq-trigger-rejected')
  ) {
    return true
  }

  return true
}

function buildFallbackTitle(method: ApiDocMethod, path: string) {
  return `${method} ${path}`
}
```

- [ ] **Step 4: Run adapter tests**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/openApiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 5: Run all API docs unit tests**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/apiDocs.test.ts src/pages/api-docs/openApiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit adapter**

Run:

```bash
git add frontend/src/pages/api-docs/openApiDocs.ts frontend/src/pages/api-docs/openApiDocs.test.ts
git commit -m "feat: parse OpenAPI for api docs"
```

## Task 4: Connect API Docs Page to Runtime OpenAPI

**Files:**
- Modify: `frontend/src/pages/api-docs/index.tsx`
- Modify: `frontend/src/pages/api-docs/apiDocs.ts`

- [ ] **Step 1: Update page imports**

In `frontend/src/pages/api-docs/index.tsx`, replace the existing API docs imports with:

```ts
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Empty, Input, Space, Spin, Switch, Table, Tag, Tooltip, Typography, message } from 'antd'
import { CopyOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'
import { fetchOpenApiSpec, parseOpenApiEndpoints } from './openApiDocs'
import type { ApiDocEndpoint, ApiDocParam } from './apiDocs'
```

- [ ] **Step 2: Replace static endpoint state with runtime loading state**

Inside `ApiDocsPage`, replace current state and endpoint calculations with:

```ts
  const [keyword, setKeyword] = useState('')
  const [showInternal, setShowInternal] = useState(false)
  const [category, setCategory] = useState<string | undefined>()
  const [apiEndpoints, setApiEndpoints] = useState<ApiDocEndpoint[]>([])
  const [warnings, setWarnings] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | undefined>()

  const loadOpenApiDocs = useCallback(async () => {
    setLoading(true)
    setError(undefined)
    try {
      const spec = await fetchOpenApiSpec()
      const parsed = parseOpenApiEndpoints(spec)
      setApiEndpoints(parsed.endpoints)
      setWarnings(parsed.warnings)
    } catch (err) {
      setApiEndpoints([])
      setWarnings([])
      setError(err instanceof Error ? err.message : '加载 /v3/api-docs 失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadOpenApiDocs()
  }, [loadOpenApiDocs])

  const categorySourceEndpoints = useMemo(() => filterApiDocEndpoints(apiEndpoints, {
    showInternal,
    keyword,
  }), [apiEndpoints, keyword, showInternal])

  const categorySummaries = useMemo(
    () => getApiDocCategorySummaries(categorySourceEndpoints),
    [categorySourceEndpoints],
  )

  const selectedCategory = categorySummaries.some(summary => summary.key === category) ? category : undefined

  useEffect(() => {
    if (category && !selectedCategory) {
      setCategory(undefined)
    }
  }, [category, selectedCategory])

  const endpoints = useMemo(() => filterApiDocEndpoints(apiEndpoints, {
    showInternal,
    keyword,
    category: selectedCategory,
  }), [apiEndpoints, keyword, selectedCategory, showInternal])

  const totalVisibleCount = categorySourceEndpoints.length
  const publicCount = apiEndpoints.filter(endpoint => !endpoint.internal).length
  const internalCount = apiEndpoints.length - publicCount
```

- [ ] **Step 3: Add loading, error, and warning UI**

After the intro paragraph and before the main two-column layout, insert:

```tsx
        {loading ? (
          <Card size="small" style={{ borderRadius: 8 }}>
            <Space>
              <Spin size="small" />
              <Text type="secondary">正在加载 /v3/api-docs...</Text>
            </Space>
          </Card>
        ) : null}

        {error ? (
          <Alert
            type="error"
            showIcon
            message="OpenAPI 文档加载失败"
            description={`请求 /v3/api-docs 未成功：${error}`}
            action={<Button size="small" icon={<ReloadOutlined />} onClick={loadOpenApiDocs}>重试</Button>}
          />
        ) : null}

        {!error && warnings.length > 0 ? (
          <Alert
            type="warning"
            showIcon
            message={`OpenAPI 解析跳过 ${warnings.length} 项`}
            description={warnings.slice(0, 3).join('；')}
          />
        ) : null}
```

- [ ] **Step 4: Disable filter controls while loading or failed**

Add `disabled={loading || Boolean(error)}` to the search `Input` and `Switch`. Also disable category buttons when `loading || Boolean(error)`:

```tsx
                disabled={loading || Boolean(error)}
```

for the `Input`, the `Switch`, the "全部 API" button, and each category button.

- [ ] **Step 5: Keep empty state precise**

Change the empty-state block to:

```tsx
              {!loading && !error && endpoints.length > 0 ? (
                endpoints.map(endpoint => <EndpointCard key={endpoint.id} endpoint={endpoint} />)
              ) : null}

              {!loading && !error && endpoints.length === 0 ? (
                <Card size="small" style={{ borderRadius: 8 }}>
                  <Empty description="没有匹配的 API" />
                </Card>
              ) : null}
```

- [ ] **Step 6: Remove the static endpoint list and old filter overload**

After `index.tsx` no longer imports `API_DOCS`, modify `frontend/src/pages/api-docs/apiDocs.ts`:

- Delete `EndpointInput`.
- Delete local example helpers that only existed for static data: `success`, `page`, `idParam`, `queryParam`, `bodyParam`, `pathParams`, and `endpoint`.
- Delete `export const API_DOCS: ApiDocEndpoint[] = [...]`.
- Replace the transitional overload with the final two-argument helper:

```ts
export function filterApiDocEndpoints(endpoints: ApiDocEndpoint[], filter: ApiDocFilter): ApiDocEndpoint[] {
  const keyword = filter.keyword?.trim().toLowerCase()

  return endpoints.filter(endpoint => {
    if (!filter.showInternal && endpoint.internal) {
      return false
    }

    if (filter.category && endpoint.category !== filter.category) {
      return false
    }

    if (!keyword) {
      return true
    }

    const category = categoriesByKey.get(endpoint.category)
    return [
      endpoint.title,
      endpoint.path,
      endpoint.method,
      endpoint.summary,
      category?.title,
      category?.description,
    ].some(value => value?.toLowerCase().includes(keyword))
  })
}
```

- [ ] **Step 7: Run TypeScript build for page integration**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS. If it fails because an import is unused, remove only the unused import introduced in this task and rerun.

- [ ] **Step 8: Commit page runtime loading and static-list removal**

Run:

```bash
git add frontend/src/pages/api-docs/index.tsx frontend/src/pages/api-docs/apiDocs.ts
git commit -m "feat: load api docs from OpenAPI"
```

## Task 5: Add Vite Dev Proxy for OpenAPI

**Files:**
- Modify: `frontend/vite.config.ts`

- [ ] **Step 1: Add failing verification by inspecting current proxy**

Run:

```bash
rg -n "'/v3'|\"/v3\"" frontend/vite.config.ts
```

Expected: no output.

- [ ] **Step 2: Add `/v3` proxy**

Modify the `proxy` object in `frontend/vite.config.ts`:

```ts
      '/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin': { target: 'http://localhost:8080', changeOrigin: true },
      '/meta':  { target: 'http://localhost:8080', changeOrigin: true },
      '/v3':    { target: 'http://localhost:8080', changeOrigin: true },
      '/canvas': {
```

- [ ] **Step 3: Verify proxy entry exists**

Run:

```bash
rg -n "'/v3'" frontend/vite.config.ts
```

Expected output includes:

```text
frontend/vite.config.ts:13:      '/v3':    { target: 'http://localhost:8080', changeOrigin: true },
```

Line number can differ if the file has changed.

- [ ] **Step 4: Commit proxy change**

Run:

```bash
git add frontend/vite.config.ts
git commit -m "chore: proxy OpenAPI docs in vite"
```

## Task 6: Final Verification

**Files:**
- Verify only; no planned edits.

- [ ] **Step 1: Run API docs tests**

Run:

```bash
cd frontend
npm test -- --run src/pages/api-docs/apiDocs.test.ts src/pages/api-docs/openApiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 2: Run full frontend test suite**

Run:

```bash
cd frontend
npm test
```

Expected: PASS. If unrelated pre-existing tests fail, capture the exact failing test names and errors before deciding whether they are in scope.

- [ ] **Step 3: Run production build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 4: Check implementation diff scope**

Run:

```bash
git diff --stat HEAD~5..HEAD
```

Expected: changes are limited to:

```text
frontend/src/pages/api-docs/apiDocs.ts
frontend/src/pages/api-docs/apiDocs.test.ts
frontend/src/pages/api-docs/apiDocOverrides.ts
frontend/src/pages/api-docs/openApiDocs.ts
frontend/src/pages/api-docs/openApiDocs.test.ts
frontend/src/pages/api-docs/index.tsx
frontend/vite.config.ts
```

- [ ] **Step 5: Confirm runtime behavior manually if backend is running**

Run:

```bash
curl -s http://localhost:8080/v3/api-docs | head -c 80
```

Expected when backend is running:

```text
{"openapi":
```

Then run:

```bash
cd frontend
npm run dev
```

Open `http://localhost:3000/api-docs` and confirm the page shows OpenAPI-loaded endpoints. Stop the dev server after checking.

If backend is not running, record that manual runtime verification was not performed and rely on unit tests plus build.

- [ ] **Step 6: Final commit if any verification-only fix was required**

If Step 2 or Step 3 required a small in-scope fix, commit it:

```bash
git add frontend/src/pages/api-docs frontend/vite.config.ts
git commit -m "fix: stabilize OpenAPI api docs"
```

Skip this commit if no files changed after Task 5.

## Self-Review Notes

- Spec coverage: runtime `/v3/api-docs` fetch is implemented in Task 3 and connected in Task 4; local overrides are Task 2; loading/error states are Task 4; dev proxy is Task 5; tests are Tasks 1, 3, and 6.
- No backend catalog endpoint, Swagger UI embed, controller annotation sweep, or online request execution is included.
- The plan intentionally does not stage unrelated dirty worktree files. Each commit command stages only files touched by that task.
