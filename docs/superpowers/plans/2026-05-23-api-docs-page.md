# API Docs Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a developer-style API documentation page that defaults to external APIs and can reveal internal management APIs with a switch.

**Architecture:** Store API documentation as typed static frontend data, keep filtering and category logic in pure helpers, and render a React/Ant Design page from that data. Add a new `/api-docs` admin route and a 「开发者文档」 menu group so the page is separate from 「API 接口配置」.

**Tech Stack:** React 18, TypeScript, Vite, Vitest, Ant Design 5, React Router 6.

---

## File Structure

- Create `frontend/src/pages/api-docs/apiDocs.ts`
  - Owns `ApiDocEndpoint`, `ApiDocCategory`, static endpoint data, filtering, category summaries, and JSON formatting.
- Create `frontend/src/pages/api-docs/apiDocs.test.ts`
  - Tests data visibility, internal switch behavior, search behavior, category counts, duplicate IDs, and JSON formatting.
- Create `frontend/src/pages/api-docs/index.tsx`
  - Renders the API documentation page using Ant Design components and helpers from `apiDocs.ts`.
- Modify `frontend/src/App.tsx`
  - Imports the page and registers `/api-docs` under `RequireAdmin`.
- Modify `frontend/src/components/layout/AppLayout.tsx`
  - Adds selected state and a new 「开发者文档」 menu group with 「API 说明」.

## Endpoint Inventory

Every row below must be present in `API_DOCS`.

| Method | Path | Category | Visibility |
|----|----|----|----|
| POST | `/auth/login` | `auth` | external |
| POST | `/auth/logout` | `auth` | external |
| GET | `/auth/me` | `auth` | external |
| POST | `/canvas/events/report` | `external-trigger` | external |
| POST | `/canvas/trigger/behavior` | `external-trigger` | external |
| POST | `/canvas/execute/direct/{canvasId}` | `external-trigger` | external |
| POST | `/canvas/execute/dry-run/{canvasId}` | `external-trigger` | external |
| POST | `/canvas/execution/{executionId}/approve` | `approval` | external |
| POST | `/canvas/execution/{executionId}/reject` | `approval` | external |
| GET | `/meta/node-types` | `metadata` | external |
| GET | `/meta/node-types/{typeKey}/schema` | `metadata` | external |
| GET | `/meta/context-fields` | `metadata` | external |
| GET | `/meta/biz-lines` | `metadata` | external |
| GET | `/meta/api-definitions` | `metadata` | external |
| GET | `/meta/event-definitions` | `metadata` | external |
| POST | `/canvas` | `canvas` | internal |
| GET | `/canvas/{id}` | `canvas` | internal |
| PUT | `/canvas/{id}` | `canvas` | internal |
| PUT | `/canvas/{id}/safe` | `canvas` | internal |
| GET | `/canvas/list` | `canvas` | internal |
| POST | `/canvas/{id}/publish` | `canvas` | internal |
| POST | `/canvas/{id}/offline` | `canvas` | internal |
| POST | `/canvas/{id}/archive` | `canvas` | internal |
| GET | `/canvas/{id}/versions` | `canvas` | internal |
| GET | `/canvas/{id}/versions/{versionId}` | `canvas` | internal |
| GET | `/canvas/{id}/versions/{v1}/diff/{v2}` | `canvas` | internal |
| POST | `/canvas/{id}/kill` | `canvas` | internal |
| POST | `/canvas/{id}/revert/{versionId}` | `canvas` | internal |
| POST | `/canvas/{id}/canary` | `canvas` | internal |
| POST | `/canvas/{id}/promote-canary` | `canvas` | internal |
| POST | `/canvas/{id}/rollback-canary` | `canvas` | internal |
| POST | `/canvas/{id}/rollback` | `canvas` | internal |
| POST | `/canvas/{id}/clone` | `canvas` | internal |
| GET | `/canvas/api-definitions` | `configuration` | internal |
| POST | `/canvas/api-definitions` | `configuration` | internal |
| PUT | `/canvas/api-definitions/{id}` | `configuration` | internal |
| DELETE | `/canvas/api-definitions/{id}` | `configuration` | internal |
| GET | `/canvas/event-definitions` | `configuration` | internal |
| POST | `/canvas/event-definitions` | `configuration` | internal |
| PUT | `/canvas/event-definitions/{id}` | `configuration` | internal |
| DELETE | `/canvas/event-definitions/{id}` | `configuration` | internal |
| GET | `/canvas/mq-definitions` | `configuration` | internal |
| POST | `/canvas/mq-definitions` | `configuration` | internal |
| PUT | `/canvas/mq-definitions/{id}` | `configuration` | internal |
| DELETE | `/canvas/mq-definitions/{id}` | `configuration` | internal |
| GET | `/canvas/tag-definitions` | `configuration` | internal |
| POST | `/canvas/tag-definitions` | `configuration` | internal |
| PUT | `/canvas/tag-definitions/{id}` | `configuration` | internal |
| DELETE | `/canvas/tag-definitions/{id}` | `configuration` | internal |
| GET | `/canvas/ab-experiments` | `configuration` | internal |
| POST | `/canvas/ab-experiments` | `configuration` | internal |
| PUT | `/canvas/ab-experiments/{id}` | `configuration` | internal |
| DELETE | `/canvas/ab-experiments/{id}` | `configuration` | internal |
| GET | `/canvas/audiences` | `configuration` | internal |
| GET | `/canvas/audiences/{id}` | `configuration` | internal |
| GET | `/canvas/audiences/ready` | `configuration` | internal |
| POST | `/canvas/audiences` | `configuration` | internal |
| PUT | `/canvas/audiences/{id}` | `configuration` | internal |
| DELETE | `/canvas/audiences/{id}` | `configuration` | internal |
| POST | `/canvas/audiences/{id}/compute` | `configuration` | internal |
| GET | `/canvas/audiences/{id}/stat` | `configuration` | internal |
| GET | `/meta/mq-topics` | `metadata` | internal |
| GET | `/meta/mq-definitions` | `metadata` | internal |
| GET | `/meta/coupon-types` | `metadata` | internal |
| GET | `/meta/reach-scenes` | `metadata` | internal |
| GET | `/meta/ab-experiments` | `metadata` | internal |
| GET | `/meta/ab-experiments/{key}/groups` | `metadata` | internal |
| GET | `/meta/tagger-tags` | `metadata` | internal |
| GET | `/meta/biz-lines/{key}/apis` | `metadata` | internal |
| GET | `/meta/behavior-strategy-types` | `metadata` | internal |
| GET | `/meta/message-codes` | `metadata` | internal |
| GET | `/meta/canvas-context-fields` | `metadata` | internal |
| GET | `/canvas/{id}/execution/{executionId}/trace` | `observability` | internal |
| GET | `/canvas/{id}/executions` | `observability` | internal |
| GET | `/canvas/{id}/stats` | `observability` | internal |
| GET | `/canvas/{id}/funnel` | `observability` | internal |
| GET | `/canvas/{id}/trend` | `observability` | internal |
| GET | `/canvas/dlq` | `observability` | internal |
| POST | `/canvas/dlq/{id}/replay` | `observability` | internal |
| DELETE | `/canvas/dlq/{id}` | `observability` | internal |
| GET | `/canvas/execution-requests` | `observability` | internal |
| POST | `/canvas/execution-requests/{id}/replay` | `observability` | internal |
| POST | `/ops/cache/invalidate/{id}` | `operations` | internal |
| GET | `/canvas/templates` | `operations` | internal |
| POST | `/canvas/{id}/save-as-template` | `operations` | internal |
| POST | `/canvas/from-template/{templateId}` | `operations` | internal |
| GET | `/canvas/pending-reviews` | `operations` | internal |
| GET | `/admin/users` | `users` | internal |
| POST | `/admin/users` | `users` | internal |
| PUT | `/admin/users/{id}` | `users` | internal |
| PUT | `/admin/users/{id}/disable` | `users` | internal |

## Task 1: API Docs Data Helpers

**Files:**
- Create: `frontend/src/pages/api-docs/apiDocs.ts`
- Create: `frontend/src/pages/api-docs/apiDocs.test.ts`

- [ ] **Step 1: Write failing helper tests**

Create `frontend/src/pages/api-docs/apiDocs.test.ts`:

```ts
import { describe, expect, it } from 'vitest'

import {
  API_DOCS,
  API_DOC_CATEGORIES,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'

describe('api docs data helpers', () => {
  it('contains external and internal endpoints', () => {
    expect(API_DOCS.some(endpoint => !endpoint.internal)).toBe(true)
    expect(API_DOCS.some(endpoint => endpoint.internal)).toBe(true)
  })

  it('hides internal endpoints by default', () => {
    const visible = filterApiDocEndpoints({ showInternal: false })

    expect(visible.every(endpoint => endpoint.internal !== true)).toBe(true)
    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/events/report')
    expect(visible.map(endpoint => endpoint.path)).not.toContain('/canvas/api-definitions')
  })

  it('reveals internal endpoints when requested', () => {
    const visible = filterApiDocEndpoints({ showInternal: true })

    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/api-definitions')
    expect(visible.map(endpoint => endpoint.path)).toContain('/admin/users')
  })

  it('filters by path title summary and category title', () => {
    expect(filterApiDocEndpoints({ showInternal: true, keyword: 'events/report' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
    expect(filterApiDocEndpoints({ showInternal: true, keyword: '用户管理' })
      .map(endpoint => endpoint.category)).toContain('users')
  })

  it('builds category summaries from visible endpoints', () => {
    const summaries = getApiDocCategorySummaries(filterApiDocEndpoints({ showInternal: false }))

    expect(summaries[0]).toMatchObject({
      key: API_DOC_CATEGORIES[0].key,
      title: API_DOC_CATEGORIES[0].title,
    })
    expect(summaries.every(summary => summary.count > 0)).toBe(true)
  })

  it('formats JSON examples with two-space indentation', () => {
    expect(formatJsonExample({ code: 0, message: 'success', data: { ok: true } })).toBe(
      '{\n  "code": 0,\n  "message": "success",\n  "data": {\n    "ok": true\n  }\n}',
    )
  })

  it('does not duplicate endpoint ids', () => {
    const ids = API_DOCS.map(endpoint => endpoint.id)

    expect(new Set(ids).size).toBe(ids.length)
  })
})
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
cd frontend
npm test -- src/pages/api-docs/apiDocs.test.ts
```

Expected: FAIL because `src/pages/api-docs/apiDocs.ts` does not exist.

- [ ] **Step 3: Implement data types, helpers, and full endpoint data**

Create `frontend/src/pages/api-docs/apiDocs.ts` with these exports:

```ts
export type ApiDocMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export type ApiDocAuth = 'none' | 'bearer'

export interface ApiDocParam {
  name: string
  in: 'path' | 'query' | 'body'
  required?: boolean
  desc: string
}

export interface ApiDocEndpoint {
  id: string
  title: string
  method: ApiDocMethod
  path: string
  category: string
  summary: string
  auth: ApiDocAuth
  internal?: boolean
  params?: ApiDocParam[]
  requestExample?: unknown
  responseExample?: unknown
}

export interface ApiDocCategory {
  key: string
  title: string
  description: string
}

export interface ApiDocCategorySummary extends ApiDocCategory {
  count: number
}

export interface ApiDocFilter {
  showInternal: boolean
  keyword?: string
  category?: string
}

export const API_DOC_CATEGORIES: ApiDocCategory[] = [
  { key: 'auth', title: '认证', description: '登录、登出和当前用户信息' },
  { key: 'external-trigger', title: '外部触发', description: '业务系统触发画布执行的主要入口' },
  { key: 'approval', title: '审批回调', description: '人工审批节点的通过和拒绝操作' },
  { key: 'canvas', title: '画布管理', description: '画布草稿、发布、版本和灰度管控' },
  { key: 'configuration', title: '配置管理', description: 'API、事件、MQ、标签、人群和实验配置' },
  { key: 'metadata', title: '元数据', description: '节点、上下文字段和下拉选项' },
  { key: 'observability', title: '运行观测', description: '执行记录、轨迹、统计和重放' },
  { key: 'operations', title: '运维与模板', description: '缓存、模板和发布审批工具' },
  { key: 'users', title: '用户管理', description: '后台用户管理接口' },
]

const ok = (data: unknown = {}) => ({ code: 0, message: 'success', data })
const page = (list: unknown[] = []) => ok({ total: list.length, list })

const endpoint = (item: ApiDocEndpoint): ApiDocEndpoint => item
```

Then add `export const API_DOCS: ApiDocEndpoint[] = [...]` with one endpoint per row from the Endpoint Inventory. Each endpoint must have:

- `id`: stable lowercase kebab string, for example `event-report`.
- `title`: short Chinese title, for example `事件上报`.
- `method`, `path`, `category`, and `internal` matching the inventory.
- `auth: 'none'` only for `POST /auth/login`; all other endpoints use `auth: 'bearer'`.
- `summary`: one sentence describing actual use.
- `params`: include path and common query/body parameters when visible from Controller signatures.
- `requestExample`: include a business-readable example for every `POST` and `PUT`.
- `responseExample`: use `ok(...)` or `page(...)`.

Add the helpers after the data:

```ts
export function filterApiDocEndpoints(filter: ApiDocFilter): ApiDocEndpoint[] {
  const keyword = filter.keyword?.trim().toLowerCase() ?? ''

  return API_DOCS.filter(endpoint => {
    if (endpoint.internal && !filter.showInternal) return false
    if (filter.category && endpoint.category !== filter.category) return false
    if (!keyword) return true

    const category = API_DOC_CATEGORIES.find(item => item.key === endpoint.category)
    const haystack = [
      endpoint.title,
      endpoint.path,
      endpoint.method,
      endpoint.summary,
      category?.title,
      category?.description,
    ].filter(Boolean).join(' ').toLowerCase()

    return haystack.includes(keyword)
  })
}

export function getApiDocCategorySummaries(endpoints: ApiDocEndpoint[]): ApiDocCategorySummary[] {
  return API_DOC_CATEGORIES.map(category => ({
    ...category,
    count: endpoints.filter(endpoint => endpoint.category === category.key).length,
  })).filter(category => category.count > 0)
}

export function formatJsonExample(value: unknown): string {
  return JSON.stringify(value, null, 2)
}
```

- [ ] **Step 4: Run helper tests and verify they pass**

Run:

```bash
cd frontend
npm test -- src/pages/api-docs/apiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit helper/data layer**

Run:

```bash
git add frontend/src/pages/api-docs/apiDocs.ts frontend/src/pages/api-docs/apiDocs.test.ts
git commit -m "feat: add api docs data helpers"
```

## Task 2: API Docs Page UI

**Files:**
- Create: `frontend/src/pages/api-docs/index.tsx`

- [ ] **Step 1: Create page component**

Create `frontend/src/pages/api-docs/index.tsx`:

```tsx
import { useMemo, useState } from 'react'
import {
  Badge,
  Button,
  Empty,
  Input,
  Space,
  Switch,
  Tag,
  Typography,
} from 'antd'
import {
  ApiOutlined,
  CopyOutlined,
  FileSearchOutlined,
  LockOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  API_DOC_CATEGORIES,
  type ApiDocEndpoint,
  type ApiDocMethod,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'

const { Text, Title, Paragraph } = Typography

const methodColors: Record<ApiDocMethod, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
}

function CodeBlock({ value }: { value: unknown }) {
  const text = formatJsonExample(value)

  return (
    <div style={{ position: 'relative' }}>
      <Button
        size="small"
        type="text"
        icon={<CopyOutlined />}
        onClick={() => navigator.clipboard?.writeText(text)}
        style={{ position: 'absolute', top: 6, right: 6, color: '#8c8c8c' }}
      />
      <pre style={{
        margin: 0,
        padding: '14px 38px 14px 14px',
        borderRadius: 8,
        background: '#101828',
        color: '#e6edf3',
        fontSize: 12,
        lineHeight: 1.7,
        overflowX: 'auto',
      }}>
        {text}
      </pre>
    </div>
  )
}

function EndpointCard({ endpoint }: { endpoint: ApiDocEndpoint }) {
  return (
    <section style={{
      border: '1px solid #e5e7eb',
      borderRadius: 8,
      background: '#fff',
      padding: 18,
      marginBottom: 14,
    }}>
      <Space size={8} wrap style={{ marginBottom: 10 }}>
        <Tag color={methodColors[endpoint.method]} style={{ fontFamily: 'monospace', marginInlineEnd: 0 }}>
          {endpoint.method}
        </Tag>
        <Text code style={{ whiteSpace: 'normal', wordBreak: 'break-all' }}>{endpoint.path}</Text>
        <Tag icon={<LockOutlined />} color={endpoint.auth === 'none' ? 'default' : 'gold'}>
          {endpoint.auth === 'none' ? '无需认证' : 'Bearer Token'}
        </Tag>
        {endpoint.internal && <Tag color="purple">内部</Tag>}
      </Space>

      <Title level={5} style={{ margin: '0 0 6px' }}>{endpoint.title}</Title>
      <Paragraph type="secondary" style={{ marginBottom: 14 }}>{endpoint.summary}</Paragraph>

      {endpoint.params && endpoint.params.length > 0 && (
        <div style={{ marginBottom: 14 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>参数</Text>
          <div style={{ border: '1px solid #edf0f3', borderRadius: 8, overflow: 'hidden' }}>
            {endpoint.params.map(param => (
              <div key={`${param.in}-${param.name}`} style={{
                display: 'grid',
                gridTemplateColumns: '120px 76px 1fr',
                gap: 10,
                padding: '9px 12px',
                borderTop: '1px solid #edf0f3',
                marginTop: -1,
                fontSize: 13,
              }}>
                <Text code>{param.name}</Text>
                <Text type={param.required ? 'danger' : 'secondary'}>{param.required ? '必填' : '可选'}</Text>
                <Text type="secondary">{param.desc}</Text>
              </div>
            ))}
          </div>
        </div>
      )}

      <div style={{
        display: 'grid',
        gridTemplateColumns: endpoint.requestExample ? '1fr 1fr' : '1fr',
        gap: 12,
      }}>
        {endpoint.requestExample && (
          <div>
            <Text strong style={{ display: 'block', marginBottom: 8 }}>请求示例</Text>
            <CodeBlock value={endpoint.requestExample} />
          </div>
        )}
        <div>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>响应示例</Text>
          <CodeBlock value={endpoint.responseExample ?? { code: 0, message: 'success', data: null }} />
        </div>
      </div>
    </section>
  )
}

export default function ApiDocsPage() {
  const [keyword, setKeyword] = useState('')
  const [showInternal, setShowInternal] = useState(false)
  const visibleForCategories = useMemo(
    () => filterApiDocEndpoints({ showInternal, keyword }),
    [showInternal, keyword],
  )
  const categories = useMemo(() => getApiDocCategorySummaries(visibleForCategories), [visibleForCategories])
  const [activeCategory, setActiveCategory] = useState<string>('all')
  const endpoints = useMemo(
    () => filterApiDocEndpoints({
      showInternal,
      keyword,
      category: activeCategory === 'all' ? undefined : activeCategory,
    }),
    [activeCategory, showInternal, keyword],
  )

  const categoryOptions = [
    { label: `全部 ${visibleForCategories.length}`, value: 'all' },
    ...categories.map(category => ({
      label: `${category.title} ${category.count}`,
      value: category.key,
    })),
  ]

  return (
    <div style={{ maxWidth: 1440, margin: '0 auto' }}>
      <div style={{ marginBottom: 18 }}>
        <Space size={10} align="center" style={{ marginBottom: 8 }}>
          <ApiOutlined style={{ fontSize: 22, color: '#1677ff' }} />
          <Title level={3} style={{ margin: 0 }}>API 说明</Title>
        </Space>
        <Paragraph type="secondary" style={{ margin: 0, maxWidth: 760 }}>
          面向业务系统和平台集成方的接口参考。默认展示外部调用接口，打开开关后可查看后台页面能力对应的内部管理接口。
        </Paragraph>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'minmax(180px, 220px) minmax(0, 1fr)',
        gap: 18,
        alignItems: 'start',
      }}>
        <aside style={{
          position: 'sticky',
          top: 24,
          border: '1px solid #e5e7eb',
          borderRadius: 8,
          background: '#fff',
          padding: 12,
        }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索路径或名称"
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            style={{ marginBottom: 12 }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 12 }}>
            <Text type="secondary">显示内部管理 API</Text>
            <Switch checked={showInternal} onChange={setShowInternal} />
          </div>
          <Space direction="vertical" size={4} style={{ display: 'flex' }}>
            {categoryOptions.map(option => (
              <Button
                key={option.value}
                block
                type={activeCategory === option.value ? 'primary' : 'text'}
                onClick={() => setActiveCategory(String(option.value))}
                style={{ textAlign: 'left', justifyContent: 'flex-start' }}
              >
                {option.label}
              </Button>
            ))}
          </Space>
        </aside>

        <main>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 12,
          }}>
            <Space>
              <FileSearchOutlined />
              <Text strong>{endpoints.length} 个接口</Text>
              {showInternal && <Badge color="purple" text="已显示内部接口" />}
            </Space>
            <Text type="secondary">
              {API_DOC_CATEGORIES.find(category => category.key === activeCategory)?.description ?? '全部接口'}
            </Text>
          </div>

          {endpoints.length === 0 ? (
            <Empty description="没有匹配的 API" />
          ) : (
            endpoints.map(endpoint => <EndpointCard key={endpoint.id} endpoint={endpoint} />)
          )}
        </main>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Run TypeScript build and fix any compile issues**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 3: Commit page UI**

Run:

```bash
git add frontend/src/pages/api-docs/index.tsx
git commit -m "feat: add api docs page"
```

## Task 3: Route and Navigation

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Register `/api-docs` route**

Modify `frontend/src/App.tsx`:

```tsx
import ApiDocsPage from './pages/api-docs'
```

Add the admin route inside the `RequireAdmin` + `AppLayout` block:

```tsx
<Route path="/api-docs" element={<ApiDocsPage />} />
```

- [ ] **Step 2: Add menu selected state and menu item**

Modify imports in `frontend/src/components/layout/AppLayout.tsx`:

```tsx
import {
  ApartmentOutlined, SettingOutlined, ApiOutlined,
  ExperimentOutlined, TeamOutlined, LogoutOutlined,
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  RocketOutlined, HomeOutlined, TagsOutlined, NotificationOutlined, ThunderboltOutlined,
  BookOutlined,
} from '@ant-design/icons'
```

Add selected key handling:

```ts
if (location.pathname.startsWith('/api-docs')) return 'api-docs'
```

Update default open keys:

```ts
if (['api-docs'].includes(selectedKey)) return ['developer']
if (['api-config', 'ab-experiments', 'admin-users'].includes(selectedKey)) return ['marketing', 'settings']
```

Add a new menu group after 「自动化营销」 and before 「系统设置」:

```tsx
{
  key: 'developer',
  icon: <BookOutlined />,
  label: '开发者文档',
  children: [
    {
      key: 'api-docs',
      icon: <ApiOutlined />,
      label: 'API 说明',
      onClick: () => navigate('/api-docs'),
    },
  ],
},
```

- [ ] **Step 3: Run build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit route and navigation**

Run:

```bash
git add frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: expose api docs navigation"
```

## Task 4: Verification

**Files:**
- Test only, no planned code edits.

- [ ] **Step 1: Run focused tests**

Run:

```bash
cd frontend
npm test -- src/pages/api-docs/apiDocs.test.ts
```

Expected: PASS.

- [ ] **Step 2: Run full frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 3: Start dev server**

Run:

```bash
cd frontend
npm run dev -- --host 0.0.0.0
```

Expected: Vite prints a local URL, usually `http://localhost:3000/`. Verify `/api-docs` loads after admin login.

- [ ] **Step 4: Final status**

Report:

- Files changed.
- Test commands and results.
- Dev server URL.
- The page defaults to external APIs and reveals internal APIs with the switch.
