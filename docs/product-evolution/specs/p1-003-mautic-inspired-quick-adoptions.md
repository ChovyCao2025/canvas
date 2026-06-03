# P1-003 - Mautic Inspired Quick Adoptions Spec

Priority: P1
Sequence: 003
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003-mautic-inspired-quick-adoptions-plan.md`

## Goal

Adopt practical Mautic-inspired improvements: dynamic segment sending, safer previews, canvas import/export, lightweight project grouping, and AI policy clarity.

## User And Business Value

Operators get safer campaign migration, clearer send semantics, and better preview confidence without adopting a full competitor ecosystem model.

## In Scope

- Static locked audience versus dynamic refreshing audience send mode.
- Preview across text, web push, and selected user contexts with sensitive data masking.
- Canvas import/export for environment migration and template reuse.
- Basic project or folder grouping if it does not duplicate existing canvas grouping.
- Internal AI capability policy if AI features are visible.

## Out Of Scope

- Public AI marketplace and public community strategy.
- Runtime third-party plugin marketplace.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`

### Frontend Touchpoints

- `frontend/src/pages/audience-list/index.tsx`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-list/index.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V94__mautic_quick_adoptions.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasImportExportServiceTest.java`
- `frontend/src/pages/canvas-list/importExportFlow.test.tsx`

## Dependencies

- Audience resolver performance and snapshot semantics must be verified.
- Import/export must preserve graph schema version without leaking secrets.

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
