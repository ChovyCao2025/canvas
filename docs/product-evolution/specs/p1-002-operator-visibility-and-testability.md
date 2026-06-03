# P1-002 - Operator Visibility And Testability Spec

Priority: P1
Sequence: 002
Source: `todo/p1/operator-visibility-and-testability.md`
Implementation plan: `../plans/p1-002-operator-visibility-and-testability-plan.md`

## Goal

Expose already-built backend execution, message, policy, canary, dry-run, audience estimate, version, and table operations through usable operator UI.

## User And Business Value

Operators can inspect runtime state and test changes without relying on logs or backend-only APIs.

## In Scope

- Execution request management UI.
- Message send record search and detail UI.
- Consent, suppression, channel preference, and frequency policy management UI.
- Circuit breaker, canary, dry-run, audience estimate, and version diff UI.
- Operational table search, filters, selection, export, fixed operations, and column customization.

## Out Of Scope

- Real-time CRDT collaboration and full report builder.
- Large-row export without async limits.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`

### Frontend Touchpoints

- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-stats/index.tsx`
- `frontend/src/services/api.ts`
- `frontend/src/components/canvas/ExecutionTracePanel.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V93__operator_visibility_and_testability.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionManagementControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/NotificationControllerTest.java`
- `frontend/src/pages/canvas-editor/dryRunVisualization.test.tsx`
- `frontend/src/pages/canvas-stats/operatorTables.test.tsx`

## Dependencies

- Endpoint contracts for canary, dry-run, and execution records must be confirmed.
- Export must define row and file size limits.

## Risks And Controls

- Scope creep: keep the first implementation to the workflow in this spec and move broader ideas to a follow-up spec.
- Tenant or permission regression: add backend tests for tenant-scoped data and role checks before exposing UI.
- UI complexity: use one page or one panel first, then expand only after the workflow is verified.
- Data migration risk: make every migration additive and reversible by disabling the new route or feature flag.

## Acceptance Criteria

- The source item has a visible implemented workflow or a documented discovery exit if this is a P3 strategy item.
- All changed backend endpoints reject unauthorized access and preserve tenant scoping.
- All changed frontend routes handle loading, empty, error, and permission states.
- Tests named in the plan pass in the local commands for backend and frontend slices.
- The implementation includes rollout notes covering feature flag, migration, and rollback behavior.
