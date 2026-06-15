# DDD-C09BM Computed Tags Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/cdp/computed-tags` as the top current route gap after
DDD-C09BL:

- old controllers: 1
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy reference:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java`

## Legacy Routes

- `GET /cdp/computed-tags`
- `POST /cdp/computed-tags`
- `POST /cdp/computed-tags/{tagCode}/preview`
- `POST /cdp/computed-tags/{tagCode}/activate`
- `POST /cdp/computed-tags/{tagCode}/pause`
- `POST /cdp/computed-tags/{tagCode}/run`
- `GET /cdp/computed-tags/{tagCode}/runs`
- `GET /cdp/computed-tags/{tagCode}/lineage`
- `POST /cdp/computed-tags/{tagCode}/impact-check`

## Exact Reserved Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedTagFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedTagCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedTagController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpComputedTagControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BM-computed-tags-routes-20260614-102400/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and
verification. Harvest the worker at most once when useful; do not idle poll.
