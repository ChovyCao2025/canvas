# Spec: Canvas State And Data Consistency

Source package: `docs/architecture/todo/p0/canvas-state-data-consistency/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Main worktree status: implemented and verified on 2026-06-04.

Verification evidence:

- Evidence note: `docs/architecture/evidence/P0-03-canvas-state-data-consistency.md`
- Focused command: `cd backend && mvn -pl canvas-engine -Dtest=CanvasStateTransitionPolicyTest,CanvasTransactionServiceStateTest,CanvasServiceDraftUpdateStateTest,CanvasOpsServiceStateTest,CanvasVersionCleanupJobTest,TriggerPreCheckServiceQuotaReconciliationTest,MqRouteRefreshServiceTest test`
- Focused result: 18 tests, 0 failures, 0 errors, 0 skipped.
- Full command: `cd backend && mvn -pl canvas-engine test`
- Full result: 466 tests, 0 failures, 0 errors, 0 skipped.

Focused tests:

- `CanvasStateTransitionPolicyTest`
- `CanvasTransactionServiceStateTest`
- `CanvasServiceDraftUpdateStateTest`
- `CanvasOpsServiceStateTest`
- `CanvasVersionCleanupJobTest`
- `TriggerPreCheckServiceQuotaReconciliationTest`
- `MqRouteRefreshServiceTest`

## Problems

Original audit findings:

- `CanvasService.publish()` did not guard previous canvas state before publishing.
- `CanvasTransactionService.publishDb()` set status to PUBLISHED regardless of whether the previous status was KILLED, ARCHIVED, or otherwise invalid.
- `CanvasService.updateDraft()` updated name, validity, quota, cooldown, trigger type, and graph draft without checking whether the canvas was currently published or killed.
- Version cleanup could clear old published `graphJson`; this needed runtime-reference safeguards before it was safe in long-running/resume flows.
- Redis quota and route cleanup was manually coordinated and remained repair-sensitive.

Implemented resolution:

- `CanvasStateTransitionPolicy` centralizes lifecycle transition validation and treats KILLED/ARCHIVED as terminal states.
- `CanvasService`, `CanvasOpsService`, and `CanvasTransactionService` now enforce lifecycle guards before publish, offline, kill, archive, canary, rollback, draft update, and version revert operations.
- Published runtime policy fields are immutable through draft update while metadata and draft graph edits remain allowed.
- Version cleanup now preserves published, previous, canary, running/paused execution, and active wait-subscription references before clearing `graphJson`.
- Redis quota and trigger route repair paths are available through `TriggerPreCheckService.reconcileInactiveCanvasQuotas()` and `MqRouteRefreshService.rebuildTriggerRoutes()`.

## Evidence

- `CanvasService.java:132-147`
- `CanvasService.java:199-239`
- `CanvasTransactionService.java:47-65`
- `CanvasTransactionService.java:92-127`
- `CanvasVersionCleanupJob.java:60-80`
- `TriggerPreCheckService.java` quota comments around permanent or valid-end-based Redis keys.

## Acceptance Criteria

- [x] Canvas state transitions are enforced through a single explicit state machine.
- [x] KILLED is terminal unless an explicit clone/recovery workflow is introduced.
- [x] Published runtime constraints are immutable for running executions or versioned with clear semantics.
- [x] Version cleanup cannot break resume, audit, rollback, or running execution reconstruction.
- [x] Redis/DB route and quota state has an idempotent reconciliation or repair path.
