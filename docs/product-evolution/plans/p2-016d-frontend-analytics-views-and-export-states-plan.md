# Frontend Analytics Views And Export States Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add frontend analytics views and bounded export states on top of P2-016C APIs.

**Architecture:** Keep HTTP calls in `analyticsApi.ts`, put formatting/state decisions in `analyticsPresentation.ts`, and build the page from tested states before wiring charts/tables into the route.

**Tech Stack:** React 18, TypeScript, Ant Design, Recharts, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016d-frontend-analytics-views-and-export-states.md`
- Depends on P2-016C backend APIs.

## File Structure

- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/services/analyticsApi.test.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.test.ts`
- Create: `frontend/src/pages/analytics/index.tsx`
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx`

### Task 1: API Wrapper And Presentation Helpers

**Files:**
- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/services/analyticsApi.test.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.test.ts`

- [ ] **Step 1: Write API wrapper tests**

Create `analyticsApi.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'
import { createAnalyticsApi } from './analyticsApi'

describe('analyticsApi', () => {
  it('passes tenant and bounded date range to event and export endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: { id: 1, status: 'QUEUED' } }),
    }
    const api = createAnalyticsApi(http as any)

    await api.eventAnalysis({ tenantId: 0, startDate: '2026-06-01', endDate: '2026-06-03' })
    await api.createExport({ tenantId: 0, reportType: 'EVENT_ANALYSIS', startDate: '2026-06-01', endDate: '2026-06-03', rowLimit: 1000 })

    expect(http.get).toHaveBeenCalledWith('/analytics/events', { params: { tenantId: 0, startDate: '2026-06-01', endDate: '2026-06-03' } })
    expect(http.post).toHaveBeenCalledWith('/analytics/exports', { tenantId: 0, reportType: 'EVENT_ANALYSIS', startDate: '2026-06-01', endDate: '2026-06-03', rowLimit: 1000 })
  })
})
```

- [ ] **Step 2: Write presentation tests**

Create `analyticsPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { exportStateText, formatEventCount, requireDateRangeMessage, timelineRowText } from './analyticsPresentation'

describe('analyticsPresentation', () => {
  it('requires date range before querying', () => {
    expect(requireDateRangeMessage({ startDate: undefined, endDate: '2026-06-03' })).toBe('Select a start and end date')
    expect(requireDateRangeMessage({ startDate: '2026-06-01', endDate: '2026-06-03' })).toBeNull()
  })

  it('formats event counts and timeline rows', () => {
    expect(formatEventCount({ eventCode: 'OrderPaid', count: 12 })).toBe('OrderPaid: 12')
    expect(timelineRowText({ eventCode: 'OrderPaid', eventTime: '2026-06-02T10:00:00Z' })).toBe('2026-06-02T10:00:00Z - OrderPaid')
  })

  it('formats export states', () => {
    expect(exportStateText('QUEUED')).toBe('Queued')
    expect(exportStateText('FAILED')).toBe('Failed')
  })
})
```

- [ ] **Step 3: Run tests and confirm red state**

Run:

```bash
cd frontend && npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Expected: FAIL because files do not exist.

- [ ] **Step 4: Implement API wrapper**

Create `frontend/src/services/analyticsApi.ts`:

```ts
import http from './api'

export interface AnalyticsScope {
  tenantId: number
  startDate: string
  endDate: string
}

export interface ExportPayload extends AnalyticsScope {
  reportType: string
  rowLimit: number
}

export function createAnalyticsApi(client = http) {
  return {
    eventAnalysis: (scope: AnalyticsScope) => client.get('/analytics/events', { params: scope }),
    funnel: (funnelKey: string, scope: AnalyticsScope) => client.get(`/analytics/funnels/${funnelKey}`, { params: scope }),
    userTimeline: (userId: string, scope: AnalyticsScope & { page: number; size: number }) =>
      client.get(`/analytics/users/${encodeURIComponent(userId)}/timeline`, { params: scope }),
    attributeDistribution: (attribute: string, scope: AnalyticsScope) =>
      client.get(`/analytics/attributes/${encodeURIComponent(attribute)}/distribution`, { params: scope }),
    alertPreview: (payload: Record<string, unknown>) => client.post('/analytics/alerts/preview', payload),
    createExport: (payload: ExportPayload) => client.post('/analytics/exports', payload),
    exportStatus: (id: number) => client.get(`/analytics/exports/${id}`),
  }
}

export const analyticsApi = createAnalyticsApi()
```

- [ ] **Step 5: Implement presentation helpers**

Create `analyticsPresentation.ts`:

```ts
export function requireDateRangeMessage(input: { startDate?: string; endDate?: string }): string | null {
  return input.startDate && input.endDate ? null : 'Select a start and end date'
}

export function formatEventCount(row: { eventCode: string; count: number }): string {
  return `${row.eventCode}: ${row.count}`
}

export function timelineRowText(row: { eventCode: string; eventTime: string }): string {
  return `${row.eventTime} - ${row.eventCode}`
}

export function exportStateText(status: string): string {
  return status === 'QUEUED' ? 'Queued' : status === 'RUNNING' ? 'Running' : status === 'DONE' ? 'Done' : 'Failed'
}
```

- [ ] **Step 6: Run API and presentation tests**

Run:

```bash
cd frontend && npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Expected: PASS.

### Task 2: Analytics Page And User Detail Link

**Files:**
- Create: `frontend/src/pages/analytics/index.tsx`
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx`

- [ ] **Step 1: Build analytics page states**

Create tabs for event analysis, funnel, user timeline, attribute distribution, alerts, and exports. Each tab must use `requireDateRangeMessage` before calling the API and show loading, empty, error, and permission-denied states from the API result.

- [ ] **Step 2: Add export queued state**

When `analyticsApi.createExport` returns `QUEUED`, show the formatted status from `exportStateText` and a refresh action that calls `exportStatus(id)`.

- [ ] **Step 3: Add CDP user detail link**

In `cdp-user-detail/index.tsx`, add a link to the analytics timeline view with `userId`, `startDate`, and `endDate` query params. Do not duplicate analytics query logic inside the CDP page.

- [ ] **Step 4: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-016d-frontend-analytics-views-and-export-states.md`
- Modify: `docs/product-evolution/plans/p2-016d-frontend-analytics-views-and-export-states-plan.md`

- [ ] **Step 1: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add frontend/src/services/analyticsApi.ts frontend/src/services/analyticsApi.test.ts frontend/src/pages/analytics frontend/src/pages/cdp-user-detail/index.tsx docs/product-evolution/specs/p2-016d-frontend-analytics-views-and-export-states.md docs/product-evolution/plans/p2-016d-frontend-analytics-views-and-export-states-plan.md
git commit -m "feat: add frontend analytics views"
```

Expected: commit contains only frontend analytics API, page, presentation helpers, CDP detail link, tests, and related docs.
