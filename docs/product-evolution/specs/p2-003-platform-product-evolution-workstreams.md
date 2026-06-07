# P2-003 - Platform Product Evolution Workstreams Spec

Priority: P2
Sequence: 003
Source: `todo/p2/platform-product-evolution-workstreams.md`
Implementation plan: `../plans/p2-003-platform-product-evolution-workstreams-plan.md`

## Goal

Turn broad platform strategy into bounded medium-term workstreams for platformization, data assets, channels, operations, knowledge, and integrations.

## User And Business Value

The product roadmap can advance beyond stabilization without restarting from raw strategy catalogs.

## In Scope

- Platformization: extension points, developer portal basics, API keys, outbound webhooks, and schema improvements.
- Data assets: data quality, data catalog, path analytics, reports, and event pipeline foundations.
- Channels: WeCom L1/L2, adapter abstraction, and channel cost/receipt tracking.
- Operations: approval expansion, audit timeline, command dashboard, and alert rules.
- Knowledge: template market, best-practice library, contextual help, and playbooks.
- Integrations: inbound webhook, API key management, SSO/OIDC decision, and data source improvements.

## Out Of Scope

- Microservices, serverless, edge computing, multi-cloud, and full public marketplace.
- Unbounded configuration inventories.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain`
- `backend/canvas-engine/src/main/resources/db/migration`

### Frontend Touchpoints

- `frontend/src/pages/api-docs/index.tsx`
- `frontend/src/pages/home/index.tsx`
- `frontend/src/pages/system-options/index.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V264__platform_product_evolution_workstreams.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java`
- `frontend/src/pages/home/platformCommandCenter.test.ts`

## Implementation Status

Completed on 2026-06-05.

- Added `V264__platform_product_evolution_workstreams.sql` for additive workstream governance metadata. The plan originally named `V162`, but the repository already contains later migrations, so this implementation uses the next available migration version.
- Added `PlatformWorkstreamService` and `JdbcPlatformWorkstreamRepository` to expose workstream readiness and block broad workstreams without child-spec paths.
- Added authenticated `PlatformWorkstreamController` at `/platform/workstreams`, guarded by `TenantContextResolver.currentOrError()`.
- Added frontend `platformWorkstreamApi`, `platformCommandCenter` helpers, focused tests, and a visible homepage platform workstream card with loading, empty, error, and status states.
- Rollout: run `V264__platform_product_evolution_workstreams.sql`, then surface `/platform/workstreams` in the home command center. Rollback: hide the command-center panel and stop calling `/platform/workstreams`; table rows are additive governance metadata and do not affect runtime execution.

## Dependencies

- Requires P0 tenant/security and P1 operator loop visibility.
- Each workstream must get its own bounded child spec before code work.

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
