# DDD-C09CF Reservation

date: 2026-06-15
task id: DDD-C09CF
dispatch id: dispatch-DDD-C09CF-tag-imports-routes-20260615-020911
worker: Carson 019ec752-904b-70c0-a060-fe9d998f4a64
status: RUNNING

## Scope

Final-module migration for the five legacy `/canvas/tag-imports` endpoints:

- `POST /canvas/tag-imports/api-push`
- `GET /canvas/tag-imports/excel-template`
- `POST /canvas/tag-imports/excel`
- `GET /canvas/tag-imports/batches`
- `GET /canvas/tag-imports/batches/{id}/errors`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/TagImportFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/TagImportApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/TagImportCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/TagImportApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/TagImportController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/TagImportControllerCompatibilityTest.java`

## Coordination Notes

- Spawned real worker before marking the dispatch RUNNING.
- Coordinator will continue local TDD and verification without idle waiting.
- Tests must cover behavior and compatibility risks only, not ceremonial wiring.
- `backend/canvas-engine/**` and `pom.xml` are out of scope.
