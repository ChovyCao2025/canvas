# DDD-C09BQ Warehouse Availability Route Aliases Reservation

Date: 2026-06-14T12:39:29+08:00

Coordinator: main thread

## Classification

CONTINUE. `dispatch-state.json` validated with `activeDispatches` empty after
DDD-C09BP closeout. Fresh preflight reported `route:/warehouse/availability` as
the top route gap with old 3 controllers / 8 endpoints and current 0 / 0.

## Reserved Scope

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseAvailabilityFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseAvailabilityApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseAvailabilityCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseAvailabilityApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAvailabilityController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseAvailabilityControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BQ-warehouse-availability-routes-20260614-123929/**`

## Target Routes

- `GET /warehouse/availability`
- `POST /warehouse/availability/assets`
- `GET /warehouse/availability/assets`
- `POST /warehouse/availability/contracts`
- `GET /warehouse/availability/contracts`
- `POST /warehouse/availability/contracts/{contractKey}/evaluate`
- `POST /warehouse/availability/incidents/scan`
- `POST /warehouse/availability/consumer-incidents/scan`

## Scheduler Rule

Spawn a real worker before marking RUNNING. The worker is a bounded sidecar; the
coordinator continues local RED/GREEN implementation and verification without
idle waiting. At most one wait is allowed before inspecting paths/evidence/tests
and closing or integrating the worker.
