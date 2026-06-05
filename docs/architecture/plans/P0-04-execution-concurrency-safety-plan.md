# Execution Concurrency Safety Verification Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the execution concurrency package by preserving the implemented fixes, rerunning focused race tests, and recording full-suite verification evidence for circuit breaker state, execution context snapshots, scheduler lifecycle, and managed virtual-thread work.

**Architecture:** The implementation is present in `main` per `../specs/P0-04-execution-concurrency-safety-spec.md`. Verification covers atomic circuit-breaker snapshots, serialized execution-context output ownership, scheduler close/register/add races, and bounded managed virtual-thread execution.

**Tech Stack:** Java 21, Spring Boot WebFlux, JUnit 5, AssertJ, virtual threads, `AtomicReference`, `AtomicBoolean`, Maven.

**Implementation Status:** Implemented and verified in `main` on 2026-06-04. Focused concurrency verification and full `canvas-engine` tests both pass.

---

## Source Material

- Spec: `../specs/P0-04-execution-concurrency-safety-spec.md`
- Source package: `../todo/p0/execution-concurrency-safety/`
- Coverage matrix: `../todo/coverage-matrix.md`
- Evidence: `../evidence/P0-04-execution-concurrency-safety.md`

## File Structure

- Circuit breaker: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java`
- Execution context: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Scheduler lifecycle: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- Managed executor: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Managed executor users: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Evidence: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`

## Task 1: Verify Circuit Breaker Atomic Transitions

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistryTest.java`
- Docs: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`

- [x] Re-run the closed/open/half-open concurrent success and failure tests.
- [x] Record the test command, Java version, and pass/fail result in the evidence file.
- [x] Confirm state, failure count, half-open attempts, and opened timestamp move as one immutable snapshot.

Result: included in the focused P0-04 command. `CircuitBreakerRegistryTest` passed 5 tests.

## Task 2: Verify Execution Context Output Ownership

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextConcurrencyTest.java`
- Docs: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`

- [x] Re-run parallel branch output collision tests.
- [x] Confirm grouped output, flat output, size counters, and stale flat-key cleanup are serialized together.
- [x] Record whether node-qualified flat keys remain stable under concurrent writers.

Result: included in the focused P0-04 command. `ExecutionContextConcurrencyTest` passed 25 tests.

## Task 3: Verify Scheduler Lifecycle Races

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerServiceTest.java`
- Docs: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`

- [x] Re-run close/register/add race coverage.
- [x] Confirm `close()` rejects new pending jitter groups while terminating existing groups deterministically.
- [x] Record the behavior for register-after-close and add-after-close attempts.

Result: included in the focused P0-04 command. `CanvasSchedulerServiceTest` passed 6 tests.

## Task 4: Verify Managed Virtual-Thread Execution

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutor.java`
- Production users: `CanvasService`, `TriggerPreCheckService`, `CanvasExecutionService`, `AudienceComputeTaskRunner`, `AudienceController`, `GroovyHandler`
- Tests: `ManagedVirtualThreadExecutorTest`, `AudienceComputeTaskRunnerTest`, `AudienceControllerTest`, `AudienceControllerTaskTest`
- Docs: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`

- [x] Re-run managed executor tests for bounded in-flight work, shutdown rejection, and drain behavior.
- [x] Re-run audience and Groovy-adjacent tests that exercise executor users.
- [x] Search for raw `Thread.ofVirtual().start` usage and record the result.

Result: focused managed-executor and caller tests passed. Raw virtual-thread start scan returned no matches in `backend/canvas-engine/src/main/java`.

## Task 5: Validate The Concurrency Package

**Files:**
- Docs: `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`
- Plan: `docs/architecture/plans/P0-04-execution-concurrency-safety-plan.md`
- Spec: `docs/architecture/specs/P0-04-execution-concurrency-safety-spec.md`

- [x] Run the full focused concurrency command from the spec.
- [x] Run the backend module test suite after the focused command passes.
- [x] Update the spec and evidence with exact verification results.

Focused command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ManagedVirtualThreadExecutorTest,CircuitBreakerRegistryTest,ExecutionContextConcurrencyTest,CanvasSchedulerServiceTest,AudienceComputeTaskRunnerTest,CdpTagOperationServiceRetryTest,AudienceControllerTest,AudienceControllerTaskTest test
```

Focused result: PASS, 59 tests, 0 failures, 0 errors, 0 skipped.

Full backend command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Full result: PASS, 491 tests, 0 failures, 0 errors, 0 skipped.
