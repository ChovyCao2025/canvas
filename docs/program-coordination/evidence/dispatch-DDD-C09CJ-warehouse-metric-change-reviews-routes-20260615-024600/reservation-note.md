# DDD-C09CJ Reservation

date: 2026-06-15
task id: DDD-C09CJ
dispatch id: dispatch-DDD-C09CJ-warehouse-metric-change-reviews-routes-20260615-024600
worker: Mill 019ec775-642f-7e62-b9bf-18dd06362e0d
status: RUNNING

## Scope

Final-module migration for the five legacy `/warehouse/metric-change-reviews` endpoints:

- `GET /warehouse/metric-change-reviews`
- `POST /warehouse/metric-change-reviews`
- `POST /warehouse/metric-change-reviews/{reviewId}/approve`
- `POST /warehouse/metric-change-reviews/{reviewId}/reject`
- `POST /warehouse/metric-change-reviews/{reviewId}/apply`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseMetricChangeReviewFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseMetricChangeReviewApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseMetricChangeReviewCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseMetricChangeReviewApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseMetricChangeReviewController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseMetricChangeReviewControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Hume completed read-only legacy contract inspection before this reservation.
- Coordinator continues local contract and verification preparation without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
