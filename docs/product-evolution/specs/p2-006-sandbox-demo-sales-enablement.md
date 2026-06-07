# P2-006 - Sandbox Demo And Sales Enablement Spec

Priority: P2
Sequence: 006
Source: `todo/p2/product-opportunities-from-filtered-scope.md#sandbox-demo-canvas-and-sales-enablement`
Implementation plan: `../plans/p2-006-sandbox-demo-sales-enablement-plan.md`

## Goal

Provide demo canvases, mock data, sandbox tenants, reset/expiry behavior, and sales enablement material.

## User And Business Value

This converts a filtered opportunity into a bounded medium-term implementation candidate with explicit dependencies and verification.

## In Scope

- Demo canvas installer.
- Mock data generator with safe demo markers.
- Sandbox tenant lifecycle and reset.
- Sales demo guide stored with product docs.

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

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/JdbcDemoSandboxRepository.java`

### Frontend Touchpoints

- `frontend/src/pages/demo-sandbox/index.tsx`
- `frontend/src/pages/demo-sandbox/demoSandbox.ts`
- `frontend/src/services/demoSandboxApi.ts`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/components/accessibility/RouteA11y.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V269__sandbox_demo_sales_enablement.sql`
- `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java`
- `frontend/src/pages/demo-sandbox/demoSandbox.test.ts`
- `frontend/src/pages/demo-sandbox/index.test.tsx`
- `frontend/src/services/demoSandboxApi.test.ts`
- `frontend/src/components/layout/AppLayout.a11y.test.tsx`

## Implementation Status

Completed on 2026-06-05.

- Added `V269__sandbox_demo_sales_enablement.sql` for sandbox lifecycle rows, `DEMO_TENANT_*` markers, expiry metadata, reset audit fields, and cleanup indexes. The plan originally named `V165`, but the current workspace already uses later migration versions through `V268`.
- Added `DemoSandboxService` and `JdbcDemoSandboxRepository` for install, reset, and expired-sandbox lookup with bounded TTL validation.
- Added authenticated `DemoSandboxController` at `/demo-sandboxes`, guarded by `TenantContextResolver.currentOrError()`; reset audit uses the authenticated username instead of a spoofable header.
- Added `demoSandboxApi`, frontend helpers, visible `/demo-sandbox` page, main-navigation entry, route announcement text, and empty/loading/error/reset states.
- Added `docs/product-evolution/runbooks/sandbox-demo-sales-guide.md` with demo boundary, walkthrough, reset command, and rollback sections.
- Rollout: run `V269__sandbox_demo_sales_enablement.sql`, create sandbox lifecycle rows, and expose demo reset only to authenticated internal users. Rollback: hide the demo sandbox route and keep demo rows marked with `DEMO_TENANT_*` for cleanup.

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
