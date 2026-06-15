# DDD-C09BJ AB Experiments Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/canvas/ab-experiments` as the top current route gap after
DDD-C09BI:

- old controllers: 2
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy references:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentController.java`
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java`

## Legacy Routes

- `GET /canvas/ab-experiments`
- `POST /canvas/ab-experiments`
- `PUT /canvas/ab-experiments/{id}`
- `DELETE /canvas/ab-experiments/{id}`
- `GET /canvas/ab-experiments/{id}/groups`
- `POST /canvas/ab-experiments/{id}/groups`
- `PUT /canvas/ab-experiments/{id}/groups/{groupId}`
- `DELETE /canvas/ab-experiments/{id}/groups/{groupId}`
- `POST /canvas/ab-experiments/{experimentId}/governance/evaluate`

## Exact Reserved Files

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AbExperimentFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AbExperimentApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AbExperimentCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/AbExperimentApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AbExperimentController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/AbExperimentControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BJ-ab-experiments-routes-20260614-090800/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and verification.
Harvest the worker at most once when useful; do not idle poll.
