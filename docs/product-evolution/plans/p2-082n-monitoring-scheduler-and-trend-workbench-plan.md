# Monitoring Scheduler And Trend Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add always-on monitoring source polling and expose persisted trend snapshots in the monitoring workbench.

**Architecture:** Backend adds a due-source scheduling service plus a disabled-by-default Spring scheduler wrapper following the existing warehouse scheduler pattern. Frontend extends the existing monitoring API and workbench page to configure/build/query trend snapshots without adding a charting dependency.

**Tech Stack:** Java 21, Spring Boot scheduling, MyBatis-Plus, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest, Testing Library.

---

## Scope

This plan implements P2-082N:

- Product docs and indexes.
- Backend due polling schedule service.
- Backend scheduled wrapper.
- Frontend API methods and types for polling/trends.
- Frontend trend snapshot panel in `/marketing-monitoring`.
- Focused backend/frontend tests plus monitoring regression.

## Files

- Create `docs/product-evolution/specs/p2-082n-monitoring-scheduler-and-trend-workbench.md`.
- Create `docs/product-evolution/plans/p2-082n-monitoring-scheduler-and-trend-workbench-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduleService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduler.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduleServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingSchedulerTest.java`.
- Modify `frontend/src/pages/marketing-monitoring/monitoringWorkbench.ts`.
- Modify `frontend/src/services/marketingMonitoringApi.ts`.
- Modify `frontend/src/services/marketingMonitoringApi.test.ts`.
- Modify `frontend/src/pages/marketing-monitoring/index.tsx`.
- Modify `frontend/src/pages/marketing-monitoring/index.test.tsx`.

## Tasks

### Task 1: Index P2-082N Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082N after P2-082M in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with scheduler/trend workbench slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082N|p2-082n-monitoring-scheduler-and-trend-workbench"`**

### Task 2: Add Backend Due Polling Service With TDD

- [x] **Step 1: Write failing `MarketingMonitorPollingScheduleServiceTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `MarketingMonitorPollingScheduleService`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Backend Scheduler Wrapper With TDD

- [x] **Step 1: Write failing `MarketingMonitorPollingSchedulerTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `MarketingMonitorPollingScheduler`**
- [x] **Step 4: Verify GREEN**

### Task 4: Extend Frontend API And Helpers With TDD

- [x] **Step 1: Write failing API/helper tests for polling and trend endpoints**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add types, query normalizers, and API methods**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Trend Snapshot Workbench Panel With TDD

- [x] **Step 1: Write failing page tests for trend loading and snapshot build**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add trend panel UI to `MarketingMonitoringPage`**
- [x] **Step 4: Verify GREEN**

### Task 6: Verify Slice And Update Docs

- [x] **Step 1: Run focused backend tests**
- [x] **Step 2: Run focused frontend tests**
- [x] **Step 3: Run monitoring regression tests**
- [x] **Step 4: Update P2-082N and parent docs to delivered after verification passes**

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitoringControllerTest test` - 26 tests passed.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitoringControllerTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookIngestionServiceTest,PublicMarketingMonitoringWebhookControllerTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest test` - 49 tests passed.
- `npm run test -- src/services/marketingMonitoringApi.test.ts src/pages/marketing-monitoring/monitoringWorkbench.test.ts src/pages/marketing-monitoring/index.test.tsx` - 7 tests passed.
- `npm run build` - TypeScript and Vite production build passed.

## TDD Evidence

- Backend scheduler service and wrapper tests were written and observed failing before implementation.
- Frontend API/helper RED failed on missing `configureSourcePolling` and `normalizeTrendQuery`.
- Frontend page RED failed on missing trend snapshot loading/build UI.
