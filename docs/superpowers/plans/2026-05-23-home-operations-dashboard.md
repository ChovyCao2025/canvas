# Home Operations Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a colorful operations-focused homepage backed by one global overview endpoint.

**Architecture:** Add a backend home overview controller/service that aggregates published canvases and executions for the selected range. Add a small frontend API wrapper plus a redesigned `HomePage` that renders KPI cards, a trend chart, Top canvases, attention items, quick actions, loading, empty, and error states.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus mappers, React 18, TypeScript, Ant Design 5, Recharts, Vitest.

---

### Task 1: Backend Overview Endpoint

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/home/HomeOverviewDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/HomeOverviewController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/HomeOverviewControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

Create `HomeOverviewControllerTest` with mocked `CanvasMapper` and `CanvasExecutionMapper`. Assert that a 7-day overview with one published canvas, two successful executions, and one failed execution returns `publishedCanvasCount=1`, `totalExecutions=3`, `uniqueUsers=2`, `failedExecutions=1`, `successRate="66.7%"`, a trend point, one top canvas, and one attention item for failures.

- [ ] **Step 2: Run the failing test**

Run: `mvn -pl canvas-engine -Dtest=HomeOverviewControllerTest test`

- [ ] **Step 3: Implement DTO and controller**

`HomeOverviewController` exposes `GET /canvas/home/overview`, clamps `days` to `1`, `7`, or `30`, loads all non-archived canvases, filters published canvases, loads executions in `[since.atStartOfDay, until.plusDays(1).atStartOfDay)`, computes summary, trend, top canvases, and attention items.

- [ ] **Step 4: Run backend test**

Run: `mvn -pl canvas-engine -Dtest=HomeOverviewControllerTest test`

### Task 2: Frontend API and Presentation Helpers

**Files:**
- Create: `frontend/src/pages/home/homeOverview.ts`
- Modify: `frontend/src/services/api.ts`
- Test: `frontend/src/pages/home/homeOverview.test.ts`

- [ ] **Step 1: Write helper tests**

Test allowed range options, KPI card construction, and graceful formatting for missing/zero values.

- [ ] **Step 2: Implement types and helpers**

Define `HomeOverview`, `HomeSummary`, `HomeTrendPoint`, `HomeTopCanvas`, `HomeAttentionItem`, `HOME_RANGE_OPTIONS`, and `buildKpiCards`.

- [ ] **Step 3: Add API wrapper**

Add `homeApi.overview(days)` to `frontend/src/services/api.ts`.

- [ ] **Step 4: Run frontend helper test**

Run: `cd frontend && npm test -- homeOverview.test.ts`

### Task 3: Frontend Homepage UI

**Files:**
- Modify: `frontend/src/pages/home/index.tsx`

- [ ] **Step 1: Replace placeholder with dashboard**

Use Ant Design `Card`, `Row`, `Col`, `Segmented`, `Table`, `List`, `Alert`, `Button`, and Recharts `AreaChart`. Keep the layout: colorful KPI cards, trend chart, quick actions, Top journeys, attention list.

- [ ] **Step 2: Add operational navigation**

Use `useNavigate` for new canvas, journey management, audiences, API, MQ, and event configuration. Top journey rows link to `/canvas/{id}/stats`; attention rows link to stats or editor depending on the item type.

- [ ] **Step 3: Verify build**

Run: `cd frontend && npm run build`

### Task 4: Final Verification

**Files:**
- All changed files.

- [ ] **Step 1: Run focused backend test**

Run: `cd backend/canvas-engine && mvn -Dtest=HomeOverviewControllerTest test`

- [ ] **Step 2: Run focused frontend test**

Run: `cd frontend && npm test -- homeOverview.test.ts`

- [ ] **Step 3: Run frontend build**

Run: `cd frontend && npm run build`
