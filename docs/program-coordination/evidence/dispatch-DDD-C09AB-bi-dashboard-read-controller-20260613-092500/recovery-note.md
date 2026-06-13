# DDD-C09AB Reservation Recovery Note

Date: 2026-06-13 09:25 +08:00

Coordinator recovered from session compaction with DDD-C09AB already reserved in
`dispatch-state.json` but not yet fully mirrored into the ledger or evidence
directory.

Reserved scope:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDashboardRepository.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/adapter/persistence/MybatisBiCatalogRepository.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

Target parity:

- `GET /canvas/bi/dashboards/resources`
- `GET /canvas/bi/dashboards/resources/{dashboardKey}` without `workspaceId`

The existing richer dashboard read-model route must remain available through
`GET /canvas/bi/dashboards/resources/{dashboardKey}?workspaceId=...`.

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the actual worker id is recorded.
