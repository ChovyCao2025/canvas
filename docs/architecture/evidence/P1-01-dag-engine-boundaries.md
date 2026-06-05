# P1-01 DAG Engine And Handler Boundaries Evidence

Date: 2026-06-04

## Status

Implemented and verified.

Completed in this slice:

- Moved mapper-heavy handler persistence access behind domain service or gateway boundaries.
- Extracted `DagEngine` gate coordination, special-node timeout scheduling, result routing, and DLQ writing into independently tested scheduler components.
- Extracted `CanvasExecutionService` trigger admission, config loading, and lane slot dispatch decisions into focused trigger components.
- Preserved handler and trigger behavior with focused tests.
- Verified the backend module after all P1-01 extractions.

## Code Evidence

- `SubFlowLookupService` owns sub-flow canvas and version lookup: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/SubFlowLookupService.java:14`
- `ConnectedContentGateway` defines the connected-content cache/fetch contract: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGateway.java:8`
- `ConnectedContentGatewayService` owns connected-content cache persistence and outbound HTTP fetch: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayService.java:17`
- `CustomerPointsLedgerService` owns points ledger lookup and insert operations: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CustomerPointsLedgerService.java:12`
- `MqMessageDefinitionService` owns enabled message definition lookup and topic resolution: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionService.java:15`
- `SubFlowRefHandler` now depends on `SubFlowLookupService`: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SubFlowRefHandler.java:41`
- `PointsOperationHandler` now depends on `CustomerPointsLedgerService`: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java:27`
- `SendMqHandler` now depends on `MqMessageDefinitionService`: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java:39`
- `MqTriggerHandler` now depends on `MqMessageDefinitionService`: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java:24`
- `ConnectedContentHandler` now depends on `ConnectedContentGateway`: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java:48`
- `NodeGateCoordinator` owns node-gate acquire/repeat/release coordination: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinator.java:9`
- `NodeTimeoutCoordinator` owns one-shot special-node timeout scheduling and elapsed checks: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinator.java:15`
- `NodeResultRouter` owns result target resolution, failure-aware downstream filtering, and skipped branch propagation: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeResultRouter.java:23`
- `ExecutionDlqWriter` owns replayable execution DLQ persistence and serialization: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriter.java:20`
- `TriggerAdmissionService` owns trigger dedup, pre-check, priority admission, overflow retry enqueue, and missing resume context marking: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerAdmissionService.java:35`
- `CanvasExecutionConfigLoader` owns canvas validation, version resolution, graph loading, and trigger node lookup: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java:29`
- `ExecutionLaneDispatcher` owns final execution lane slot acquisition and overflow response shaping: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcher.java:19`

## Boundary Scans

Handler direct mapper import scan:

```bash
rg -n "org\\.chovy\\.canvas\\.dal\\.mapper" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers || true
```

Result: no matches.

Line counts:

```bash
wc -l backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerAdmissionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcher.java
```

Result:

- `DagEngine.java`: 1436 lines before extraction.
- `DagEngine.java` final P1-01 count: 1387 lines.
- `CanvasExecutionService.java`: 1479 lines before trigger extraction.
- `CanvasExecutionService.java` final P1-01 count: 850 lines.
- `TriggerAdmissionService.java`: 459 lines.
- `CanvasExecutionConfigLoader.java`: 131 lines.
- `ExecutionLaneDispatcher.java`: 88 lines.

DagEngine direct DLQ mapper scan:

```bash
rg -n "CanvasExecutionDlqMapper|CanvasExecutionDlqDO" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java || true
```

Result: no matches.

CanvasExecutionService extracted-dependency scan:

```bash
rg -n "CanvasExecutionDlqMapper|CanvasExecutionDlqDO|RocketMQTemplate|TriggerPriorityConfig|ExecutionLaneResolver|CanvasMapper|CanvasVersionMapper|CanvasConfigCache|CanvasEntityCache|DagParser|MqTriggerHandler" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java || true
```

Result: no matches.

## Verification

Focused handler command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=BlockingHandlerAssemblyTest,PointsOperationHandlerTest,SubFlowRefHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test
```

Focused handler result: PASS, 19 tests, 0 failures, 0 errors, 0 skipped.

Expanded handler/gateway command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ConnectedContentGatewayServiceTest,ConnectedContentHandlerTest,BlockingHandlerAssemblyTest,PointsOperationHandlerTest,SubFlowRefHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest test
```

Expanded handler/gateway result: PASS, 30 tests, 0 failures, 0 errors, 0 skipped.

Execution context regression command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ExecutionContextConcurrencyTest test
```

Execution context result: PASS, 25 tests, 0 failures, 0 errors, 0 skipped.

Focused engine boundary command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,DagEngineLifecycleTest,DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,CanvasExecutionServiceResumeTest test
```

Focused engine boundary result: PASS, 26 tests, 0 failures, 0 errors, 0 skipped.

Focused trigger extraction command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,CanvasExecutionServiceResumeTest,WaitResumeServiceTest test
```

Focused trigger extraction result: PASS, 21 tests, 0 failures, 0 errors, 0 skipped.

P1-01 package command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=DagEngineCommitActionTest,DagEngineDepthTest,DagEnginePendingTest,DagEngineLifecycleTest,CanvasExecutionServiceResumeTest,NodeGateCoordinatorTest,NodeTimeoutCoordinatorTest,NodeResultRouterTest,ExecutionDlqWriterTest,TriggerAdmissionServiceTest,CanvasExecutionConfigLoaderTest,ExecutionLaneDispatcherTest,SubFlowRefHandlerTest,PointsOperationHandlerTest,SendMqHandlerTest,MqTriggerHandlerTest,ConnectedContentHandlerTest,ConnectedContentGatewayServiceTest,BlockingHandlerAssemblyTest,WaitResumeServiceTest,NodeTypeGovernanceTest test
```

P1-01 package result: PASS, 72 tests, 0 failures, 0 errors, 0 skipped.

Full backend command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Full backend result before Task 2 extraction: PASS, 505 tests, 0 failures, 0 errors, 0 skipped.

Full backend result after Task 2 extraction: PASS, 564 tests, 0 failures, 0 errors, 0 skipped.

Full backend result after P1-01 completion: PASS, 601 tests, 0 failures, 0 errors, 1 skipped.

## Notes

- `ExecutionContextConcurrencyTest.approximateSizeReflectsCurrentNodeOutputSnapshots` was updated to assert the current serialized node-output size semantics instead of the older key/value character-count approximation.
- Expected negative-path logs still appear in tests that intentionally simulate downstream failures.
- `ExecutionLifecycleGate` remains a dedicated shutdown guard component instead of being folded into `TriggerAdmissionService`; this keeps process lifecycle and trigger admission responsibilities separate while satisfying the boundary objective.
