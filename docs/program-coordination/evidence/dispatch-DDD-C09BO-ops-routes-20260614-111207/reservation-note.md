# DDD-C09BO Ops Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/ops` as the top current route gap after DDD-C09BN:

- old controllers: 1
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy reference:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`

## Legacy Routes

- `POST /ops/cache/invalidate/{id}`
- `POST /ops/recovery/runtime-state/rebuild`
- `GET /ops/runtime/status`
- `GET /ops/audit-events`
- `POST /ops/canvas/{id}/pause`
- `POST /ops/canvas/{id}/offline`
- `POST /ops/canvas/{id}/resume`
- `POST /ops/canvas/{id}/kill`
- `POST /ops/canvas/{id}/rollback`

## Exact Reserved Files

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/OpsFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/OpsApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/OpsCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/OpsApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/ops/OpsControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BO-ops-routes-20260614-111207/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and
verification. Harvest the worker at most once when useful; do not idle poll.
