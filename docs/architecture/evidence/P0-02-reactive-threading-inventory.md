# P0-02 Reactive Threading And Transaction Boundaries Evidence

## Implemented Behavior

- Added `BlockingWorkScheduler` as the central adapter for blocking Reactor waits and blocking callables.
  - `call(...)` shifts callables to `Schedulers.boundedElastic()`.
  - `await(...)` / `get(...)` reject synchronous waits from Reactor/Netty event-loop-like threads.
- Added `TrackedReactiveTaskRegistry` for fire-and-forget Reactor work.
  - Tracks `Mono` and `Publisher` tasks by name.
  - Exposes in-flight count, drain, shutdown disposal, and shutdown rejection behavior.
- Removed scattered direct `.block()` from the first runtime batch:
  - `TagImportSourceService` remote tag import source pull.
  - `AudienceEvaluationContextFetcher` Tagger context query.
  - `AudienceBatchComputeService` Tagger seed-user pagination.
  - `CanvasSchedulerService` scheduled user-source calls.
- Removed `Thread.sleep(...)` from main sources.
  - Route mutation lock wait, route initializer wait, audience lock retry, and Disruptor shutdown drain now use `LockSupport.parkNanos(...)` with interrupt checks.
- Replaced business-path fire-and-forget `.subscribe()` with tracked execution:
  - `CanvasSchedulerService` jitter timers and scheduled trigger dispatch.
  - `DagEngine` HUB/AGGREGATE/THRESHOLD timeout timers and timeout continuation work.
  - `WaitResumeService` wait-resume trigger dispatch.
  - `CanvasExecutionRequestExecutor` running-heartbeat task.
- Verified transaction side-effect boundaries:
  - `CanvasTransactionService` remains DB-only for publish/offline/kill/archive state changes.
  - `CanvasService` runs Redis route, scheduler, cache, quota, and Groovy precompile side effects after DB state changes.
  - `CanvasOpsService.kill` only publishes kill signals and runtime cleanup after `killDb(...)` succeeds.
  - Rollback-path tests prove external side effects are not triggered when the DB phase fails.

## Current Scan Classification

Command:

```bash
rg -n "\.block\(|\.subscribe\(|Thread\.sleep|@Transactional" backend/canvas-engine/src/main/java
```

`Thread.sleep`: no matches.

Direct `.block()`:

- `BlockingWorkScheduler.await(...)` and `BlockingWorkScheduler.get(...)`: accepted central adapter. This is the only allowed synchronous Reactor wait surface; it fails fast on event-loop threads and is covered by `BlockingWorkSchedulerTest`.

Direct `.subscribe()`:

- `TrackedReactiveTaskRegistry.submit(...)`: accepted central registry for named fire-and-forget tasks. Covered by success, failure, cancellation, drain, disposal, and shutdown-rejection tests.
- `KillSwitchSubscriber.subscribe()`: accepted lifecycle Redis Pub/Sub subscription. The `Disposable` is stored and disposed in `@PreDestroy`; listener container is destroyed on shutdown.
- `NotificationRealtimeService.subscribeRedisChannel()`: accepted lifecycle Redis Pub/Sub subscription. The `Disposable` is stored and disposed in `@PreDestroy`; listener container and local sinks are closed.
- `CanvasDisruptorService.subscribeTracked(...)`: accepted Disruptor dispatch bridge. It increments/decrements `inFlight` around the subscription and shutdown drains `inFlight` before stopping workers.

Transactional boundaries:

- `CanvasTransactionService`: DB-only lifecycle mutations; external side effects are intentionally outside these transactions.
- `CanvasService`: draft/version DB writes are transactional; publish/offline/archive side effects are executed after the DB phase using repairable runtime state.
- `CanvasOpsService`: save/canary/rollback/clone operations remain synchronous DB transactions; kill side effects are after `killDb(...)`.
- `CanvasExampleSeeder`, `CdpTagService`, and `TagImportService`: accepted synchronous DB transactions; no Redis/scheduler/MQ fire-and-forget side effect is mixed into those transaction methods in the current P0-02 scope.

## Converted Runtime Paths

- `TagImportSourceService.executeRequest(...)`: direct remote `.block()` replaced with `BlockingWorkScheduler.await(...)`.
- `AudienceEvaluationContextFetcher.fetch(...)`: direct Tagger `.block()` replaced with `BlockingWorkScheduler.await(...)`.
- `AudienceBatchComputeService.computeViaTaggerApi(...)`: direct paged `.block()` replaced with `BlockingWorkScheduler.await(...)`.
- `CanvasSchedulerService.resolveUserIds(...)`: scheduled user-source `.block()` replaced with `BlockingWorkScheduler.await(...)`.
- `CanvasSchedulerService.scheduleTriggerWithJitter(...)` and `dispatchScheduledTrigger(...)`: direct `.subscribe()` replaced with `TrackedReactiveTaskRegistry.submit(...)`.
- `DagEngine`: special-node timeout timer and timeout-continuation `.subscribe()` calls replaced with `TrackedReactiveTaskRegistry.submit(...)`.
- `WaitResumeService.trigger(...)`: wait-resume fire-and-forget `.subscribe()` replaced with `TrackedReactiveTaskRegistry.submit(...)`.
- `CanvasExecutionRequestExecutor.startHeartbeat(...)`: heartbeat `.subscribe()` replaced with `TrackedReactiveTaskRegistry.submit(...)`.
- `TriggerRouteService`, `CanvasRouteInitializer`, `AudienceComputeTaskRunner`, and `CanvasDisruptorService`: `Thread.sleep(...)` replaced with `LockSupport.parkNanos(...)` and interrupt checks.

## Focused Verification

Commands:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=BlockingWorkSchedulerTest,TrackedReactiveTaskRegistryTest test
```

Result: 7 tests, 0 failures, 0 errors.

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=BlockingWorkSchedulerTest,TrackedReactiveTaskRegistryTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,TriggerRouteServiceTest test
```

Result: 22 tests, 0 failures, 0 errors.

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=BlockingWorkSchedulerTest,TrackedReactiveTaskRegistryTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,TriggerRouteServiceTest,DagEngineLifecycleTest,WaitResumeServiceTest,CanvasExecutionRequestExecutorTest,CanvasTransactionAnnotationTest,CanvasTransactionSideEffectTest test
```

Result: 33 tests, 0 failures, 0 errors.

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CanvasTransactionAnnotationTest,CanvasTransactionSideEffectTest test
```

Result: 4 tests, 0 failures, 0 errors.

Additional focused checks:

- `DagEngineLifecycleTest`, `BlockingWorkSchedulerTest`, `TrackedReactiveTaskRegistryTest`: 9 tests, 0 failures, 0 errors.
- `WaitResumeServiceTest`, `TrackedReactiveTaskRegistryTest`: 7 tests, 0 failures, 0 errors.
- `CanvasExecutionRequestExecutorTest`, `TrackedReactiveTaskRegistryTest`: 6 tests, 0 failures, 0 errors.

Module command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Result: 449 tests, 0 failures, 0 errors.

## Remaining Risk

- The codebase is still a reactive WebFlux app over a mostly blocking MyBatis/JDBC model. Controllers and handler paths still rely on boundedElastic or explicit background execution rather than a fully reactive persistence stack.
- `CanvasDisruptorService.subscribeTracked(...)` remains a local tracked bridge instead of using `TrackedReactiveTaskRegistry`; it is classified as accepted because it already tracks `inFlight` and drains on shutdown.
- Transaction side-effect handling is currently a two-phase repairable-state pattern, not a durable outbox. If Redis/scheduler/cache side effects fail after commit, recovery relies on later rebuild/repair paths rather than guaranteed outbox replay.
