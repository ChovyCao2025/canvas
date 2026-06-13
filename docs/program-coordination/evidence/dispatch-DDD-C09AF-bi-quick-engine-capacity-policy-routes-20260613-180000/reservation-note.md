# DDD-C09AF Reservation Note

Date: 2026-06-13 18:00 +08:00

## Selector Context

After DDD-C09AE closeout, cutover preflight still reports `/canvas/bi` as the
largest route gap:

- old canvas-engine web: 142 controllers / 806 endpoints
- current canvas-web: 15 controllers / 59 endpoints
- route group `/canvas/bi`: old 20 controllers / 169 endpoints, current 1
  controller / 19 endpoints
- `cutoverReady: false`

Boyle `019ec06a-1f54-7930-a702-959a2f3faa30` confirmed this is a compact next
BI route-parity scope.

## Target Legacy Routes

From `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiCapacityController.java`:

- `POST /canvas/bi/capacity/quick-engine/alert-policy`
- `POST /canvas/bi/capacity/quick-engine/tenant-pool-policy`

## Target Semantics

- Use final-context `org.chovy.canvas.bi.*` code only.
- Preserve compatibility envelopes and `X-Tenant-Id` / `X-Actor` header
  behavior at the web boundary.
- Preserve legacy request/response JSON field names:
  - alert policy command: `enabled`, `capacityLimitRows`,
    `warningThresholdPercent`, `criticalThresholdPercent`,
    `notificationChannels`, `notificationReceivers`
  - alert policy view: command fields plus `updatedBy`, `updatedAt`
  - tenant pool policy command: `poolKey`, `maxConcurrentQueries`,
    `queueLimit`, `queueTimeoutSeconds`, `poolWeight`
  - tenant pool policy view: command fields plus `updatedBy`, `updatedAt`
- Preserve compact legacy validation shape for thresholds, positive limits,
  pool key normalization, and list normalization.
- Also correct the existing compact summary fixture key from
  `alertPolicy.receivers` to legacy `alertPolicy.notificationReceivers`.
- Do not use old `canvas-engine` domain classes or bridge dependencies.

## Reserved Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineCapacityAlertPolicyCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineCapacityAlertPolicyView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineTenantPoolPolicyCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQuickEngineTenantPoolPolicyView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQuickEngineCapacityCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the actual worker id is recorded in `dispatch-state.json` and the ledger.
