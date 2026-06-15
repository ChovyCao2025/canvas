# DDD-C09BK Warehouse Tables Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/warehouse/tables` as the top current route gap after
DDD-C09BJ:

- old controllers: 2
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy references:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentController.java`
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java`

## Legacy Routes

- `GET /warehouse/tables/contracts`
- `POST /warehouse/tables/contracts`
- `POST /warehouse/tables/contracts/{tableKey}/inspect`
- `POST /warehouse/tables/inspect`
- `POST /warehouse/tables/contracts/{tableKey}/inspect-live`
- `POST /warehouse/tables/inspect-live`
- `POST /warehouse/tables/contracts/{tableKey}/remediation-plan`
- `POST /warehouse/tables/remediation-plan`
- `POST /warehouse/tables/incidents/scan`

## Exact Reserved Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseTableFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseTableApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseTableCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseTableApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseTableController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseTableControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BK-warehouse-tables-routes-20260614-094500/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and
verification. Harvest the worker at most once when useful; do not idle poll.
