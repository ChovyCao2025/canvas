# DDD-C09CE Tag Import Sources Routes Reservation

- Reserved at: 2026-06-15T01:58:41+08:00
- Coordinator: Codex
- Worker: Nietzsche (`019ec748-ee51-7f42-a5fd-41dc07105141`)
- Status: RUNNING

## Scope

Migrate the legacy `/canvas/tag-import-sources` route family into final modules without editing `backend/canvas-engine/**` or `pom.xml`.

## Write Scope

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/TagImportSourceFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/TagImportSourceApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/TagImportSourceCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/TagImportSourceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/TagImportSourceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/TagImportSourceControllerCompatibilityTest.java`

## Compatibility Contract

- `GET /canvas/tag-import-sources`
- `POST /canvas/tag-import-sources`
- `PUT /canvas/tag-import-sources/{id}`
- `DELETE /canvas/tag-import-sources/{id}`
- `POST /canvas/tag-import-sources/{id}/run`

Meaningful checks only: route shape, enabled filtering, source defaults/validation, update/delete behavior, run result shape, disabled/missing source errors, and `API_001` bad request envelopes.
