# Spec: DAG Engine And Handler Boundaries

## Verification Status

Confirmed.

## Problems

- `DagEngine.java` is 1539 lines and owns scheduling, gates, timeout handling, tracing/DLQ, routing, and error handling.
- `CanvasExecutionService.java` is 1407 lines and coordinates trigger admission, config loading, lane dispatch, persistence, DLQ, and MQ fallback.
- Several handlers and engine services inject mappers directly, mixing domain I/O with node execution logic.
- The current shape makes testing and safe changes expensive.

## Evidence

- `DagEngine.java` line count: 1539
- `CanvasExecutionService.java` line count: 1407
- Direct mapper examples: `CanvasTriggerHandler`, `SubFlowRefHandler`, `ManualApprovalHandler`, `PointsOperationHandler`, `TagOperationHandler`, `GoalCheckHandler`, `TrackEventHandler`, `UpdateProfileHandler`, `SendMqHandler`
- Source review documents: remediation parts 1 and 5, constraints/risk report.

## Acceptance Criteria

- DAG execution responsibilities are split into independently testable components.
- Handler data access goes through domain services or repositories with clear contracts.
- NodeHandler remains focused on node semantics and idempotency.
- Existing behavior is covered by characterization tests before large refactors.
