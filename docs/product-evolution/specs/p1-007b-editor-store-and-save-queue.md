# P1-007B - Editor Store And Save Queue Spec

Priority: P1
Sequence: 007B
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/production-readiness-checklist.md`
Implementation plan: `../plans/p1-007b-editor-store-and-save-queue-plan.md`

## Goal

Move high-churn canvas editor state into a tested store and add a bounded save queue that prevents unbounded retry loops and preserves unsaved changes.

## Current Baseline

- `frontend/src/pages/canvas-editor/index.tsx` owns selection, graph state, history, save status, modals, reload behavior, and many side effects.
- Existing tests cover local drafts, graph hydration, insertion, connection interaction, and several presentation helpers.
- `frontend/package.json` does not include `zustand` or `immer`.

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
