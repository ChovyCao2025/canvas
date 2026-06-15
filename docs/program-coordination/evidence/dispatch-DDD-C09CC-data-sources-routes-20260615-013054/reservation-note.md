# DDD-C09CC Data Sources Routes Reservation

- Reserved at: 2026-06-15T01:30:54+08:00
- Coordinator: Codex
- Worker: Sartre (`019ec72f-87f3-78a3-8e0a-8ceef03a4dc3`)
- Status: RUNNING

## Scope

Migrate the legacy `/canvas/data-sources` route family into final modules without editing `backend/canvas-engine/**` or `pom.xml`.

## Write Scope

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/DataSourceConfigFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/DataSourceConfigApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/DataSourceConfigCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/DataSourceConfigApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/DataSourceConfigController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/DataSourceConfigControllerCompatibilityTest.java`

## Compatibility Contract

- `GET /canvas/data-sources`
- `GET /canvas/data-sources/{id}/tables`
- `POST /canvas/data-sources`
- `PUT /canvas/data-sources/{id}`
- `DELETE /canvas/data-sources/{id}`

Meaningful checks only: pagination/filter forwarding, tenant scoping, JDBC defaults, required field validation, unsupported type handling, password redaction, deterministic table metadata, and `API_001` bad request envelopes.
