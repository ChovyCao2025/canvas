# P1-007 - Canvas Editor Edge Projection And History Spec

Priority: P1
Sequence: 007
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p1-007-canvas-editor-edge-projection-and-history-plan.md`

## Goal

Make canvas node `bizConfig` the only persisted source for routing edges and make undo/redo snapshots immutable.

## Current Baseline

- `frontend/src/types/canvas.ts` already documents that `bizConfig` is the route source of truth.
- `frontend/src/pages/canvas-editor/outletRouting.ts` already derives, patches, and clears many outlet edge cases.
- `frontend/src/pages/canvas-editor/index.tsx` still owns independent React Flow `edges` state and stores history snapshots with shallow array copies.

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
