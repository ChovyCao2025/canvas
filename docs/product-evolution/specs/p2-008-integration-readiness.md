# P2-008 - Integration Readiness Spec

Priority: P2
Sequence: 008
Source: `todo/p2/product-opportunities-from-filtered-scope.md#integration-readiness`
Implementation plan: `../plans/p2-008-integration-readiness-plan.md`

## Goal

Package inbound webhooks, outbound webhooks, API keys, SSO/OIDC decision, data source management, and data sync into an integration foundation.

## User And Business Value

This converts a filtered opportunity into a bounded medium-term implementation candidate with explicit dependencies and verification.

## In Scope

- API key management and permissions.
- Inbound and outbound webhook signing, retry, and logs.
- SSO/OIDC discovery and configuration decision.
- Data source and sync management.

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

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java`

### Frontend Touchpoints

- `frontend/src/pages/integrations/index.tsx`
- `frontend/src/services/integrationApi.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V107__integration_readiness.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java`
- `frontend/src/pages/integrations/integrationReadiness.test.tsx`

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
