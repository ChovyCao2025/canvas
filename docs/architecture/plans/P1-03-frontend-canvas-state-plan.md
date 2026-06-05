# Frontend Canvas State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the canvas editor page into focused state hooks and components with typed service models for graph state, selection, history, publish/test workflows, settings, and local drafts.

**Architecture:** Characterize editor behavior first, then move cohesive state groups out of `frontend/src/pages/canvas-editor/index.tsx` without changing user-visible behavior. Keep graph operations, workflow modals, local draft persistence, and API models behind named hooks and pure helpers that can be tested independently.

**Tech Stack:** React 18, TypeScript, Vite, Vitest, React Flow, Ant Design, ES modules.

---

## Source Material

- Spec: `../specs/P1-03-frontend-canvas-state-spec.md`
- Source package: `../todo/p1/frontend-canvas-state/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Editor shell: `frontend/src/pages/canvas-editor/index.tsx`
- Existing graph helpers: `frontend/src/pages/canvas-editor/graphHydration.ts`
- Existing graph helpers: `frontend/src/pages/canvas-editor/connectionInteraction.ts`
- Existing graph helpers: `frontend/src/pages/canvas-editor/insertNode.ts`
- Existing history/local draft helpers: `frontend/src/pages/canvas-editor/localDraft.ts`
- Existing autosave helpers: `frontend/src/pages/canvas-editor/canvasEditorAutosave.ts`
- New hook: `frontend/src/pages/canvas-editor/useCanvasGraphState.ts`
- New hook: `frontend/src/pages/canvas-editor/useCanvasSelectionState.ts`
- New hook: `frontend/src/pages/canvas-editor/useCanvasHistoryState.ts`
- New hook: `frontend/src/pages/canvas-editor/useCanvasPublishWorkflow.ts`
- New hook: `frontend/src/pages/canvas-editor/useCanvasTestRunWorkflow.ts`
- New component: `frontend/src/pages/canvas-editor/CanvasEditorToolbar.tsx`
- New component: `frontend/src/pages/canvas-editor/CanvasEditorSettingsPanel.tsx`
- New component: `frontend/src/pages/canvas-editor/CanvasWorkflowModals.tsx`
- Service model: `frontend/src/services/api.ts`
- Type model: `frontend/src/types/canvas.ts`
- Tests: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Tests: `frontend/src/pages/canvas-editor/connectionInteraction.test.ts`
- Tests: `frontend/src/pages/canvas-editor/insertNode.test.ts`
- Tests: `frontend/src/pages/canvas-editor/localDraft.test.ts`
- Tests: `frontend/src/pages/canvas-editor/canvasEditorAutosave.test.tsx`
- Tests: `frontend/src/pages/canvas-editor/useCanvasGraphState.test.tsx`
- Tests: `frontend/src/pages/canvas-editor/useCanvasWorkflow.test.tsx`
- Evidence: `docs/architecture/evidence/P1-03-frontend-canvas-state.md`

### Task 1: Add characterization tests for editor workflows

**Files:**
- Production: `frontend/src/pages/canvas-editor/index.tsx`
- Production: `frontend/src/pages/canvas-editor/graphHydration.ts`
- Production: `frontend/src/pages/canvas-editor/connectionInteraction.ts`
- Production: `frontend/src/pages/canvas-editor/insertNode.ts`
- Production: `frontend/src/pages/canvas-editor/localDraft.ts`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Test: `frontend/src/pages/canvas-editor/connectionInteraction.test.ts`
- Test: `frontend/src/pages/canvas-editor/insertNode.test.ts`
- Test: `frontend/src/pages/canvas-editor/localDraft.test.ts`
- Test: `frontend/src/pages/canvas-editor/canvasEditorAutosave.test.tsx`
- Docs: `docs/architecture/evidence/P1-03-frontend-canvas-state.md`

- [x] Cover editor load hydration, graph edit, publish pre-check, test run payload, autosave, and local draft restore.
- [x] Record current editor state groups and line count in the evidence file.
- [x] Keep tests focused on pure helpers and stable UI behavior before extraction begins.

Run:

```bash
cd frontend && npm run test -- src/pages/canvas-editor/graphHydration.test.ts src/pages/canvas-editor/connectionInteraction.test.ts src/pages/canvas-editor/insertNode.test.ts src/pages/canvas-editor/localDraft.test.ts src/pages/canvas-editor/canvasEditorAutosave.test.tsx
wc -l frontend/src/pages/canvas-editor/index.tsx
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: existing editor behavior is covered before state extraction; evidence names the state groups to move.

### Task 2: Extract graph, selection, and history hooks

**Files:**
- Production: `frontend/src/pages/canvas-editor/index.tsx`
- Production: `frontend/src/pages/canvas-editor/useCanvasGraphState.ts`
- Production: `frontend/src/pages/canvas-editor/useCanvasSelectionState.ts`
- Production: `frontend/src/pages/canvas-editor/useCanvasHistoryState.ts`
- Production: `frontend/src/pages/canvas-editor/graphHydration.ts`
- Production: `frontend/src/pages/canvas-editor/connectionInteraction.ts`
- Production: `frontend/src/pages/canvas-editor/insertNode.ts`
- Test: `frontend/src/pages/canvas-editor/useCanvasGraphState.test.tsx`
- Test: `frontend/src/pages/canvas-editor/connectionInteraction.test.ts`
- Test: `frontend/src/pages/canvas-editor/insertNode.test.ts`

- [x] Move nodes, edges, selected node, selected edge, insert placeholder, and branch connection state into explicit hooks.
- [x] Move undo/redo snapshots into `useCanvasHistoryState`.
- [x] Keep `index.tsx` as orchestration glue for React Flow and extracted hooks.

Run:

```bash
cd frontend && npm run test -- src/pages/canvas-editor/useCanvasGraphState.test.tsx src/pages/canvas-editor/connectionInteraction.test.ts src/pages/canvas-editor/insertNode.test.ts
cd frontend && npm run build
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: graph hook tests and frontend build pass; graph editing, selection, and undo state are no longer owned directly by the page shell.

### Task 3: Extract publish, test, canary, and settings workflows

**Files:**
- Production: `frontend/src/pages/canvas-editor/index.tsx`
- Production: `frontend/src/pages/canvas-editor/useCanvasPublishWorkflow.ts`
- Production: `frontend/src/pages/canvas-editor/useCanvasTestRunWorkflow.ts`
- Production: `frontend/src/pages/canvas-editor/CanvasWorkflowModals.tsx`
- Production: `frontend/src/pages/canvas-editor/CanvasEditorSettingsPanel.tsx`
- Production: `frontend/src/pages/canvas-editor/settingsPresentation.ts`
- Production: `frontend/src/pages/canvas-editor/settingsPanel.css`
- Test: `frontend/src/pages/canvas-editor/useCanvasWorkflow.test.tsx`
- Test: `frontend/src/pages/canvas-editor/settingsPresentation.test.ts`

- [x] Move publish validation, publish submit, test execution, canary workflow, and modal state into workflow hooks/components.
- [x] Move settings panel rendering into a focused component with typed props.
- [x] Keep API request construction tested through pure helpers or hook tests.

Run:

```bash
cd frontend && npm run test -- src/pages/canvas-editor/useCanvasWorkflow.test.tsx src/pages/canvas-editor/settingsPresentation.test.ts
cd frontend && npm run build
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: workflow tests and frontend build pass; publish/test/canary/settings state is isolated from graph editing state.

### Task 4: Type service calls and editor data models

**Files:**
- Production: `frontend/src/services/api.ts`
- Production: `frontend/src/types/canvas.ts`
- Production: `frontend/src/types/index.ts`
- Production: `frontend/src/pages/canvas-editor/index.tsx`
- Production: `frontend/src/pages/canvas-editor/useCanvasPublishWorkflow.ts`
- Production: `frontend/src/pages/canvas-editor/useCanvasTestRunWorkflow.ts`
- Test: `frontend/src/services/api.test.ts`
- Test: `frontend/src/pages/canvas-editor/useCanvasWorkflow.test.tsx`

- [x] Replace high-risk canvas editor `any` casts with typed canvas graph, node config, publish, test execution, and canary models.
- [x] Align frontend request/response types with backend API contract work.
- [x] Keep remaining casts documented in the evidence file with a removal owner.

Run:

```bash
cd frontend && npm run test -- src/services/api.test.ts src/pages/canvas-editor/useCanvasWorkflow.test.tsx
cd frontend && npm run build
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: frontend service tests and build pass; editor request and response models are explicit for graph, publish, test, and canary operations.

### Task 5: Validate frontend canvas state boundaries

**Files:**
- Docs: `docs/architecture/evidence/P1-03-frontend-canvas-state.md`
- Plan: `docs/architecture/plans/P1-03-frontend-canvas-state-plan.md`
- Spec: `docs/architecture/specs/P1-03-frontend-canvas-state-spec.md`

- [x] Run the canvas editor focused test set.
- [x] Run all frontend tests and production build.
- [x] Record final editor line count, extracted hooks/components, and remaining cast inventory in evidence.

Run:

```bash
cd frontend && npm run test -- src/pages/canvas-editor/graphHydration.test.ts src/pages/canvas-editor/connectionInteraction.test.ts src/pages/canvas-editor/insertNode.test.ts src/pages/canvas-editor/localDraft.test.ts src/pages/canvas-editor/canvasEditorAutosave.test.tsx src/pages/canvas-editor/useCanvasGraphState.test.tsx src/pages/canvas-editor/useCanvasWorkflow.test.tsx src/pages/canvas-editor/settingsPresentation.test.ts src/services/api.test.ts
cd frontend && npm run test
cd frontend && npm run build
wc -l frontend/src/pages/canvas-editor/index.tsx
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused editor tests, full frontend tests, and build pass; evidence shows graph, selection, history, workflow, and settings state owned by named hooks/components.
