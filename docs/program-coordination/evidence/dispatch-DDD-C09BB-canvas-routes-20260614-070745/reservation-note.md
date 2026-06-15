# DDD-C09BB Reservation Note

Reserved at: 2026-06-14T07:07:45+08:00

Task: DDD-C09BB Canvas route batch

Reason: cutover preflight reports `family:Canvas` as the largest remaining top gap, with old CanvasController at 24 endpoints and current final Canvas web coverage at 6 endpoints.

Scheduling rule for this batch: spawn a real worker before marking RUNNING, then keep the coordinator on non-overlapping local TDD and verification work. Wait for the worker at most once; on timeout, inspect reserved paths and evidence, close the worker if there is no useful return, and continue locally.

Exact reserved files:

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasCompatibilityFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasCompatibilityApplicationService.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasCompatibilityApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`

Existing final mappings excluded from duplication:

- `GET /canvas/{id}/versions`
- `GET /canvas/{id}/versions/{versionId}`
- `POST /canvas/{id}/publish`
- `POST /canvas/{id}/offline`
- `POST /canvas/{id}/archive`
- `POST /canvas/{id}/kill`
- Canvas DSL routes under `/canvas/dsl/**`
- Project folder metadata routes under `/canvas/{id}/project-folder-metadata`

Forbidden:

- No writes under `backend/canvas-engine/**`.
- No POM edits.
- No legacy `org.chovy.canvas.domain`, `dto`, `query`, `dal`, or `TenantContextResolver` dependencies.
