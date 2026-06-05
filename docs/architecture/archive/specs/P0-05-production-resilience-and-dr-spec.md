# Spec: Production Resilience And Disaster Recovery

Source package: `docs/architecture/todo/p0/production-resilience-and-dr/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Implemented for the P0-05 focused scope, with focused backend verification passing for graceful shutdown configuration, execution-entry shutdown gating, Disruptor drain behavior, managed background work, Redis subscription disposal, Redis route recovery, local scheduled-trigger recovery, and missing Redis execution-context handling.

Verification evidence:

- `ApplicationShutdownConfigTest`
- `ExecutionLifecycleGateTest`
- `CanvasExecutionServiceResumeTest`
- `CanvasDisruptorServiceLifecycleTest`
- `ManagedVirtualThreadExecutorTest`
- `KillSwitchSubscriberTest`
- `DagEngineLifecycleTest`
- `TriggerRouteRecoveryServiceTest`
- `OpsControllerRecoveryTest`
- `CanvasSchedulerServiceTest`
- `TriggerRouteServiceTest`
- `ContextPersistenceServiceTest`
- Command: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ApplicationShutdownConfigTest,ExecutionLifecycleGateTest,CanvasExecutionServiceResumeTest,CanvasDisruptorServiceLifecycleTest,ManagedVirtualThreadExecutorTest,KillSwitchSubscriberTest,DagEngineLifecycleTest,TraceWriteBufferTest,TriggerRouteRecoveryServiceTest,OpsControllerRecoveryTest,OpsControllerTemplateTest,CanvasSchedulerServiceTest,MqRouteRefreshServiceTest,TriggerRouteServiceTest,ContextPersistenceServiceTest test`

## Resolved Problems

- `server.shutdown: graceful` and `spring.lifecycle.timeout-per-shutdown-phase: 30s` are now configured in `application.yml`.
- `CanvasExecutionService` now uses `ExecutionLifecycleGate` to reject new trigger chains after shutdown starts and wait for already-entered execution chains to exit.
- Off-path virtual-thread work is now submitted through `ManagedVirtualThreadExecutor`, which tracks in-flight tasks, enforces a configurable capacity, rejects after close, and drains on `@PreDestroy`.
- `GroovyHandler` now shuts down its virtual-thread executor on bean destruction.
- `KillSwitchSubscriber` now disposes its Redis Pub/Sub subscription and listener container on shutdown.
- `DagEngine` now owns and disposes its special-node timeout scheduler, and DLQ side writes no longer use unmanaged Reactor `subscribe()`.
- `POST /ops/recovery/runtime-state/rebuild` rebuilds MQ, behavior, and Tagger Redis routes from published DB versions and replaces local scheduled-trigger registrations.
- Internal WAIT/timeout continuations now skip instead of creating a new execution when Redis `ExecutionContext` is missing; the original `PAUSED` execution is marked `FAILED` when the execution id is available.

## Remaining Problems

- Redis `ExecutionContext` snapshots are still not reconstructable without loss; the implemented behavior is explicit failure of affected paused executions rather than lossless reconstruction.

## Evidence

- `backend/canvas-engine/src/main/resources/application.yml`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGate.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/KillSwitchSubscriber.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteRecoveryService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- `docs/architecture/evidence/P0-05-production-resilience-and-dr.md`
- `docs/architecture/archive/reviews/architecture-supplement-review-2026-05.md` disaster-recovery section.
- `docs/architecture/archive/remediation/part7-resilience.md` shutdown section.

## Acceptance Criteria

- The service stops accepting new trigger work during shutdown.
- MQ consumption, direct execution, scheduler triggers, and disruptor publishing drain or reject consistently.
- Running executions are either completed, checkpointed, or explicitly marked resumable.
- Redis loss has a documented and tested recovery behavior for paused/resumed contexts and trigger routes.
- Shutdown behavior is covered by tests or scripted verification.
