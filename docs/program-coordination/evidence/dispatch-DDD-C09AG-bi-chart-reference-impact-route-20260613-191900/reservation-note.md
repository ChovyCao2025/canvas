# DDD-C09AG Reservation

Date: 2026-06-13
Dispatch: `dispatch-DDD-C09AG-bi-chart-reference-impact-route-20260613-191900`
Task: DDD-C09AG BI chart reference impact route seed
Mode: code-writing
Status: RESERVED

## Selector Evidence

Read-only selector Meitner `019ec0af-0e8e-78a1-9da1-bc6771851a27` recommended a compact single-route BI continuation:

- `GET /canvas/bi/charts/resources/{chartKey}/impact`

The route is read-only and can be backed by existing modular BI chart/dashboard surfaces. Portal and subscription references may remain compatibility-shaped empty lists until those resource families move.

## Exact Reserved Files

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiChartReferenceImpactView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiChartDashboardReferenceView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiChartPortalReferenceView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiChartSubscriptionReferenceView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

## Legacy Contract To Preserve

- Compatibility envelope: success uses `code=0`, `message=success`, `data`; `IllegalArgumentException` maps to HTTP 400 with `errorCode=API_001`.
- Tenant default follows current BI cutover convention: missing `X-Tenant-Id` defaults to `7L`.
- Response fields: `chartKey`, `chartName`, `datasetKey`, `dashboards`, `portals`, `subscriptions`.
- `dashboards[]`: `dashboardKey`, `title`, `widgetKey`, `widgetTitle`, `status`.
- `portals[]`: `portalKey`, `name`, `menuKey`, `menuTitle`, `status`.
- `subscriptions[]`: `subscriptionKey`, `name`, `enabled`.
- Empty reference groups must be empty arrays, not `null`.
- Missing or archived chart impact lookup should fail with `BI chart not found: {chartKey}`.

## Pre-dispatch Verification

- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: passed before reservation.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Result: passed before reservation.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: `canvas-web` 15 controllers / 61 endpoints; `/canvas/bi` 1 controller / 21 endpoints; `cutoverReady=false`.

## Expected Focused Verification

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
```

## Next Step

Spawn a real code-writing worker, then update the active dispatch row from RESERVED to RUNNING with the actual worker nickname and id.

