# DDD-C09BE Reservation Note

Date: 2026-06-14

## Selection

After DDD-C09BD closeout, `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported `route:/warehouse/privacy` as the top route gap with 2 old controllers, 15 old endpoints, and 0 current endpoints.

Legacy reference files are read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyTombstoneController.java`

## Exact Reserved Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehousePrivacyFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehousePrivacyApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehousePrivacyCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehousePrivacyApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehousePrivacyController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehousePrivacyControllerCompatibilityTest.java`

## Scheduler Rule

Spawn a real code-writing worker before moving the dispatch to RUNNING. The coordinator must not repeatedly wait on the worker: wait at most once, inspect reserved paths/evidence if timed out, close the worker if no useful result exists, and recover locally.
