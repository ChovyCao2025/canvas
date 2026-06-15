# DDD-C09BL Creator Collaboration Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/canvas/creator-collaboration` as the top current route gap
after DDD-C09BK:

- old controllers: 1
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy reference:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java`

## Legacy Routes

- `POST /canvas/creator-collaboration/creators`
- `POST /canvas/creator-collaboration/campaigns`
- `POST /canvas/creator-collaboration/collaborations`
- `POST /canvas/creator-collaboration/deliverables`
- `POST /canvas/creator-collaboration/mutations`
- `POST /canvas/creator-collaboration/mutations/{mutationId}/approve`
- `POST /canvas/creator-collaboration/mutations/{mutationId}/execute`
- `GET /canvas/creator-collaboration/mutations`
- `GET /canvas/creator-collaboration/summary`

## Exact Reserved Files

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CreatorCollaborationFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CreatorCollaborationApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CreatorCollaborationCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CreatorCollaborationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CreatorCollaborationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CreatorCollaborationControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BL-creator-collaboration-routes-20260614-100500/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and
verification. Harvest the worker at most once when useful; do not idle poll.
