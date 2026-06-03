# Event Config Write Key And Attribute Review UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add frontend service helpers and event-config entry points for CDP write keys and discovered attribute review.

**Architecture:** Keep API normalization in `cdpEventApi.ts` and presentation labels in a small helper before wiring into the existing event-config page. The page surfaces compact operational entry points without introducing a full developer portal.

**Tech Stack:** React 18, TypeScript, Ant Design, Vite, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005a3-event-config-write-key-and-attribute-review-ui.md`
- Depends on `docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md`
- Depends on `docs/product-evolution/specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md`

## File Structure

- Create: `frontend/src/services/cdpEventApi.ts`
- Create: `frontend/src/services/cdpEventApi.test.ts`
- Create: `frontend/src/pages/event-config/eventAttributeReview.ts`
- Create: `frontend/src/pages/event-config/eventAttributeReview.test.ts`
- Modify: `frontend/src/pages/event-config/index.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

### Task 1: API Helper

**Files:**
- Create: `frontend/src/services/cdpEventApi.ts`
- Create: `frontend/src/services/cdpEventApi.test.ts`

- [ ] **Step 1: Write API helper tests**

Create `cdpEventApi.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildCreateWriteKeyPayload,
  normalizeDiscoveredAttributeRows,
  safeWriteKeyRows,
  type DiscoveredAttribute,
  type WriteKeyRow,
} from './cdpEventApi'

describe('cdp event api helpers', () => {
  it('builds write key payload with default platform and qps', () => {
    expect(buildCreateWriteKeyPayload({ name: 'Website' })).toEqual({
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
    ]

    expect(normalizeDiscoveredAttributeRows(rows).map(row => row.attrName)).toEqual(['currency', 'amount'])
  })

  it('drops secret-like fields from write key rows', () => {
    const rows = [{ id: 1, name: 'Website', keyPrefix: 'ck_test', platform: 'WEB', status: 'ACTIVE', keyHash: 'hash' } as WriteKeyRow & { keyHash: string }]

    expect(safeWriteKeyRows(rows)[0]).not.toHaveProperty('keyHash')
  })
})
```

- [ ] **Step 2: Add API helper**

Create `cdpEventApi.ts`:

```ts
import { http } from './api'
import type { R } from '../types'

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

export interface WriteKeyCreateResp extends WriteKeyRow {
  writeKey: string
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

export function buildCreateWriteKeyPayload(form: WriteKeyCreateForm) {
  return {
    name: form.name.trim(),
    platform: form.platform || 'WEB',
    rateLimitQps: form.rateLimitQps || 100,
    dailyQuota: form.dailyQuota ?? null,
    description: form.description || '',
  }
}

export function normalizeDiscoveredAttributeRows(rows: DiscoveredAttribute[]) {
  return [...rows].sort((a, b) => {
    if (a.status === b.status) return a.attrName.localeCompare(b.attrName)
    return a.status === 'PENDING_REVIEW' ? -1 : 1
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

export const cdpEventApi = {
  listWriteKeys: () => http.get<R<WriteKeyRow[]>, R<WriteKeyRow[]>>('/cdp/write-keys'),
  createWriteKey: (form: WriteKeyCreateForm) =>
    http.post<R<WriteKeyCreateResp>, R<WriteKeyCreateResp>>('/cdp/write-keys', buildCreateWriteKeyPayload(form)),
  disableWriteKey: (id: number) => http.delete<R<void>, R<void>>(`/cdp/write-keys/${id}`),
  listDiscoveredAttributes: () =>
    http.get<R<DiscoveredAttribute[]>, R<DiscoveredAttribute[]>>('/canvas/event-attributes/discovered'),
}
```

- [ ] **Step 3: Run API helper tests**

Run:

```bash
cd frontend && npm test -- cdpEventApi.test.ts
```

Expected: PASS.

### Task 2: Attribute Review Presentation

**Files:**
- Create: `frontend/src/pages/event-config/eventAttributeReview.ts`
- Create: `frontend/src/pages/event-config/eventAttributeReview.test.ts`

- [ ] **Step 1: Write presentation tests**

Create `eventAttributeReview.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { statusColor, statusLabel } from './eventAttributeReview'

describe('event attribute review presentation', () => {
  it('labels review statuses in Chinese', () => {
    expect(statusLabel('PENDING_REVIEW')).toBe('待审核')
    expect(statusLabel('APPROVED')).toBe('已通过')
    expect(statusLabel('REJECTED')).toBe('已拒绝')
  })

  it('uses warning color for pending rows', () => {
    expect(statusColor('PENDING_REVIEW')).toBe('orange')
  })
})
```

- [ ] **Step 2: Add presentation helper**

Create `eventAttributeReview.ts`:

```ts
export function statusLabel(status: string) {
  if (status === 'PENDING_REVIEW') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已拒绝'
  return status
}

export function statusColor(status: string) {
  if (status === 'PENDING_REVIEW') return 'orange'
  if (status === 'APPROVED') return 'green'
  if (status === 'REJECTED') return 'red'
  return 'default'
}
```

- [ ] **Step 3: Run presentation tests**

Run:

```bash
cd frontend && npm test -- eventAttributeReview.test.ts
```

Expected: PASS.

### Task 3: Event Config Page Entry Points

**Files:**
- Modify: `frontend/src/pages/event-config/index.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Add navigation entry**

In `AppLayout.tsx`, add a child item under the current event/config or CDP section:

```tsx
{
  key: '/event-config',
  icon: <SettingOutlined />,
  label: '事件配置',
}
```

Reuse the icon import already present in the file when available.

- [ ] **Step 2: Add event-config operational header**

In `event-config/index.tsx`, import the API helper and presentation helpers:

```tsx
import { cdpEventApi, normalizeDiscoveredAttributeRows, safeWriteKeyRows } from '../../services/cdpEventApi'
import { statusColor, statusLabel } from './eventAttributeReview'
```

Add state:

```tsx
const [writeKeys, setWriteKeys] = useState<WriteKeyRow[]>([])
const [discoveredAttributes, setDiscoveredAttributes] = useState<DiscoveredAttribute[]>([])
```

Add loader functions:

```tsx
const loadWriteKeys = async () => {
  const res = await cdpEventApi.listWriteKeys()
  setWriteKeys(safeWriteKeyRows(res.data))
}

const loadDiscoveredAttributes = async () => {
  const res = await cdpEventApi.listDiscoveredAttributes()
  setDiscoveredAttributes(normalizeDiscoveredAttributeRows(res.data))
}
```

- [ ] **Step 3: Render compact tables**

Add a write-key table:

```tsx
<Table
  rowKey="id"
  dataSource={writeKeys}
  pagination={false}
  columns={[
    { title: '名称', dataIndex: 'name' },
    { title: 'Key 前缀', dataIndex: 'keyPrefix' },
    { title: '平台', dataIndex: 'platform' },
    { title: '状态', dataIndex: 'status' },
  ]}
/>
```

Add an attribute review table:

```tsx
<Table
  rowKey="id"
  dataSource={discoveredAttributes}
  pagination={false}
  columns={[
    { title: '事件', dataIndex: 'eventCode' },
    { title: '属性', dataIndex: 'attrName' },
    { title: '类型', dataIndex: 'attrType' },
    {
      title: '状态',
      render: (_, record) => <Tag color={statusColor(record.status)}>{statusLabel(record.status)}</Tag>,
    },
  ]}
/>
```

- [ ] **Step 4: Run frontend focused tests**

Run:

```bash
cd frontend && npm test -- cdpEventApi.test.ts eventAttributeReview.test.ts
```

Expected: PASS.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005a3-event-config-write-key-and-attribute-review-ui.md`
- Read: `docs/product-evolution/plans/p1-005a3-event-config-write-key-and-attribute-review-ui-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add frontend/src/services/cdpEventApi.ts \
  frontend/src/services/cdpEventApi.test.ts \
  frontend/src/pages/event-config/eventAttributeReview.ts \
  frontend/src/pages/event-config/eventAttributeReview.test.ts \
  frontend/src/pages/event-config/index.tsx \
  frontend/src/components/layout/AppLayout.tsx \
  docs/product-evolution/specs/p1-005a3-event-config-write-key-and-attribute-review-ui.md \
  docs/product-evolution/plans/p1-005a3-event-config-write-key-and-attribute-review-ui-plan.md
git commit -m "feat: add cdp event config entry points"
```

Expected: commit contains only event config frontend helpers, page wiring, tests, and docs.
