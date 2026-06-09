# Frontend Analytics Views And Export States Implementation Plan

Status: Current frontend implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add frontend analytics views and bounded export states on top of currently exposed P2-016C APIs.

**Architecture:** Keep HTTP calls in `analyticsApi.ts`, put formatting/state decisions in `analyticsPresentation.ts`, and build the route from current backend contracts: event counts, event total, user timeline, and attribute distribution. Funnel, alert preview, and export-job backend endpoints are not exposed in this backend slice, so the page shows an explicit unavailable export state instead of calling missing APIs.

**Tech Stack:** React 18, TypeScript, Ant Design, Vitest, Vite.

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
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx`

### Task 1: API Wrapper And Presentation Helpers

**Files:**
- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/services/analyticsApi.test.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.ts`
- Create: `frontend/src/pages/analytics/analyticsPresentation.test.ts`

- [x] **Step 1: Write API wrapper tests**

`analyticsApi.test.ts` covers event counts, event total, user timeline path encoding, and attribute distribution path encoding against the currently exposed backend routes.

- [x] **Step 2: Write presentation tests**

`analyticsPresentation.test.ts` covers date-range requirements, event count formatting, timeline row formatting, event count sorting, and export state labels.

- [x] **Step 3: Verify red state**

Initial run on 2026-06-09:

```bash
cd frontend
npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Result: FAIL because `analyticsApi.ts` and `analyticsPresentation.ts` did not exist.

- [x] **Step 4: Implement API wrapper**

`analyticsApi.ts` wraps:
- `GET /analytics/events/counts`
- `GET /analytics/events/count`
- `GET /analytics/users/{userId}/timeline`
- `GET /analytics/events/attributes/{attribute}/distribution`

- [x] **Step 5: Implement presentation helpers**

`analyticsPresentation.ts` implements date-range guard, event/timeline formatting, sorted event count rows, and export status labels.

- [x] **Step 6: Run API and presentation tests**

Run:

```bash
cd frontend
npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Result on 2026-06-09: PASS, 2 test files, 6 tests, 0 failures.

### Task 2: Analytics Page And Navigation

**Files:**
- Create: `frontend/src/pages/analytics/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/pages/cdp-user-detail/index.tsx`

- [x] **Step 1: Build analytics page states**

The page provides event overview, user timeline, attribute distribution, and export status tabs. It handles loading, empty, and error states through Ant Design table/alert states.

- [x] **Step 2: Add export unavailable state**

Because the current backend does not expose export-job endpoints, the frontend renders `UNAVAILABLE` through `exportStateText` and does not call nonexistent routes.

- [x] **Step 3: Add route and menu entry**

`App.tsx` lazy-loads `/analytics`, and `AppLayout.tsx` adds the analytics menu item under the data analysis group.

- [x] **Step 4: Add CDP user detail link**

`cdp-user-detail/index.tsx` adds a user timeline link that opens `/analytics` with `userId`, `startDate`, and `endDate` query params. Analytics querying remains inside the analytics page.

### Task 3: Verification

**Files:**
- Modify: `docs/product-evolution/specs/p2-016d-frontend-analytics-views-and-export-states.md`
- Modify: `docs/product-evolution/plans/p2-016d-frontend-analytics-views-and-export-states-plan.md`

- [x] **Step 1: Run focused frontend tests**

Run:

```bash
cd frontend
npm test -- analyticsApi.test.ts analyticsPresentation.test.ts
```

Result on 2026-06-09: PASS, 2 test files, 6 tests, 0 failures.

- [x] **Step 2: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Result on 2026-06-09: PASS.

- [ ] **Step 3: Commit and merge**

Commit and merge status are intentionally not claimed by this document. Treat this as an implementation and focused-verification record until the branch is committed and integrated.
