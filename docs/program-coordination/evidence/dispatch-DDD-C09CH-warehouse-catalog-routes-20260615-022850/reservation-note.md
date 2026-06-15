# DDD-C09CH Reservation

date: 2026-06-15
task id: DDD-C09CH
dispatch id: dispatch-DDD-C09CH-warehouse-catalog-routes-20260615-022850
worker: Lagrange 019ec764-9802-7521-a891-99e8a1285d83
status: RUNNING

## Scope

Final-module migration for the five legacy `/warehouse/catalog` endpoints:

- `GET /warehouse/catalog/datasets`
- `POST /warehouse/catalog/datasets`
- `POST /warehouse/catalog/lineage`
- `GET /warehouse/catalog/datasets/{datasetKey}/lineage`
- `GET /warehouse/catalog/datasets/{datasetKey}/lineage/transitive`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseCatalogFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseCatalogApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseCatalogCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseCatalogControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Coordinator will continue local TDD and verification without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
