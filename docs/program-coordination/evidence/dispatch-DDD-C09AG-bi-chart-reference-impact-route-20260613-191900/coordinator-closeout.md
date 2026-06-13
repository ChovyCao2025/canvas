# DDD-C09AG Coordinator Closeout

Date: 2026-06-13
Task: DDD-C09AG BI chart reference impact route seed
Dispatch: `dispatch-DDD-C09AG-bi-chart-reference-impact-route-20260613-191900`
Status: DONE_WITH_CONCERNS

## Result

Closed DDD-C09AG after Meitner selector recommendation, Carson code-writing implementation with RED/GREEN evidence, coordinator verification, and Lagrange PASS_WITH_CONCERNS review.

Implemented compact modular route coverage for:

- `GET /canvas/bi/charts/resources/{chartKey}/impact`

Compatibility details preserved:

- legacy compatibility envelope shape
- missing `X-Tenant-Id` defaults to `7L`
- response fields `chartKey`, `chartName`, `datasetKey`, `dashboards`, `portals`, `subscriptions`
- dashboard reference fields `dashboardKey`, `title`, `widgetKey`, `widgetTitle`, `status`
- non-null empty `portals` and `subscriptions` arrays for the compact seed
- missing/archived chart maps to HTTP 400 `API_001` with `BI chart not found: {chartKey}`

## Verification

- `cd backend && mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  - Result: BUILD SUCCESS, 53 focused tests passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: current `canvas-web` 15 controllers / 62 endpoints; `/canvas/bi` 1 controller / 22 endpoints; compatibility tests presentCount 7 / missingCount 0; `cutoverReady=false`.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: passed before closeout edits.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Result: passed before closeout edits.
- `rg -n "org\\.chovy\\.canvas\\.domain\\.bi|canvas-engine" <DDD-C09AG exact BI source/test paths> -S`
  - Result: no matches, exit 1.
- `git diff --check -- <DDD-C09AG exact files and evidence>`
  - Result: passed before closeout edits.

## Accepted Concerns

- Dashboard references are dashboard-level synthesized references because the current final `BiDashboardRepository` exposes `BiDashboard.chartKeys`, not widget key/title reference records.
- Full legacy widget-level impact parity requires a later repository/domain contract expansion around dashboard widgets.
- Portal and subscription reference arrays are intentionally empty until those resource families move.
- Broader BI route parity and global DDD-C09 cutover remain blocked.

## Rollback

Remove DDD-C09AG edits from the exact reserved BI chart impact source/test files only. Leave prior BI catalog, chart read, dashboard, query dataset, preset, and capacity route seeds intact.

