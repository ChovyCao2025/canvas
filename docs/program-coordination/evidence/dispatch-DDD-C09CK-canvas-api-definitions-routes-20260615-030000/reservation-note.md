# DDD-C09CK Reservation

date: 2026-06-15
task id: DDD-C09CK
dispatch id: dispatch-DDD-C09CK-canvas-api-definitions-routes-20260615-030000
worker: Ramanujan 019ec77d-17e7-7392-ba71-a2c50de99d10
status: RUNNING

## Scope

Final-module migration for the four legacy `/canvas/api-definitions` endpoints:

- `GET /canvas/api-definitions`
- `POST /canvas/api-definitions`
- `PUT /canvas/api-definitions/{id}`
- `DELETE /canvas/api-definitions/{id}`

Exact reserved files:

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ApiDefinitionFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/ApiDefinitionApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/ApiDefinitionCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/ApiDefinitionApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/ApiDefinitionController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/ApiDefinitionControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Coordinator continues local legacy contract reading and verification preparation without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
