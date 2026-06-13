# DDD-C09AE Reservation Note

Date: 2026-06-13 17:00 +08:00

## Selector Context

After DDD-C09AD closeout, cutover preflight still reports `/canvas/bi` as the
largest route gap:

- old canvas-engine web: 142 controllers / 806 endpoints
- current canvas-web: 15 controllers / 57 endpoints
- route group `/canvas/bi`: old 20 controllers / 169 endpoints, current 1
  controller / 17 endpoints
- `cutoverReady: false`

## Target Legacy Routes

From `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiCapacityController.java`:

- `GET /canvas/bi/capacity/quick-engine`
- `GET /canvas/bi/capacity/quick-engine/queue`

The legacy POST alert-policy and tenant-pool-policy routes are explicitly out
of scope for this dispatch.

## Target Semantics

- Use final-context `org.chovy.canvas.bi.*` code only.
- Preserve compatibility envelopes and `X-Tenant-Id` header behavior at the web
  boundary.
- Preserve `limit` query parameter defaults and clamping in a compact read-only
  final-context capacity catalog.
- Preserve queue filtering by optional `poolKey` and `status`.
- Do not use old `canvas-engine` domain classes or bridge dependencies.

## Reserved Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineCapacitySummaryView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEnginePoolView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineQueueSnapshotView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineQueueItemView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQuickEngineCapacityCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the actual worker id is recorded in `dispatch-state.json` and the ledger.
