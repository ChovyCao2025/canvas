# P1-007B - Editor Store And Save Queue Spec

Priority: P1
Sequence: 007B
Source: `docs/optimization/archive/production-design-gaps.md`, `docs/optimization/archive/production-readiness-checklist.md`
Implementation plan: `../plans/p1-007b-editor-store-and-save-queue-plan.md`

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

## Goal

Move high-churn canvas editor state into a tested store and add a bounded save queue that prevents unbounded retry loops and preserves unsaved changes.

## Current Baseline

- Implemented on 2026-06-05: `frontend/package.json` now includes direct `zustand` and `immer` dependencies.
- `frontend/src/pages/canvas-editor/editorStore.ts` owns selected node id, mirrored graph snapshots, dirty state, save status, modal state, conflict state, and undo/redo history labels.
- `frontend/src/pages/canvas-editor/saveQueue.ts` owns save coalescing, bounded retry, exponential backoff, conflict detection, and retry exhaustion state.
- `frontend/src/pages/canvas-editor/index.tsx` still owns the React Flow graph rendering workflow, but high-churn editor state and save retry state are now delegated to the tested store and queue.

## In Scope

- Add `zustand` and `immer`.
- Create `editorStore.ts` for selected node id, graph nodes, dirty state, history labels, modal state, save state, and conflict state.
- Create `saveQueue.ts` with coalescing, bounded retries, exponential backoff, transient recovery, 409 conflict handling, and retry exhaustion state.
- Integrate store and queue into `canvas-editor/index.tsx` in small steps after tests exist.

## Out Of Scope

- Edge projection and immutable snapshot foundation; P1-007 must land first.
- HTTP client cancellation/dedupe/runtime schemas; split into P1-007C.
- Realtime collaboration and CRDT.

## Functional Requirements

1. Rapid edits must coalesce into the latest pending save payload.
2. Retry count must be bounded and visible in save state.
3. A 409 response must preserve unsaved changes and expose conflict state.
4. Selector subscriptions must avoid rerendering unrelated editor slices.
5. Undo/redo must operate through the store without mutating captured snapshots.

## Acceptance Criteria

- Store tests cover selected node, graph update, undo, redo, dirty state, save state, modal state, and selector isolation.
- Save queue tests cover coalescing, retry count, backoff, recovery, conflict, and retry exhaustion.
- Existing local draft and editor interaction tests remain green.

## Implementation Notes

- `editorStore.ts` uses a vanilla Zustand store with `subscribeWithSelector` and Immer updates; selector tests prove unrelated modal/save/dirty changes do not notify selected-node subscribers.
- Undo/redo snapshots are cloned through `cloneEditorSnapshot`, and the page mirrors current React Flow nodes/edges into the store before history restoration.
- `saveQueue.ts` exposes queue state through `getState()` and `onStateChange`, coalesces rapid edits to the latest payload, drains pending edits after active saves, and stops after the configured retry budget.
- The editor maps queue `retrying`, `failed`, and `conflict` states into visible Ant Design alerts. Conflicts and failures preserve local drafts instead of clearing dirty state.
- `canvasApi.update` accepts an optional `AbortSignal`, preparing this slice for the P1-007C HTTP cancellation work.

## Verification

- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- editorStore.test.ts saveQueue.test.ts` passed: 2 files, 9 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- editorStore.test.ts saveQueue.test.ts localDraft.test.ts canvasNameUpdate.test.ts graphHydration.test.ts connectionInteraction.test.ts useCanvasHistoryState.test.ts useCanvasGraphState.test.ts` passed: 8 files, 29 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.
