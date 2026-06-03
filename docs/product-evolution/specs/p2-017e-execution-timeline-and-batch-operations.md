# P2-017E - Execution Timeline And Batch Operations Spec

Priority: P2
Sequence: 017E
Source: `docs/optimization/bmad-product-review-2026-05.md`, `docs/optimization/optimization_list_v7.md`
Implementation plan: `../plans/p2-017e-execution-timeline-and-batch-operations-plan.md`

## Goal

Improve execution timeline inspection and add safe batch list actions.

## Current Baseline

- `ExecutionTracePanel` shows trace rows, but full error expansion, download, path highlight, and trace click-to-node are incomplete.
- Canvas list actions exist individually, but batch pause/resume/archive/clone with per-item results is missing.

## In Scope

- Timeline UI with full error expansion/download, path highlighting, and trace click-to-node.
- `CanvasBatchOperationController` for pause, resume, archive, clone with parameter replacement, grouped filtering, and per-item results.
- Frontend batch result drawer.

## Out Of Scope

- Rerun execution modes; split into P2-017D.

## Acceptance Criteria

- Frontend timeline tests cover full error display, download, path highlighting, and click-to-node.
- Backend batch tests prove per-item `SUCCESS`, `SKIPPED`, and `FAILED`.
