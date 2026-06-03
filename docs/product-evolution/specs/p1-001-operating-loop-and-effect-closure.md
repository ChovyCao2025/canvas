# P1-001 - Operating Loop And Effect Closure Spec

Priority: P1
Sequence: 001
Source: `todo/p1/operating-loop-and-effect-closure.md`
Implementation plan: `../plans/p1-001-operating-loop-and-effect-closure-plan.md`

## Goal

Make the core operator loop measurable and reversible from template selection through preview, publish, attribution, receipt tracking, versioning, and audit review.

## User And Business Value

Operators can launch faster, inspect expected impact before publish, measure outcome, and recover from bad releases.

## In Scope

- Template browsing and one-click clone from seeded templates.
- Audience estimate, touch preview, test send, and dry-run visibility before publish.
- Lightweight last-touch attribution with conversion event intake.
- Global control group support for incremental lift measurement.
- Canvas version visibility, diff, and rollback.
- Channel receipt tracking and audit log UI.

## Out Of Scope

- Full multi-touch attribution, predictive optimization, and AI journey creation.
- Advanced report builder.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java`

### Frontend Touchpoints

- `frontend/src/pages/canvas-list/index.tsx`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-stats/index.tsx`
- `frontend/src/services/api.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V92__operating_loop_effect_closure.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceTest.java`
- `frontend/src/pages/canvas-list/templateCloneFlow.test.tsx`
- `frontend/src/pages/canvas-editor/prePublishChecks.test.tsx`

## Dependencies

- P0 safety work must protect publish and send paths.
- Attribution depends on stable conversion event intake and touchpoint recording.

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
