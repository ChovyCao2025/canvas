# Spec: Canvas State And Data Consistency

Source package: `docs/architecture/todo/p0/canvas-state-data-consistency/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Confirmed, with some prior route-cleanup concerns partially improved.

## Problems

- `CanvasService.publish()` does not guard previous canvas state before publishing.
- `CanvasTransactionService.publishDb()` sets status to PUBLISHED regardless of whether the previous status was KILLED, ARCHIVED, or otherwise invalid.
- `CanvasService.updateDraft()` updates name, validity, quota, cooldown, trigger type, and graph draft without checking whether the canvas is currently published or killed.
- Version cleanup can clear old published `graphJson`; this needs runtime-reference safeguards before it is safe in long-running/resume flows.
- Redis quota and route cleanup is manually coordinated and remains repair-sensitive.

## Evidence

- `CanvasService.java:132-147`
- `CanvasService.java:199-239`
- `CanvasTransactionService.java:47-65`
- `CanvasTransactionService.java:92-127`
- `CanvasVersionCleanupJob.java:60-80`
- `TriggerPreCheckService.java` quota comments around permanent or valid-end-based Redis keys.

## Acceptance Criteria

- Canvas state transitions are enforced through a single explicit state machine.
- KILLED is terminal unless an explicit clone/recovery workflow is introduced.
- Published runtime constraints are immutable for running executions or versioned with clear semantics.
- Version cleanup cannot break resume, audit, rollback, or running execution reconstruction.
- Redis/DB route and quota state has an idempotent reconciliation or repair path.
