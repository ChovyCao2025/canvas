# DDD-C09BC Reservation Note

Reserved at: 2026-06-14T07:23:57+08:00

Task: DDD-C09BC BI remaining route aliases

Reason: cutover preflight reports `route:/canvas/bi` as the largest remaining top gap, with old BI coverage at 169 endpoints and current final `canvas-web` coverage at 152 endpoints.

Scheduling rule for this batch: spawn a real worker before marking RUNNING, then keep the coordinator on non-overlapping local TDD and verification work. Wait for the worker at most once; on timeout, inspect reserved paths and evidence, close the worker if there is no useful return, and continue locally.

Exact reserved files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`

Target route family:

- remaining `/canvas/bi` compatibility aliases after the existing 152 final endpoints, especially legacy base resource aliases, datasource base aliases, embed resource aliases, portal runtime aliases, and self-service base aliases.

Forbidden:

- No writes under `backend/canvas-engine/**`.
- No POM edits.
- No legacy `org.chovy.canvas.domain`, `dto`, `query`, `dal`, or `TenantContextResolver` dependencies.
- No new BI controller class unless coordinator re-reserves exact paths.
