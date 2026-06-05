# DAG Engine And Handler Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split DAG execution and trigger admission responsibilities into independently testable components, and move mapper-heavy handler behavior behind domain service contracts without changing node semantics.

**Architecture:** Start with characterization tests around current execution behavior. Extract one responsibility at a time from `DagEngine` and `CanvasExecutionService`, preserve `NodeHandler` as the node semantics boundary, and replace direct mapper injection in handlers with domain services or repositories after each test group is stable.

**Tech Stack:** Java 21, Spring Boot WebFlux, Reactor, MyBatis-Plus, JUnit 5, AssertJ, Maven.

**Current Status:** Implemented and verified on 2026-06-04. Tasks 1-5 are complete; remaining architecture work continues in the other P1/P2/P3 plans.

---

## Source Material

- Spec: `../specs/P1-01-dag-engine-and-handler-boundaries-spec.md`
- Source package: `../../../todo/p1/dag-engine-and-handler-boundaries/`
- Coverage matrix: `../../../todo/coverage-matrix.md`

## File Structure

- Engine: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Trigger orchestration: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Handler API: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Handler API: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java`
- Handler API: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java`
- Extracted engine component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinator.java`
- Extracted engine component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinator.java`
- Extracted engine component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeResultRouter.java`
- Extracted engine component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriter.java`
- Extracted trigger component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerAdmissionService.java`
- Extracted trigger component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java`
- Extracted trigger component: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcher.java`
- Mapper-heavy handler: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SubFlowRefHandler.java`
- Mapper-heavy handler: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java`
- Mapper-heavy handler: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java`
- Mapper-heavy handler: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java`
- Domain service boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/SubFlowLookupService.java`
- Domain service boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGateway.java`
- Domain service boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayService.java`
- Domain service boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CustomerPointsLedgerService.java`
- Domain service boundary: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionService.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineCommitActionTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineDepthTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEnginePendingTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceResumeTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SubFlowRefHandlerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PointsOperationHandlerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMqHandlerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayServiceTest.java`
- Evidence: `docs/architecture/evidence/P1-01-dag-engine-boundaries.md`

### Task 1: Add characterization tests for key execution paths

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineCommitActionTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineDepthTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEnginePendingTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceResumeTest.java`
- Docs: `docs/architecture/evidence/P1-01-dag-engine-boundaries.md`

- [x] Capture current behavior for start/end, condition routing, wait/resume, commit action, DLQ write, timeout, and lane admission.
- [x] Record current `DagEngine` and `CanvasExecutionService` responsibilities in the evidence file.
- [x] Keep characterization assertions stable before extraction work begins.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,CanvasExecutionServiceResumeTest test
wc -l backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: tests pass against current behavior and evidence lists the responsibilities that will move.

Result on 2026-06-04:

- Characterization coverage is present across `DagEngineCommitActionTest`, `DagEngineDepthTest`, `DagEnginePendingTest`, `DagEngineLifecycleTest`, `CanvasExecutionServiceResumeTest`, `WaitResumeServiceTest`, `ExecutionDlqWriterTest`, `NodeTimeoutCoordinatorTest`, `ExecutionLaneDispatcherTest`, and `TriggerAdmissionServiceTest`.
- Evidence records the responsibilities moved from `DagEngine` and `CanvasExecutionService`.
- P1-01 focused validation passed after extraction with 72 tests, 0 failures, 0 errors, 0 skipped.

### Task 2: Extract small components from `DagEngine`

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinator.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinator.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeResultRouter.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriter.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinatorTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinatorTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeResultRouterTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriterTest.java`

- [x] Move gate coordination into `NodeGateCoordinator`.
- [x] Move timeout scheduling and cancellation into `NodeTimeoutCoordinator`.
- [x] Move node result routing into `NodeResultRouter`.
- [x] Move DLQ persistence and serialization into `ExecutionDlqWriter`.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: extracted component tests and existing DAG characterization tests pass; `DagEngine` delegates to the new components without behavior drift.

Result on 2026-06-04:

- Added `NodeGateCoordinator`, `NodeTimeoutCoordinator`, `NodeResultRouter`, and `ExecutionDlqWriter`.
- Added `NodeGateCoordinatorTest`, `NodeTimeoutCoordinatorTest`, `NodeResultRouterTest`, and `ExecutionDlqWriterTest`.
- Updated `DagEngine` to delegate gate coordination, special-node timeout scheduling, result routing, and DLQ persistence/serialization.
- Focused command passed: `cd backend && mvn -pl canvas-engine -Dtest=NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,DagEngineLifecycleTest,DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,CanvasExecutionServiceResumeTest test`
- Focused result: 26 tests, 0 failures, 0 errors, 0 skipped.
- `DagEngine.java` line count after extraction: 1386 lines.
- `DagEngine.java` no longer imports `CanvasExecutionDlqMapper` or `CanvasExecutionDlqDO`; those imports are isolated in `ExecutionDlqWriter`.

### Task 3: Extract trigger and admission pieces from `CanvasExecutionService`

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerAdmissionService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcher.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerAdmissionServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoaderTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcherTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceResumeTest.java`

- [x] Move trigger pre-check, dedup, priority admission, missing-context marking, and overflow retry decisions into `TriggerAdmissionService`; keep shutdown lifecycle guard in dedicated `ExecutionLifecycleGate`.
- [x] Move canvas/version/config loading into `CanvasExecutionConfigLoader`.
- [x] Move lane dispatch and request fallback branching into `ExecutionLaneDispatcher`.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,CanvasExecutionServiceResumeTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: trigger service tests pass and `CanvasExecutionService` retains orchestration while delegated services own admission, config loading, and lane dispatch decisions.

Result on 2026-06-04:

- Added `TriggerAdmissionService`, `CanvasExecutionConfigLoader`, and `ExecutionLaneDispatcher`.
- Updated `CanvasExecutionService` to delegate trigger admission/dedup/overflow retry decisions, canvas/version/graph loading, trigger-node lookup, and final lane slot acquisition.
- Kept `ExecutionLifecycleGate` as the shutdown guard component and verified lifecycle rejection through `CanvasExecutionServiceResumeTest` and `ExecutionLifecycleGateTest`.
- Added `TriggerAdmissionServiceTest`, `CanvasExecutionConfigLoaderTest`, and `ExecutionLaneDispatcherTest`; updated resume/wait tests for the new constructor boundaries.
- Focused Task 3 command passed: `cd backend && mvn -pl canvas-engine -Dtest=TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,CanvasExecutionServiceResumeTest,WaitResumeServiceTest test`
- Focused Task 3 result: 21 tests, 0 failures, 0 errors, 0 skipped.
- `CanvasExecutionService.java` line count after extraction: 850 lines.
- `CanvasExecutionService.java` no longer imports canvas/version/config mapper/cache/parser dependencies, `RocketMQTemplate`, `TriggerPriorityConfig`, or `ExecutionLaneResolver`.

### Task 4: Introduce domain service boundaries for mapper-heavy handlers

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SubFlowRefHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/SubFlowLookupService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGateway.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CustomerPointsLedgerService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SubFlowRefHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PointsOperationHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMqHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayServiceTest.java`

- [x] Move subflow lookup, points ledger writes, and MQ definition lookup behind domain service methods.
- [x] Move connected content cache lookup/save and outbound HTTP gateway behind a domain gateway service.
- [x] Keep handler tests focused on node input, node output, idempotency, and failure semantics.
- [x] Scan handler package for remaining direct mapper injections and record accepted residuals in evidence.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=SubFlowRefHandlerTest,PointsOperationHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test
rg -n "Mapper" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers
# Do not stage or commit in this session unless the user explicitly asks.
```

Result on 2026-06-04:

- Added `SubFlowLookupService`, `CustomerPointsLedgerService`, and `MqMessageDefinitionService`.
- Updated `SubFlowRefHandler`, `PointsOperationHandler`, `SendMqHandler`, and `MqTriggerHandler` to depend on those services.
- Added/updated `SubFlowRefHandlerTest`, `PointsOperationHandlerTest`, `SendMqHandlerTest`, `MqTriggerHandlerTest`, and `BlockingHandlerAssemblyTest`.
- Focused command passed: `cd backend && mvn -pl canvas-engine -Dtest=BlockingHandlerAssemblyTest,PointsOperationHandlerTest,SubFlowRefHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test`
- Focused result: 19 tests, 0 failures, 0 errors, 0 skipped.
- Added `ConnectedContentGateway` and `ConnectedContentGatewayService`; updated `ConnectedContentHandler` to depend on the gateway interface.
- Added `ConnectedContentGatewayServiceTest` and updated `ConnectedContentHandlerTest`.
- Expanded focused command passed: `cd backend && mvn -pl canvas-engine -Dtest=ConnectedContentGatewayServiceTest,ConnectedContentHandlerTest,BlockingHandlerAssemblyTest,PointsOperationHandlerTest,SubFlowRefHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test`
- Expanded focused result: 30 tests, 0 failures, 0 errors, 0 skipped.
- Handler mapper scan returned no `org.chovy.canvas.dal.mapper` imports in `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers`.
- Full backend command passed: `cd backend && mvn -pl canvas-engine test`
- Full result: 505 tests, 0 failures, 0 errors, 0 skipped.

### Task 5: Validate DAG boundaries

**Files:**
- Docs: `docs/architecture/evidence/P1-01-dag-engine-boundaries.md`
- Plan: `docs/architecture/archive/completed/plans/P1-01-dag-engine-and-handler-boundaries-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P1-01-dag-engine-and-handler-boundaries-spec.md`

- [x] Run the package test set after all extractions.
- [x] Re-run line count and mapper-in-handler scans.
- [x] Run the backend module suite after focused checks pass.

Current line counts are `DagEngine.java` 1387 lines and `CanvasExecutionService.java` 850 lines.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,CanvasExecutionServiceResumeTest,NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,SubFlowRefHandlerTest,PointsOperationHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test
wc -l backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java
rg -n "Mapper" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers || true
cd backend && mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused and backend module tests pass; evidence shows reduced responsibilities and no unowned direct mapper injection in node handlers.

Result on 2026-06-04:

- P1-01 package command passed: `cd backend && mvn -pl canvas-engine -Dtest=DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,DagEngineLifecycleTest,CanvasExecutionServiceResumeTest,NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,SubFlowRefHandlerTest,PointsOperationHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest,ConnectedContentHandlerTest,ConnectedContentGatewayServiceTest,BlockingHandlerAssemblyTest,WaitResumeServiceTest,NodeTypeGovernanceTest test`
- P1-01 package result: 72 tests, 0 failures, 0 errors, 0 skipped.
- Handler mapper scan result: no `org.chovy.canvas.dal.mapper` imports under `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers`.
- `CanvasExecutionService` boundary scan result: no direct `CanvasMapper`, `CanvasVersionMapper`, `CanvasConfigCache`, `CanvasEntityCache`, `DagParser`, `MqTriggerHandler`, `RocketMQTemplate`, `TriggerPriorityConfig`, or `ExecutionLaneResolver` references.
- Full backend command passed: `cd backend && mvn -pl canvas-engine test`
- Full backend result: 601 tests, 0 failures, 0 errors, 1 skipped.
