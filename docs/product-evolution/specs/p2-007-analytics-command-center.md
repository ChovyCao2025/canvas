# P2-007 - Analytics Command Center Spec

Priority: P2
Sequence: 007
Source: `todo/p2/product-opportunities-from-filtered-scope.md#analytics-command-center`
Implementation plan: `../plans/p2-007-analytics-command-center-plan.md`

Status: Open execution plan; implementation is not complete in this docs-only audit because the plan retains unchecked execution tasks.

## Goal

Create a deeper analytics command center with revenue, ROI, channel comparison, canvas comparison, report templates, scheduled reports, export, and chart drill-down.

## User And Business Value

This converts a filtered opportunity into a bounded medium-term implementation candidate with explicit dependencies and verification.

## In Scope

- Metric dictionary and dashboard aggregation.
- Channel and canvas comparison.
- Saved report templates and scheduled delivery.
- CSV or Excel export and chart drill-down.

## Out Of Scope

- Unbounded source-strategy scope and raw configuration inventories.
- Features that depend on unstated business ownership or unvalidated demand.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java`

### Frontend Touchpoints

- `frontend/src/pages/analytics-command-center/index.tsx`
- `frontend/src/services/analyticsApi.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java`
- `frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.tsx`

## Dependencies

- P0/P1 stabilization remains ahead of this work.
- A short discovery pass must confirm current API and data contracts before code work.

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
