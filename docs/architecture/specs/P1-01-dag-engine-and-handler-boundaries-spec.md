# Spec: DAG Engine And Handler Boundaries

Source package: `docs/architecture/todo/p1/dag-engine-and-handler-boundaries/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Implemented and verified on 2026-06-04.

- Completed on 2026-06-04: mapper-heavy node handlers now depend on domain services or gateway interfaces instead of direct `dal.mapper` imports.
- Completed on 2026-06-04: `DagEngine` now delegates gate coordination, special-node timeout scheduling, result routing, and DLQ writing to independently tested components.
- Completed on 2026-06-04: `CanvasExecutionService` now delegates trigger admission/dedup/overflow decisions, canvas/version/config loading, and lane slot dispatch to focused services.
- Verified on 2026-06-04: P1-01 focused package tests passed and the full `canvas-engine` suite passed.

## Problems

- `DagEngine.java` is currently 1387 lines and delegates gate coordination, special-node timeout scheduling, result routing, and DLQ writing; it still owns core recursive scheduling, special-timeout continuation, tracing, and error handling.
- `CanvasExecutionService.java` is currently 850 lines and delegates admission, config loading, and lane slot dispatch; it retains execution record persistence and high-level orchestration.
- `CanvasExecutionService` retains execution/status mappers only for execution record persistence; canvas/version/config lookup, MQ overflow retry, priority admission, and lane resolution are outside the service.
- Historical mapper-heavy handlers have been remediated for this package slice: `SubFlowRefHandler`, `PointsOperationHandler`, `SendMqHandler`, `MqTriggerHandler`, and `ConnectedContentHandler` now use domain services or gateway interfaces.
- The original shape made testing and safe changes expensive; the completed boundaries reduce this risk while leaving deeper service decomposition to later architecture plans.

## Evidence

- Evidence note: `docs/architecture/evidence/P1-01-dag-engine-boundaries.md`
- `DagEngine.java` line count: 1387
- `CanvasExecutionService.java` line count: 850
- Added engine boundaries: `NodeGateCoordinator`, `NodeTimeoutCoordinator`, `NodeResultRouter`, `ExecutionDlqWriter`.
- Focused engine boundary command: `cd backend && mvn -pl canvas-engine -Dtest=NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,DagEngineLifecycleTest,DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,CanvasExecutionServiceResumeTest test`
- Focused engine boundary result: 26 tests, 0 failures, 0 errors, 0 skipped.
- Handler mapper scan: `rg -n "org\\.chovy\\.canvas\\.dal\\.mapper" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers || true` returns no matches.
- Added trigger boundaries: `TriggerAdmissionService`, `CanvasExecutionConfigLoader`, `ExecutionLaneDispatcher`.
- Added service boundaries: `SubFlowLookupService`, `CustomerPointsLedgerService`, `MqMessageDefinitionService`, `ConnectedContentGateway`, `ConnectedContentGatewayService`.
- Updated handlers: `SubFlowRefHandler`, `PointsOperationHandler`, `SendMqHandler`, `MqTriggerHandler`, `ConnectedContentHandler`.
- Focused handler command: `cd backend && mvn -pl canvas-engine -Dtest=BlockingHandlerAssemblyTest,PointsOperationHandlerTest,SubFlowRefHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test`
- Focused handler result: 19 tests, 0 failures, 0 errors, 0 skipped.
- Expanded focused package command: `cd backend && mvn -pl canvas-engine -Dtest=DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,DagEngineLifecycleTest,CanvasExecutionServiceResumeTest,NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,SubFlowRefHandlerTest,PointsOperationHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest,ConnectedContentHandlerTest,ConnectedContentGatewayServiceTest,BlockingHandlerAssemblyTest,WaitResumeServiceTest,NodeTypeGovernanceTest test`
- Expanded focused package result: 72 tests, 0 failures, 0 errors, 0 skipped.
- Full backend result: `cd backend && mvn -pl canvas-engine test` passed with 601 tests, 0 failures, 0 errors, 1 skipped.
- Source review documents: remediation parts 1 and 5, constraints/risk report.

## Acceptance Criteria

- [x] DAG execution responsibilities are split into independently testable components for gate coordination, timeout scheduling, result routing, and DLQ writing.
- [x] Handler data access goes through domain services or repositories with clear contracts.
- [x] NodeHandler remains focused on node semantics and idempotency for the remediated handler slice.
- [x] Existing behavior is covered by characterization tests before large refactors.
