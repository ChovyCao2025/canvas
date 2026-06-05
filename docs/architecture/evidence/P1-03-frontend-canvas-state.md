# P1-03 Frontend Canvas State Evidence

Date: 2026-06-04

## Current State

- `frontend/src/pages/canvas-editor/index.tsx` current line count: 1581.
- Previous spec line count was 2084; repository drift before this pass was 2037.
- This pass extracted the inline undo/redo state hook into `frontend/src/pages/canvas-editor/useCanvasHistoryState.ts`.
- Added `frontend/src/pages/canvas-editor/useCanvasHistoryState.test.ts` for stable history labels.
- Extracted publish pre-check rules into `frontend/src/pages/canvas-editor/publishValidation.ts`.
- Added `frontend/src/pages/canvas-editor/publishValidation.test.ts` for trigger, scheduled audience-chain, event/MQ, and SPLIT branch validation.
- Extracted graph persistence serialization and placeholder filtering into `frontend/src/pages/canvas-editor/graphSerialization.ts`.
- Added `frontend/src/pages/canvas-editor/graphSerialization.test.ts`.
- Extracted workflow/API boundary adapters into `frontend/src/pages/canvas-editor/workflowApiAdapters.ts`.
- Added `frontend/src/pages/canvas-editor/workflowApiAdapters.test.ts` for version-list response normalization, dry-run execution ID extraction, JSON payload parsing, and editor API error messages.
- Typed `canvasApi.getVersions` as `CanvasVersionsPayload` and `canvasApi.dryRun` as `CanvasDryRunResult`.
- Updated `CanvasVersion.status` to cover the version-history status values rendered by the editor.
- Removed all `any` casts and `catch (...: any)` blocks from `frontend/src/pages/canvas-editor/index.tsx`.
- Extracted test-run and canary modal rendering into `frontend/src/pages/canvas-editor/CanvasWorkflowModals.tsx`.
- Extracted version history drawer rendering into `frontend/src/pages/canvas-editor/CanvasVersionHistoryDrawer.tsx`.
- Extracted settings modal rendering and form summary watchers into `frontend/src/pages/canvas-editor/CanvasEditorSettingsPanel.tsx`.
- Extracted dry-run test modal state and submit workflow into `frontend/src/pages/canvas-editor/useCanvasTestRunWorkflow.ts`.
- Extracted canary modal state and canary publish/promote/rollback actions into `frontend/src/pages/canvas-editor/useCanvasCanaryWorkflow.ts`.
- Extracted version history drawer state, loading, and revert actions into `frontend/src/pages/canvas-editor/useCanvasVersionHistoryWorkflow.ts`.
- Extracted publish validation and publish submit workflow into `frontend/src/pages/canvas-editor/useCanvasPublishWorkflow.ts`.
- Extracted selected node, edge insertion context, and clipboard state into `frontend/src/pages/canvas-editor/useCanvasSelectionState.ts`.
- Extracted React Flow nodes/edges, dragging node state, real/display node derivation, and branch placeholders into `frontend/src/pages/canvas-editor/useCanvasGraphState.ts`.

## State Groups Still In `index.tsx`

- Graph state: React Flow nodes/edges, dragging node state, real/display node derivation, and branch placeholders are now owned by `useCanvasGraphState`; drag/drop and connection event handlers remain in the page shell because they orchestrate React Flow events, node config patching, and dirty-state transitions.
- Selection state: selected node, selected edge insertion context, and clipboard are now owned by `useCanvasSelectionState`; selection event handlers remain in the page shell because they are coupled to React Flow interactions.
- Persistence state: dirty flag, save queue, edit version, local draft persistence, autosave timer.
- Workflow state: publish/test/canary/version-history workflows are now external. The page still decides when toolbar buttons are shown, but workflow validation, submit actions, state, and modal/drawer rendering are outside the shell.
- Settings state: settings modal open flag, save handler, form instance, and execution limits expansion. Settings modal rendering and form summary watchers are now external.

## Remaining Cast Inventory

High-risk `any` inventory:

- `rg -n "as any|\bany\b|catch \(.+: any\)" frontend/src/pages/canvas-editor frontend/src/services/api.ts frontend/src/types/index.ts -g '*.ts' -g '*.tsx' -g '!*.test.ts' -g '!*.test.tsx'` returns no matches.
- Version-list adaptation now uses `extractCanvasVersions(res.data)`.
- Dry-run execution ID extraction now uses `extractDryRunExecutionId(res.data)`.
- React Flow `isValidConnection` is typed with `IsValidConnection` instead of an `any` prop cast.
- Error catch blocks now receive `unknown` and use `editorApiErrorMessage` / `isApiConflict`.

Remaining typed casts still visible in the editor shell:

- `CanvasNodeData` casts around React Flow's untyped `Node.data`.
- `Node<CanvasNodeData>[]` casts around React Flow node arrays returned by `getNodes()`.
- DOM event target casts for keyboard handling.

Owners:

- React Flow node data casts: handler extraction follow-up if the page shell needs further reduction.
- DOM event target casts: keyboard handler extraction if it grows beyond current scope.

## Verification

Focused editor helper and service API tests:

```bash
cd frontend && npm run test -- src/services/api.test.ts src/pages/canvas-editor/workflowApiAdapters.test.ts src/pages/canvas-editor/graphSerialization.test.ts src/pages/canvas-editor/publishValidation.test.ts src/pages/canvas-editor/graphHydration.test.ts src/pages/canvas-editor/connectionInteraction.test.ts src/pages/canvas-editor/insertNode.test.ts src/pages/canvas-editor/localDraft.test.ts src/pages/canvas-editor/canvasEditorAutosave.test.tsx src/pages/canvas-editor/editorSnapshot.test.ts src/pages/canvas-editor/useCanvasHistoryState.test.ts src/pages/canvas-editor/settingsPresentation.test.ts
```

Result: 12 test files, 45 tests, 0 failures.

Full frontend tests:

```bash
cd frontend && npm run test
```

Result: 55 test files, 201 tests, 0 failures.

Frontend production build:

```bash
cd frontend && npm run build
```

Result: `tsc && vite build` completed successfully.
