# P2-005 - Message Template Center Spec

Priority: P2
Sequence: 005
Source: `todo/p2/product-opportunities-from-filtered-scope.md#message-template-center`
Implementation plan: `../plans/p2-005-message-template-center-plan.md`

## Goal

Build a unified message template center for template CRUD, variables, channel adaptation, preview, and approval.

## User And Business Value

This converts a filtered opportunity into a bounded medium-term implementation candidate with explicit dependencies and verification.

## In Scope

- Template CRUD and search.
- Variable metadata API and autocomplete.
- Channel-specific adaptation and preview.
- Approval flow integration.

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

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/JdbcMessageTemplateRepository.java`

### Frontend Touchpoints

- `frontend/src/pages/message-templates/index.tsx`
- `frontend/src/pages/message-templates/messageTemplateCenter.ts`
- `frontend/src/services/messageTemplateApi.ts`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/components/accessibility/RouteA11y.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V268__message_template_center.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java`
- `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`
- `frontend/src/pages/message-templates/index.test.tsx`
- `frontend/src/services/messageTemplateApi.test.ts`
- `frontend/src/components/layout/AppLayout.a11y.test.tsx`

## Implementation Status

Completed on 2026-06-05.

- Added `V268__message_template_center.sql` for tenant-scoped template metadata, body content, extracted variable JSON, status, and creator audit fields. The plan originally named `V164`; the current workspace already uses `V267` for the technical migration evidence registry, so this implementation uses the next available migration version.
- Added `MessageTemplateService` and `JdbcMessageTemplateRepository` for authenticated tenant create/search/preview workflows, variable extraction, channel validation, local rendering, and missing-variable reporting.
- Added authenticated `MessageTemplateController` at `/message-templates`, guarded by `TenantContextResolver.currentOrError()`.
- Added `messageTemplateApi`, `messageTemplateCenter` presentation helpers, a visible `/message-templates` page, route registration, main-navigation entry, and route announcement text.
- Rollout: run `V268__message_template_center.sql`, then expose `/message-templates` to authenticated operators. Rollback: remove or hide the route/menu entry; template rows are additive and do not affect runtime sends until later approval/channel-adaptation specs wire them into journey nodes.

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
