# DDD-C09CG Reservation

date: 2026-06-15
task id: DDD-C09CG
dispatch id: dispatch-DDD-C09CG-message-deliveries-routes-20260615-021728
worker: Lovelace 019ec75a-3904-7550-b949-37042f765add
status: RUNNING

## Scope

Final-module migration for the five legacy `/message-deliveries` endpoints:

- `GET /message-deliveries`
- `GET /message-deliveries/{id}`
- `GET /message-deliveries/{id}/receipts`
- `POST /message-deliveries/{id}/replay`
- `POST /message-deliveries/reconcile`

Exact reserved files:

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/MessageDeliveryFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/MessageDeliveryApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/MessageDeliveryCatalog.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/MessageDeliveryApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/MessageDeliveryController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/MessageDeliveryControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Coordinator will continue local TDD and verification without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
