# P0-001 - Production Safety And Compliance Spec

Priority: P0
Sequence: 001
Source: `todo/p0/production-safety-and-compliance-stopgaps.md`
Implementation plan: `../plans/p0-001-production-safety-and-compliance-plan.md`

## Goal

Make production use safe by closing tenant isolation, authorization, compliance-send-path, stub-node, unsaved-change, and runtime-degradation gaps.

## User And Business Value

Operators and tenants can use the platform without cross-tenant data exposure, unprotected operational endpoints, or misleading production behavior.

## In Scope

- Tenant-scoped read/write enforcement for canvas, audience, CDP, notification, and execution data.
- Authentication and authorization for ops and execution-sensitive endpoints.
- Consent, suppression, channel preference, and frequency checks wired into real send paths.
- Production-safe treatment for AI and recommendation stub nodes.
- Operator-visible circuit breaker and degradation state.
- Unsaved-change protection for editor and key configuration forms.

## Out Of Scope

- Advanced privacy computing, global compliance certification, and public security attestations.
- Full AI productization beyond making current AI-like nodes safe.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`

### Frontend Touchpoints

- `frontend/src/App.tsx`
- `frontend/src/auth/guards.tsx`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/services/api.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V91__production_safety_and_compliance.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandlerPolicyTest.java`
- `frontend/src/auth/guards.test.tsx`
- `frontend/src/pages/canvas-editor/unsavedChangeGuard.test.ts`

## Dependencies

- Current tenant schema and tenant_id coverage must be audited before migration.
- Policy service behavior must be traced against every send handler.

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
