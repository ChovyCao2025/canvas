# DDD-C09AD Reservation Note

Date: 2026-06-13 16:12 +08:00

## Selector Context

After DDD-C09AC closeout, cutover preflight still reports `/canvas/bi` as the
largest route gap:

- old canvas-engine web: 142 controllers / 806 endpoints
- current canvas-web: 15 controllers / 55 endpoints
- route group `/canvas/bi`: old 20 controllers / 169 endpoints, current 1
  controller / 15 endpoints
- `cutoverReady: false`

## Target Legacy Routes

From `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`:

- `GET /canvas/bi/dashboards/presets`
- `GET /canvas/bi/dashboards/presets/{dashboardKey}`

## Target Semantics

- Use final-context `org.chovy.canvas.bi.*` code only.
- Return compact dashboard preset catalog views with stable dashboard key,
  title, description, dataset key, widgets, filters, interactions,
  subscription channels, and embed scopes.
- Preserve unknown-key behavior as
  `IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey)`,
  which the web envelope maps to `API_001`.
- Do not use old `canvas-engine` domain classes or bridge dependencies.

## Reserved Scope

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

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the actual worker id is recorded in `dispatch-state.json` and the ledger.
