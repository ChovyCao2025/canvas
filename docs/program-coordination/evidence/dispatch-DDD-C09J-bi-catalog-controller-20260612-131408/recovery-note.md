# DDD-C09J Recovery Note

date: 2026-06-12T13:14:08+08:00

dispatch id: `dispatch-DDD-C09J-bi-catalog-controller-20260612-131408`

task id: `DDD-C09J`

selection basis:
- Cold-start recovery found no active dispatches in `dispatch-state.json` and `progress-ledger.md`.
- `node tools/program-coordination/check-dispatch-state.mjs .` and `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed before reservation.
- `routeGapSummary` reports top gap `route:/canvas/bi` with 20 old controllers, 169 old endpoints, and 0 current production controllers/endpoints.
- Full BI route migration is too broad for one shared-worktree dispatch.
- The compact, gate-safe slice is a production `BiCatalogController` under `canvas-web` covering the seven BI catalog/permission routes already exercised by `BiApiCompatibilityTest`.

pre-dispatch evidence:
- Branch: `main`
- Base SHA: `01aac65697d524f4cf2e92d954db088895631004`
- G0B backup manifest exists at `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`.
- Target files do not exist before reservation:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- Existing `backend/canvas-web/pom.xml` already depends on `canvas-context-bi`; no POM edit is reserved.
- Read-only selector Carver `019eba3a-5e3d-7481-ac84-7f46d2f922f9` timed out once and was closed; local inspection provided enough evidence to recover without `NEEDS_CONTEXT`.

reserved write scope:
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`

required behavior:
- Add a real production WebFlux controller that delegates only to `org.chovy.canvas.bi.api.BiCatalogFacade`.
- Preserve the stable compatibility envelope for these existing BI catalog routes:
  - `POST /canvas/bi/workspaces`
  - `POST /canvas/bi/datasets/resources/{datasetKey}/draft`
  - `POST /canvas/bi/charts/resources/{chartKey}/draft`
  - `POST /canvas/bi/dashboards/resources/{dashboardKey}/draft`
  - `GET /canvas/bi/dashboards/resources/{dashboardKey}`
  - `POST /canvas/bi/permissions/resources`
  - `GET /canvas/bi/permissions/effective-access`
- Do not import old `canvas-engine` classes, old `org.chovy.canvas.domain.bi.*` classes, mappers, DOs, or persistence adapters.

rollback:
- Remove the two reserved files and this evidence directory.
