# DDD-C09AA BI Chart Read Controller Reservation

Reserved at: 2026-06-13T08:45:00+08:00

## Scope

Implement compact read-only BI chart route parity:

- `GET /canvas/bi/charts/resources`
- `GET /canvas/bi/charts/resources/{chartKey}`

## Legacy Behavior Reference

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java`
- `BiChartController#list()`
- `BiChartController#get(String chartKey)`
- `BiChartResourceService#list(Long tenantId)`
- `BiChartResourceService#get(Long tenantId, String chartKey)`

Observed behavior:

- no request body
- no required `workspaceId` query parameter
- tenant-scoped read; current final `canvas-web` BI seed convention uses optional `X-Tenant-Id` defaulting to `7L`
- resolves default workspace key `marketing_canvas`
- list excludes `ARCHIVED`
- list order: `updatedAt DESC`, then `chartKey ASC`
- detail resolves by `chartKey` within the tenant/default workspace and throws `IllegalArgumentException` when missing
- blocking reads run on `Schedulers.boundedElastic()` in the old controller
- compatibility envelope is `code=0`, `message=success`, `data=...`
- `IllegalArgumentException` maps to HTTP 400 with `errorCode=API_001` in current final `canvas-web` BI seed pattern

## Exact Reserved Files

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiChartRepository.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/adapter/persistence/MybatisBiCatalogRepository.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

## Verification Before Reservation

- Cold-start `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- Cold-start `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- Preflight reported current `canvas-web` 15 controllers / 49 endpoints, `/canvas/bi` still the largest route gap, and global `cutoverReady=false`.
- Gauss `019ebe31-755e-7c00-b227-8fd0a07e0eab` recommended BI chart read parity as the next compact slice.

## Out Of Scope

- `GET /canvas/bi/charts/resources/{chartKey}/impact`
- chart draft/publish/archive/version/restore routes
- dashboard import/export/clone/runtime state
- datasource, query, AI, self-service, spreadsheet, portal, subscription, capacity, resource collaboration/favorite/movement/transfer workflows
- edits under `backend/canvas-engine`

## Worker Spawn

Spawned at: 2026-06-13T08:49:00+08:00

Worker: Wegener `019ebe3a-b371-7281-a4dc-a8096052ba0f`

The dispatch was moved to `RUNNING` only after the real code-writing worker was
spawned and the actual worker id was recorded in `dispatch-state.json` and this
ledger evidence.
