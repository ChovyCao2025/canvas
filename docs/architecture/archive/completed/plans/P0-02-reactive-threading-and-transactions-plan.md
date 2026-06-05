# Reactive Threading And Transaction Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep blocking JDBC, Redis, HTTP, MQ, and sleep-based waits off Netty event-loop threads while making asynchronous side effects explicit, drainable, and transaction-aware.

**Architecture:** First capture every `.block()`, `.subscribe()`, `Thread.sleep()`, and `@Transactional` occurrence in an evidence inventory. Then convert runtime paths to bounded blocking adapters or tracked background execution, and separate DB transactions from Redis, scheduler, and MQ side effects through after-commit orchestration or repairable state transitions.

**Tech Stack:** Java 21, Spring Boot WebFlux, Reactor, MyBatis-Plus, Redis, RocketMQ, JUnit 5, StepVerifier, Maven.

---

## Source Material

- Spec: `../specs/P0-02-reactive-threading-and-transactions-spec.md`
- Source package: `../../../todo/p0/reactive-threading-and-transactions/`
- Coverage matrix: `../../../todo/coverage-matrix.md`

## File Structure

- App mode: `backend/canvas-engine/src/main/resources/application.yml`
- Inventory: `docs/architecture/evidence/P0-02-reactive-threading-inventory.md`
- Blocking adapter: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/reactor/BlockingWorkScheduler.java`
- Background registry: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/reactor/TrackedReactiveTaskRegistry.java`
- Transaction side effects: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Transaction side effects: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Transaction side effects: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceEvaluationContextFetcher.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java`
- Runtime path: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/BlockingWorkSchedulerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/TrackedReactiveTaskRegistryTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteServiceTest.java`

### Task 1: Inventory blocking and transactional boundaries

**Files:**
- Docs: `docs/architecture/evidence/P0-02-reactive-threading-inventory.md`
- Spec: `docs/architecture/archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md`
- Plan: `docs/architecture/archive/completed/plans/P0-02-reactive-threading-and-transactions-plan.md`

- [x] Record each `.block()`, `.subscribe()`, `Thread.sleep()`, and `@Transactional` occurrence with class, method, caller type, and classification.
- [x] Mark each occurrence as accepted, scheduler-wrapped, redesigned, tracked background execution, or transaction-side-effect work.
- [x] Identify the first runtime batch: scheduler trigger execution, audience fetching, tag import source metadata calls, route waits, and canvas publish/offline/kill side effects.

Run:

```bash
rg -n "\\.block\\(|\\.subscribe\\(|Thread\\.sleep|@Transactional" backend/canvas-engine/src/main/java > /tmp/p0-02-reactive-scan.txt
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: the evidence file maps every scan hit to an owner and next action; no runtime conversion starts without this inventory.

### Task 2: Introduce managed blocking and background execution primitives

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/reactor/BlockingWorkScheduler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/reactor/TrackedReactiveTaskRegistry.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/BlockingWorkSchedulerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/TrackedReactiveTaskRegistryTest.java`

- [x] Add a small adapter that runs blocking callables on `Schedulers.boundedElastic()` and asserts they are not executed on Netty event-loop threads.
- [x] Add a registry for background `Disposable`, `Mono`, or `Publisher` work with names, lifecycle state, and drain/shutdown behavior.
- [x] Cover success, failure, cancellation, shutdown rejection, and boundedElastic scheduling with JUnit and StepVerifier.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=BlockingWorkSchedulerTest,TrackedReactiveTaskRegistryTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: tests prove blocking work shifts to boundedElastic and tracked background tasks can be drained or rejected during shutdown.

### Task 3: Convert critical runtime blocking paths

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceEvaluationContextFetcher.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteServiceTest.java`

- [x] Replace direct `.block()` and `Thread.sleep()` in the named runtime paths with `BlockingWorkScheduler`, Reactor timers, or explicit synchronous service boundaries.
- [x] Replace business-path fire-and-forget `.subscribe()` with tracked registry calls or returned `Mono` orchestration.
- [x] Keep accepted scan hits documented in `docs/architecture/evidence/P0-02-reactive-threading-inventory.md` with a reason.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,TriggerRouteServiceTest test
rg -n "\\.block\\(|\\.subscribe\\(|Thread\\.sleep" backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceEvaluationContextFetcher.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: targeted tests pass; any remaining scan output for these files is explicitly classified as accepted or pending in the evidence file.

### Task 4: Separate transactions from external side effects

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionSideEffectTest.java`

- [x] Ensure DB writes commit before Redis route updates, scheduler registration, MQ publishing, and background task dispatch.
- [x] Use after-commit callbacks, an outbox table, or an explicit repairable state transition for each side effect.
- [x] Add rollback tests proving external side effects do not run when the database transaction rolls back.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasTransactionAnnotationTest,CanvasTransactionSideEffectTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: rollback tests show no Redis, scheduler, MQ, or background side effect is fired before commit.

### Task 5: Validate the reactive boundary package

**Files:**
- Docs: `docs/architecture/evidence/P0-02-reactive-threading-inventory.md`
- Plan: `docs/architecture/archive/completed/plans/P0-02-reactive-threading-and-transactions-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md`

- [x] Run all focused tests from this package in one Maven invocation.
- [x] Run the backend module suite.
- [x] Update the evidence file with accepted scan hits and converted scan hits.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=BlockingWorkSchedulerTest,TrackedReactiveTaskRegistryTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,TriggerRouteServiceTest,CanvasTransactionAnnotationTest,CanvasTransactionSideEffectTest test
cd backend && mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused tests and backend module tests pass; evidence explains every remaining blocking or subscription scan hit.
