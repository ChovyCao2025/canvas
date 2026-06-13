# DDD-C09AD Coordinator Closeout

Date: 2026-06-13 16:36 +08:00

## Status

DDD-C09AD is closed as `DONE_WITH_CONCERNS`.

## Dispatch Summary

- James `019ec010-b6a5-7413-92ca-e19364f8a3ca` was spawned as the real
  code-writing worker before the dispatch moved to `RUNNING`.
- James timed out once and was closed with previous status `running`, but
  exact-scope code and tests were present.
- Dewey `019ec01b-9b67-7641-99ad-d98738835f71` completed read-only review with
  `PASS` and no findings.

## Files Changed

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetWidgetView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetFilterView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetInteractionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDashboardPresetCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

## Result

- `BiCatalogController` now exposes:
  - `GET /canvas/bi/dashboards/presets`
  - `GET /canvas/bi/dashboards/presets/{dashboardKey}`
- `BiCatalogFacade` and `BiCatalogApplicationService` expose final
  dashboard-preset catalog methods.
- `BiDashboardPresetCatalog` holds the final-context built-in
  `canvas-effect` preset without old `canvas-engine` imports.
- Unknown keys preserve
  `IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey)`,
  which the web compatibility envelope maps to `API_001`.

## Verification

```bash
export JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
```

Result: `BUILD SUCCESS`.

- `BiCatalogApplicationServiceTest`: 18 tests, 0 failures, 0 errors
- `BiApiCompatibilityTest`: 9 tests, 0 failures, 0 errors
- `BiCatalogControllerCompatibilityTest`: 14 tests, 0 failures, 0 errors

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result: passed and reported `cutoverReady: false`, current `canvas-web` at 15
controllers / 57 endpoints, compatibility tests present 7 / missing 0, and
`route:/canvas/bi` current endpoint count 17.

```bash
node tools/program-coordination/check-dispatch-state.mjs .
```

Result: `{ "ok": true }`.

```bash
git diff --check -- docs/program-coordination/dispatch-state.json docs/program-coordination/progress-ledger.md docs/program-coordination/evidence/dispatch-DDD-C09AD-bi-dashboard-preset-catalog-routes-20260613-161200 backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetWidgetView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetFilterView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardPresetInteractionView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDashboardPresetCatalog.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
```

Result: passed.

## Review

Dewey `019ec01b-9b67-7641-99ad-d98738835f71` returned `PASS` with no findings.
Review confirmed final routes, no old-domain imports, exact unknown-key
exception behavior, and API_001 envelope mapping.

## Accepted Concerns

- James timed out and did not produce a worker-return packet before closeout.
- The dispatch covers only compact BI dashboard preset catalog routes; broader
  `/canvas/bi` route parity and global DDD-C09 cutover readiness remain out of
  scope.
