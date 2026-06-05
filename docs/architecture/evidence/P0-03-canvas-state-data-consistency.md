# P0-03 Canvas State And Data Consistency Evidence

Date: 2026-06-04

## Status

Implemented and verified in the main worktree.

## Code Evidence

- `CanvasStateTransitionPolicy` centralizes canvas lifecycle validation and rejects illegal KILLED/ARCHIVED transitions: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicy.java:13`
- `CanvasService.publish()` validates the current canvas state before publishing: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:250`
- `CanvasService.updateDraft()` and `revertToVersion()` reject terminal-state edits: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:174`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:495`
- `CanvasService.invalidateRuntimeCanvas()` provides a shared runtime cache invalidation path for post-commit operations: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:448`
- `CanvasTransactionService` validates publish, offline, kill, and archive DB transitions inside the transactional boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java:57`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java:105`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java:132`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java:155`
- `CanvasOpsService` invalidates runtime canvas cache after canary/rollback commits: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java:398`
- `CanvasVersionCleanupJob` preserves published, previous, canary, running/paused execution, and active wait-subscription version references before clearing old `graphJson`: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java:88`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java:110`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java:137`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java:149`
- `TriggerPreCheckService.reconcileInactiveCanvasQuotas()` adds a quota repair path for inactive canvases: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java:291`
- `MqRouteRefreshService.rebuildTriggerRoutes()` rebuilds MQ, behavior, and tagger trigger routes; `rebuildMqRoutes()` remains a compatibility entrypoint: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java:48`, `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java:87`

## Verification

Focused command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CanvasStateTransitionPolicyTest,CanvasTransactionServiceStateTest,CanvasServiceDraftUpdateStateTest,CanvasOpsServiceStateTest,CanvasVersionCleanupJobTest,TriggerPreCheckServiceQuotaReconciliationTest,MqRouteRefreshServiceTest test
```

Focused result: PASS, 18 tests, 0 failures, 0 errors, 0 skipped.

Full backend command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Full result: PASS, 466 tests, 0 failures, 0 errors, 0 skipped.

Additional targeted regression:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CanvasTransactionSideEffectTest test
```

Result: PASS, 3 tests, 0 failures, 0 errors, 0 skipped.

## Notes

- `CanvasTransactionSideEffectTest.publishDoesNotRunExternalSideEffectsWhenDbPhaseFails` needed its test fixture canvas status set to DRAFT so the new state-machine guard would not preempt the intended DB rollback assertion.
- No Flyway migration was required for this slice.
