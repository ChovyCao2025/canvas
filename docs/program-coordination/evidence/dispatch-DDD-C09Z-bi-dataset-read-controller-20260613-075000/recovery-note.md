# DDD-C09Z BI Dataset Read Controller Reservation

Reserved at: 2026-06-13T07:50:00+08:00

## Scope

Implement compact read-only BI dataset route parity:

- `GET /canvas/bi/datasets/resources`
- `GET /canvas/bi/datasets/resources/{datasetKey}`

## Legacy Behavior Reference

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java`
- `BiDatasetController#list()`
- `BiDatasetController#get(String datasetKey)`
- `BiDatasetResourceService#listResources(Long tenantId)`
- `BiDatasetResourceService#getResource(Long tenantId, String datasetKey)`

Observed behavior:

- no request body
- tenant-scoped read
- old no-resolver fallback uses tenant `0L`; current `canvas-web` BI seed convention uses optional `X-Tenant-Id` defaulting to `7L`
- old list resolves default workspace `marketing_canvas`
- old list excludes `ARCHIVED`
- old list orders `updatedAt DESC`, then `datasetKey ASC`
- old detail resolves by `datasetKey`; old service falls back from tenant rows to tenant `0L`, then built-in registry
- blocking reads run on `Schedulers.boundedElastic()` in the old controller
- compatibility envelope is `code=0`, `message=success`, `data=...`
- `IllegalArgumentException` maps to `400` with `errorCode=API_001` in current final `canvas-web` BI seed pattern

## Exact Reserved Files

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDatasetRepository.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/adapter/persistence/MybatisBiCatalogRepository.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

## Verification Before Reservation

- Dalton selector recommended `DDD-C09Z` after current route-gap preflight.
- `dispatch-state` and `progress-ledger` agreed there were no active dispatches after `DDD-C09Y`.
- Coordination validators passed before reservation.
- Preflight reported current `canvas-web` 15 controllers / 47 endpoints, `/canvas/bi` still the largest route gap, and global `cutoverReady=false`.
- Exact target files exist as untracked prior DDD/OSG work; they are reserved as the only worker write scope.

## Out Of Scope

- dataset draft/publish/archive/version/restore routes
- chart/dashboard read parity beyond existing final seed
- BI datasource, query, AI, subscription, portal, big-screen, permission expansions
- old `MarketingBiDatasetRegistry` migration unless already required by focused tests
- edits under `backend/canvas-engine`

## Coordinator Recovery

Recovered at: 2026-06-13T08:24:30+08:00

Coordinator inspection found the worker implementation preserved tenant filtering,
`ARCHIVED` exclusion, ordering, and tenant-0 detail fallback, but it omitted the
old `BiDatasetResourceService` default workspace boundary for `marketing_canvas`.

RED evidence:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest#repositoryListDatasetResourcesFiltersTenantExcludesArchivedAndOrdersCatalogRows,repositoryDatasetDetailFallsBackFromTenantToTenantZero test
```

Observed failure: `repositoryListDatasetResourcesFiltersTenantExcludesArchivedAndOrdersCatalogRows`
captured dataset SQL with tenant and status predicates but without
`workspace_id =`.

GREEN recovery:

- added final `MybatisBiCatalogRepository` default workspace lookup for
  `marketing_canvas`, matching the old tenant-or-tenant-0 lookup behavior
- constrained BI dataset list and detail queries by the resolved workspace id
- extended mapper-level tests to assert `workspace_id =` on list and detail
  query SQL

Focused rerun evidence:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest#repositoryListDatasetResourcesFiltersTenantExcludesArchivedAndOrdersCatalogRows,repositoryDatasetDetailFallsBackFromTenantToTenantZero test
```

Result: BUILD SUCCESS for the selected repository list test; full DDD-C09Z
verification remains required before review/closeout.
