# Spec: Execution Concurrency Safety

Source package: `docs/architecture/active/reviewed-packages/p0/execution-concurrency-safety/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented and verified in the main worktree on 2026-06-04.

Verification evidence:

- Evidence note: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`
- `CircuitBreakerRegistryTest`
- `ExecutionContextConcurrencyTest`
- `CanvasSchedulerServiceTest`
- `ManagedVirtualThreadExecutorTest`
- `AudienceComputeTaskRunnerTest`
- `CdpTagOperationServiceRetryTest`
- `AudienceControllerTest`
- `AudienceControllerTaskTest`
- Focused command: `cd backend && mvn -pl canvas-engine -Dtest=ManagedVirtualThreadExecutorTest,CircuitBreakerRegistryTest,ExecutionContextConcurrencyTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,CdpTagOperationServiceRetryTest,AudienceControllerTest,AudienceControllerTaskTest test`
- Focused result: 59 tests, 0 failures, 0 errors, 0 skipped.
- Full command: `cd backend && mvn -pl canvas-engine test`
- Full result: 491 tests, 0 failures, 0 errors, 0 skipped.
- Raw virtual-thread scan: `rg -n "Thread\\.ofVirtual\\(\\)\\.start" backend/canvas-engine/src/main/java || true` returned no matches.

## Resolved Problems

- `CircuitBreakerRegistry` now stores state, failure count, half-open attempts, and opened timestamp in one immutable `AtomicReference` snapshot with CAS transitions.
- `ExecutionContext.putNodeOutput()` now snapshots node output, serializes grouped/flat/size updates, exposes node-qualified flat keys, and clears stale flat keys without deleting newer owners.
- `CanvasSchedulerService.closed` is now an `AtomicBoolean`, and close rejects new pending jitter groups while terminating existing groups.
- Scheduler lifecycle, circuit breaker races, and execution-context output collisions have focused concurrency tests.
- Raw runtime `Thread.ofVirtual().start(...)` calls were replaced by `ManagedVirtualThreadExecutor`, which bounds in-flight work, tracks running tasks, rejects after shutdown, and drains on `@PreDestroy`.
- `GroovyHandler` now shuts down its virtual-thread executor on bean destruction.

## Evidence

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`

## Acceptance Criteria

- [x] Circuit breaker transitions are atomic and deterministic under concurrent success/failure/check calls.
- [x] Execution context writes either become atomic snapshots or are confined to a single execution lane with documented ownership.
- [x] Scheduler lifecycle state is thread-safe and covered by close/register/add race tests.
- [x] Background virtual-thread work is tracked, bounded, and shut down cleanly.
