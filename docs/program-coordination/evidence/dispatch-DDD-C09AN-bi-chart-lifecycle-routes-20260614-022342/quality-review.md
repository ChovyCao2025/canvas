# DDD-C09AN Quality Review

review status: PASS
reviewer: Kuhn `019ec253-33c6-72f0-89ed-288065f1f51e`

## Review Scope

DDD-C09AN read-only quality/spec review for the four legacy BI chart lifecycle
routes:

- `POST /canvas/bi/charts/resources/{chartKey}/publish`
- `DELETE /canvas/bi/charts/resources/{chartKey}`
- `GET /canvas/bi/charts/resources/{chartKey}/versions`
- `POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore`

## Files Reviewed

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiChartLifecycleCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09AN-bi-chart-lifecycle-routes-20260614-022342/worker-return.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09AN-bi-chart-lifecycle-routes-20260614-022342/reservation-note.md`

## Requirements Checked

- Four target routes are present in final `BiCatalogController`.
- Routes call final `BiCatalogFacade` methods only.
- `BiCatalogApplicationService` implements lifecycle behavior without old
  `canvas-engine` service/DTO coupling.
- Forbidden import scan for old BI domain/controller/service names returned no
  matches.
- Tenant/chart scoping is enforced in the in-memory lifecycle catalog key.
- Version listing is newest-first.
- Archive is idempotent and hides archived charts from list/detail.
- Restore creates a draft chart from a stored snapshot and appends a new
  version.
- Compatibility envelope behavior is covered for success and bad request paths.

## Commands

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  - result: BUILD SUCCESS; `BiCatalogApplicationServiceTest` 33/33,
    `BiApiCompatibilityTest` 16/16, `BiCatalogControllerCompatibilityTest`
    26/26.
- Forbidden coupling scan with `rg`
  - result: no matches.

## Findings

None.

## Required Fixes

None.

## Residual Risks

- Chart lifecycle version catalog is process-local in memory. This is allowed by
  the packet, but versions will not survive restart or be shared across app
  instances.
- Restore currently requires the chart to be available before restoring a
  version. Reviewed tests cover restore before archive; archive hiding and
  idempotency are covered separately.

## Ledger Update

Mark `dispatch-DDD-C09AN-bi-chart-lifecycle-routes-20260614-022342` review as
PASS.
