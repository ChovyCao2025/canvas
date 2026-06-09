# Execution Timeline And Batch Operations Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve execution timeline inspection and add safe batch list actions.

**Architecture:** Keep trace formatting in frontend presentation helpers, add a small batch operation controller for list actions, and return per-item statuses for every batch command.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017e-execution-timeline-and-batch-operations.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused verification passed on 2026-06-09:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest,UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest,ConnectedContentHandlerTest,ExecutionRerunControllerTest,CanvasBatchOperationControllerTest,RuntimeMigrationEvidenceTest` (covered `CanvasBatchOperationControllerTest`; 48 total selected backend tests passed).
- `npm test -- variableAvailability.test.ts variablePicker.test.tsx executionTimeline.test.tsx` (covered `executionTimeline.test.tsx`; 12 frontend tests passed).

Historical RED-state checks were not reproduced because the current worktree already contains the implementation. No commit or merge was created in this audit, so commit and merge status remain unverified.

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasBatchOperationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasBatchOperationControllerTest.java`
- Modify: `frontend/src/components/canvas/ExecutionTracePanel.tsx`
- Create: `frontend/src/components/canvas/executionTimelinePresentation.ts`
- Create: `frontend/src/components/canvas/executionTimeline.test.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

### Task 1: Timeline Presentation

**Files:**
- Create: `frontend/src/components/canvas/executionTimelinePresentation.ts`
- Create: `frontend/src/components/canvas/executionTimeline.test.tsx`
- Modify: `frontend/src/components/canvas/ExecutionTracePanel.tsx`

- [x] **Step 1: Write timeline tests**

Create tests for full error expansion, long error download content, path highlight class, trace click-to-node callback, and status label formatting.

- [x] **Step 2: Implement presentation helpers**

Add helpers `formatTraceStatus`, `isLongError`, `downloadErrorText`, and `tracePathClass`. Wire them into `ExecutionTracePanel` without changing backend trace contracts.

- [x] **Step 3: Run frontend tests**

Run:

```bash
cd frontend && npm test -- executionTimeline.test.tsx
```

Expected: PASS.

### Task 2: Batch Operation API And UI

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasBatchOperationControllerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasBatchOperationController.java`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [x] **Step 1: Write batch controller tests**

Create tests for pause, resume, archive, clone with parameter replacement, grouped filtering input, and per-item `SUCCESS`, `SKIPPED`, `FAILED` results.

- [x] **Step 2: Implement batch controller**

Expose `POST /canvas/batch/{operation}` accepting canvas ids, optional filters, parameter replacements, and reason. Return `BatchOperationResult` with per-item rows and aggregate counts.

- [x] **Step 3: Add canvas-list batch UI**

Add selection, grouped filters, operation menu, parameter replacement drawer for clone, and result drawer with success/skipped/failed counts.

- [x] **Step 4: Run backend and frontend checks**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasBatchOperationControllerTest
cd frontend && npm test -- executionTimeline.test.tsx
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017e-execution-timeline-and-batch-operations.md`
- Modify: `docs/product-evolution/plans/p2-017e-execution-timeline-and-batch-operations-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasBatchOperationControllerTest
cd frontend && npm test -- executionTimeline.test.tsx
```

Expected: PASS.

Commit boundary: no commit was created in this audit; commit and merge status remain unverified.

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasBatchOperationController.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasBatchOperationControllerTest.java frontend/src/components/canvas frontend/src/pages/canvas-list/index.tsx docs/product-evolution/specs/p2-017e-execution-timeline-and-batch-operations.md docs/product-evolution/plans/p2-017e-execution-timeline-and-batch-operations-plan.md
git commit -m "feat: add execution timeline and batch operations"
```

Expected: commit contains only timeline helpers, batch controller/UI, tests, and related docs.
