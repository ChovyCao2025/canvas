# DDD-C09CI Reservation

date: 2026-06-15
task id: DDD-C09CI
dispatch id: dispatch-DDD-C09CI-warehouse-enterprise-olap-routes-20260615-023800
worker: Kuhn 019ec76d-a4ae-7752-abe1-ad7776cb1350
status: RUNNING

## Scope

Final-module migration for the five legacy `/warehouse/enterprise-olap/evidence` endpoints:

- `POST /warehouse/enterprise-olap/evidence`
- `GET /warehouse/enterprise-olap/evidence/latest`
- `GET /warehouse/enterprise-olap/evidence/proof`
- `POST /warehouse/enterprise-olap/evidence/collect`
- `GET /warehouse/enterprise-olap/evidence/collections`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseEnterpriseOlapEvidenceFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseEnterpriseOlapEvidenceApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseEnterpriseOlapEvidenceCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseEnterpriseOlapEvidenceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseEnterpriseOlapEvidenceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseEnterpriseOlapEvidenceControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Coordinator continues local contract reading and verification preparation without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
