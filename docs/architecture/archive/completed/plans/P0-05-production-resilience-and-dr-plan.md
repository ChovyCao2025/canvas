# Production Resilience And Disaster Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make shutdown, drain, Redis recovery, and route rebuild behavior deterministic for canvas execution, MQ consumption, scheduler triggers, direct execution, Disruptor publishing, and Groovy/background work.

**Architecture:** Add explicit application shutdown configuration, centralize execution admission behind a lifecycle gate, make async execution paths drainable, and document Redis-loss recovery with testable route/context reconciliation commands.

**Tech Stack:** Java 21, Spring Boot WebFlux, Reactor, Disruptor, Redis, RocketMQ, virtual threads, JUnit 5, Maven, YAML application profiles.

---

## Source Material

- Spec: `../specs/P0-05-production-resilience-and-dr-spec.md`
- Source package: `../../../todo/p0/production-resilience-and-dr/`
- Coverage matrix: `../../../todo/coverage-matrix.md`

## File Structure

- Shutdown config: `backend/canvas-engine/src/main/resources/application.yml`
- Lifecycle gate: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGate.java`
- In-flight tracking: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Direct execution: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Scheduler triggers: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- MQ triggers: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/MqTriggerConsumer.java`
- Disruptor: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Trace buffer: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Redis context: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Redis routes: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java`
- Redis routes: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java`
- Redis/runtime recovery: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteRecoveryService.java`
- Redis routes: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java`
- Groovy executor: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Ops command: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationShutdownConfigTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGateTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryConcurrencyTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorServiceLifecycleTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteRecoveryServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerRecoveryTest.java`
- Evidence: `docs/architecture/evidence/P0-05-production-resilience-and-dr.md`

### Task 1: Add graceful shutdown configuration

**Files:**
- Production: `backend/canvas-engine/src/main/resources/application.yml`
- Production: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationShutdownConfigTest.java`

- [x] Configure `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase` for production and local defaults.
- [x] Keep timeout values explicit so drain behavior is visible in config review.
- [x] Test that required shutdown settings are present and production profile values are not empty.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApplicationShutdownConfigTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: shutdown config tests pass and production config exposes a deterministic drain timeout.

### Task 2: Introduce a central execution lifecycle gate

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGate.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/MqTriggerConsumer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGateTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryConcurrencyTest.java`

- [x] Route direct execution, scheduled triggers, and MQ trigger admission through `ExecutionLifecycleGate`.
- [x] Track in-flight execution count and lane ownership through `InFlightExecutionRegistry`.
- [x] Reject new trigger work once shutdown begins and keep existing in-flight work visible until completion or timeout.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ExecutionLifecycleGateTest,InFlightExecutionRegistryConcurrencyTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: admission tests pass for open, closing, closed, and concurrent completion states; rejected work has a stable reason.

### Task 3: Make async chains drainable

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorServiceLifecycleTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceWriteBufferTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutorTest.java`

- [x] Ensure Disruptor publishing rejects or drains consistently after shutdown begins.
- [x] Ensure trace buffer shutdown flushes queued spans or reports dropped spans through a metric/log path.
- [x] Ensure Groovy and virtual-thread work is tracked, bounded, and drained through managed executor shutdown.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasDisruptorServiceLifecycleTest,TraceWriteBufferTest,ManagedVirtualThreadExecutorTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: shutdown-sensitive async components either complete queued work inside timeout or reject new work with observable status.

### Task 4: Define Redis recovery behavior

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/MqRouteRefreshServiceTest.java`
- Docs: `docs/architecture/evidence/P0-05-production-resilience-and-dr.md`

- [x] Specify how paused and waiting execution contexts are rebuilt or marked resumable after Redis loss.
- [x] Specify route rebuild commands for direct, scheduled, and MQ routes.
- [x] Test context persistence and route refresh behavior using the current Redis service abstractions.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ContextPersistenceServiceTest,TriggerRouteServiceTest,MqRouteRefreshServiceTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: Redis recovery evidence names the exact rebuild entry points and tests prove route/context services can reconstruct expected state.

### Task 5: Validate shutdown and disaster recovery

**Files:**
- Docs: `docs/architecture/evidence/P0-05-production-resilience-and-dr.md`
- Plan: `docs/architecture/archive/completed/plans/P0-05-production-resilience-and-dr-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P0-05-production-resilience-and-dr-spec.md`

- [x] Record stop order, drain timeout, Redis loss recovery, route rebuild, and resumed execution checks in the evidence file.
- [x] Run the complete focused resilience command.
- [x] Run the backend module suite after focused checks pass.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ApplicationShutdownConfigTest,ExecutionLifecycleGateTest,InFlightExecutionRegistryConcurrencyTest,CanvasDisruptorServiceLifecycleTest,TraceWriteBufferTest,ManagedVirtualThreadExecutorTest,ContextPersistenceServiceTest,TriggerRouteServiceTest,MqRouteRefreshServiceTest test
cd backend && mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused resilience tests and backend module tests pass; evidence contains a rehearsable shutdown and Redis recovery procedure.
