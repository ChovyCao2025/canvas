# P2-001 - Collaboration Personalization And Reporting Spec

Priority: P2
Sequence: 001
Source: `todo/p2/collaboration-personalization-and-reporting.md`
Implementation plan: `../plans/p2-001-collaboration-personalization-and-reporting-plan.md`

## Goal

Add medium-term collaboration, persistent user preferences, contextual help, analytics depth, template market, and audience operations after core flows stabilize.

## User And Business Value

Teams can coordinate editing, personalize work surfaces, and analyze outcomes with fewer manual workarounds.

## In Scope

- Edit locks, presence, comments, share links, and change notifications.
- User preference storage for theme, sidebar, notifications, recent nodes, editor layout, and list defaults.
- Onboarding tour and contextual help.
- Behavior analytics, report export, chart/table linking, and report builder after data contracts stabilize.
- Message template market and audience operations after API validation.

## Out Of Scope

- CRDT offline collaboration and full mobile editor.
- Advanced AI reporting.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`

### Frontend Touchpoints

- `frontend/src/context/NotificationContext.tsx`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-stats/index.tsx`
- `frontend/src/services/systemOptions.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V263__collaboration_personalization_reporting.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/UserPreferenceServiceTest.java`
- `frontend/src/pages/canvas-editor/collaborationAwareness.test.tsx`

## Dependencies

- Requires stable version and permission model.
- Reporting depends on event, attribution, and analytics data quality.

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

## Implementation Status

Completed on 2026-06-05.

- Added `V263__collaboration_personalization_reporting.sql` for tenant/user-scoped editor preference storage. The plan originally named `V160`, but the repository already contains later migrations, so this implementation uses the next available migration version.
- Added `UserWorkspacePreferenceService` with a MyBatis-backed repository, allowed editor preference patch keys, default editor preferences, and tenant/user scoped upsert behavior.
- Added `CanvasCollaborationSummaryService` and `CanvasCollaborationController` for read-only collaboration summary and editor preference GET/PUT endpoints under `/canvas`.
- Added frontend `collaborationApi` plus editor collaboration awareness presentation helpers and focused tests.
- Rollout: run `V263__collaboration_personalization_reporting.sql`, then enable editor calls to `/canvas/{canvasId}/collaboration/summary` and `/canvas/preferences/editor` for tenant operators. Rollback: hide the editor collaboration chrome and stop calling the new endpoints; stored preferences are additive and can remain in place.
