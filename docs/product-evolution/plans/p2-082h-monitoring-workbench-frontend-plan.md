# Monitoring Workbench Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an authenticated frontend workbench for P2-082G monitoring sources, mentions, sentiment/competitor review, and alert resolution.

**Architecture:** Reuse the existing frontend pattern of typed API service, pure presentation helpers, and a page-level Ant Design workbench. Keep external crawlers out of scope and rely on the P2-082G connector-neutral backend contract.

**Tech Stack:** React, TypeScript, Vite, Ant Design, Vitest, React Testing Library.

**Implementation Status:** Current workspace record: delivered frontend first slice. Verification results are recorded below.

---

## Scope

This plan implements P2-082H:

- Documentation and product indexes.
- Typed monitoring frontend service.
- Presentation helpers for filters, tags, JSON parsing, KPI summaries, and date display.
- `/marketing-monitoring` authenticated page with source upsert, manual ingest, mention review, alert review, and resolve action.
- Route and navigation integration.
- Focused frontend tests and build verification.

## Files

- Create `docs/product-evolution/specs/p2-082h-monitoring-workbench-frontend.md`.
- Create `docs/product-evolution/plans/p2-082h-monitoring-workbench-frontend-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `frontend/src/services/marketingMonitoringApi.ts`.
- Create `frontend/src/services/marketingMonitoringApi.test.ts`.
- Create `frontend/src/pages/marketing-monitoring/monitoringWorkbench.ts`.
- Create `frontend/src/pages/marketing-monitoring/monitoringWorkbench.test.ts`.
- Create `frontend/src/pages/marketing-monitoring/index.tsx`.
- Create `frontend/src/pages/marketing-monitoring/index.test.tsx`.
- Modify `frontend/src/App.tsx`.
- Modify `frontend/src/components/layout/AppLayout.tsx`.

## Tasks

### Task 1: Index P2-082H Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082H after P2-082G in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs to mention monitoring workbench frontend**
- [x] **Step 4: Verify indexability with `rg -n "P2-082H|p2-082h-monitoring-workbench-frontend"`**

### Task 2: Add Monitoring API Service With TDD

- [x] **Step 1: Write failing `marketingMonitoringApi.test.ts`**
- [x] **Step 2: Verify RED with `npm run test -- marketingMonitoringApi.test.ts --run`**
- [x] **Step 3: Implement `marketingMonitoringApi.ts`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Presentation Helpers With TDD

- [x] **Step 1: Write failing `monitoringWorkbench.test.ts`**
- [x] **Step 2: Verify RED with `npm run test -- monitoringWorkbench.test.ts --run`**
- [x] **Step 3: Implement helper types and functions**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Workbench Page With TDD

- [x] **Step 1: Write failing `index.test.tsx`**
- [x] **Step 2: Verify RED with `npm run test -- marketing-monitoring/index.test.tsx --run`**
- [x] **Step 3: Implement `index.tsx`**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Route And Navigation

- [x] **Step 1: Add lazy route in `App.tsx`**
- [x] **Step 2: Add operations nav item and selected/open-key handling in `AppLayout.tsx`**
- [x] **Step 3: Verify focused route/nav tests or compile**

### Task 6: Verify Frontend Slice

- [x] **Step 1: Run focused frontend tests**
- [x] **Step 2: Run `npm run build`**
- [x] **Step 3: Run local app and inspect `/marketing-monitoring` in browser**
