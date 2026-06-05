# P0-04 Execution Concurrency Safety Evidence

Date: 2026-06-04

## Status

Implemented and verified in the main worktree.

## Code Evidence

- `CircuitBreakerRegistry` stores circuit state in an `AtomicReference<StateSnapshot>` so state, failure count, half-open attempts, and opened timestamp transition together: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java:66`
- `ExecutionContext.putNodeOutput()` snapshots node output and serializes grouped, flat, owner, and size updates through one synchronized block: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java:150`
- `CanvasSchedulerService.closed` is an `AtomicBoolean`, and close/register/add paths reject new work after shutdown begins: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java:114`
- `ManagedVirtualThreadExecutor` tracks off-path virtual-thread work, bounds in-flight work, rejects after shutdown, and drains during `shutdown()`: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java:24`, `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java:67`, `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java:127`
- Background executor users include `CanvasService`, `TriggerPreCheckService`, `CanvasExecutionService`, `AudienceComputeTaskRunner`, and `AudienceController`: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:84`, `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java:63`, `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java:118`, `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java:38`, `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java:75`
- `GroovyHandler` submits Groovy execution through a managed executor and shuts it down on bean destruction: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java:173`, `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java:226`

## Verification

Focused command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ManagedVirtualThreadExecutorTest,CircuitBreakerRegistryTest,ExecutionContextConcurrencyTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,CdpTagOperationServiceRetryTest,AudienceControllerTest,AudienceControllerTaskTest test
```

Focused result: PASS, 59 tests, 0 failures, 0 errors, 0 skipped.

Per-class counts observed in the focused run:

- `AudienceControllerTest`: 3 tests
- `AudienceControllerTaskTest`: 10 tests
- `ManagedVirtualThreadExecutorTest`: 2 tests
- `CanvasSchedulerServiceTest`: 6 tests
- `ExecutionContextConcurrencyTest`: 25 tests
- `CircuitBreakerRegistryTest`: 5 tests
- `AudienceComputeTaskRunnerTest`: 7 tests
- `CdpTagOperationServiceRetryTest`: 1 test

Raw virtual-thread scan:

```bash
rg -n "Thread\\.ofVirtual\\(\\)\\.start" backend/canvas-engine/src/main/java || true
```

Result: no matches.

Full backend command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Full result: PASS, 491 tests, 0 failures, 0 errors, 0 skipped.

## Notes

- No Flyway migration was required for this slice.
- The focused run includes expected negative-path log output from audience compute notification and compute failure tests; assertions still passed.
