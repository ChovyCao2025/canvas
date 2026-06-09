# P1-007 - Canvas Editor Edge Projection And History Spec

Priority: P1
Sequence: 007
Source: `docs/optimization/archive/production-design-gaps.md`, `docs/optimization/archive/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p1-007-canvas-editor-edge-projection-and-history-plan.md`

## Implementation Status

Implemented on 2026-06-05. The repo already had `editorSnapshot.ts` and history hook scaffolding, so this slice completed the missing projection guard by deriving display edges from node `bizConfig` in `useCanvasGraphState.ts`. Commit step was not executed because the user requested no commit.

## Goal

Make canvas node `bizConfig` the only persisted source for routing edges and make undo/redo snapshots immutable.

## Current Baseline

- `frontend/src/types/canvas.ts` already documents that `bizConfig` is the route source of truth.
- `frontend/src/pages/canvas-editor/outletRouting.ts` already derives, patches, and clears many outlet edge cases.
- Implemented on 2026-06-05: displayed React Flow edges are projected from node `bizConfig`, stale edge state is ignored, and undo/redo snapshots are deep cloned.

## In Scope

- Keep and harden `outletRouting.ts` as the projection boundary.
- Add missing tests for stale edge removal, snapshot deep cloning, and config-panel route consistency.
- Derive displayed React Flow edges from current node `bizConfig` after load, connect, delete, paste, and node delete flows.
- Store undo/redo snapshots with deep-cloned nodes and derived edges.

## Out Of Scope

- Zustand editor store and save queue; split into P1-007B.
- HTTP client, request cancellation, retry, and runtime schemas; split into P1-007C.
- React Flow replacement.

## Functional Requirements

1. Connecting an edge must update the source node `bizConfig`.
2. Deleting an edge must clear the matching `bizConfig` target and remove stale display edges.
3. React Flow display edges must be derived from node config instead of persisted as a second business state.
4. Undo/redo snapshots must not share node or `bizConfig` references with later edits.
5. Existing outlet routing tests must remain green.

## Acceptance Criteria

- Tests prove direct, dynamic, branch, START, and DIRECT_CALL edge derivation.
- Tests prove stale display edges disappear when `bizConfig` no longer references them.
- Tests prove snapshot mutation after capture does not mutate history.
- Editor regression tests for hydration, insertion, outlet routing, connection interaction, and clipboard still pass.

## Implementation Notes

- `frontend/src/pages/canvas-editor/useCanvasGraphState.ts` now derives routed display edges with `deriveEdges(buildBackendNodesFromFlowNodes(realNodes))`.
- `useEdgesState` remains only as a React Flow interaction compatibility layer; it is not a durable business edge source.
- `deleteEdgeById` resolves the edge from `displayEdges` before clearing `bizConfig`, so toolbar deletion works with derived edges.
- `frontend/src/pages/canvas-editor/editorSnapshot.ts` deep clones nodes, nested `bizConfig`, and edges for undo/redo.

## Verification

- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- graphHydration.test.ts graphReloadKey.test.ts insertNode.test.ts outletRouting.test.ts connectionInteraction.test.ts settingsPresentation.test.ts localDraft.test.ts canvasEditorClipboard.test.tsx useCanvasGraphState.test.ts editorSnapshot.test.ts useCanvasHistoryState.test.ts` passed: 11 files, 51 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.
