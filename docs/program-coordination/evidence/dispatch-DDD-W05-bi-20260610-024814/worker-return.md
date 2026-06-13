# DDD-W05 BI Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-W05
dispatch id: dispatch-DDD-W05-bi-20260610-024814
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 plus uncommitted DDD-W05 BI module changes
assigned task pack: docs/ddd-rewrite/task-packs/05-worker-bi.md

files changed:
- backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
- backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/**
- backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
- backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/adapter/persistence/**
- backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/**
- docs/program-coordination/progress-ledger.md
- docs/program-coordination/dispatch-state.json
- docs/program-coordination/evidence/dispatch-DDD-W05-bi-20260610-024814/worker-return.md

contracts changed:
- Added `BiCatalogFacade` as the stable BI API facade for workspace, dataset, chart, dashboard read model, and permission operations.
- Added API command/view records for BI workspace, dataset field, metric, dataset, chart, dashboard, dashboard readiness/read model, datasource view, permission grants, and access decisions.
- Added domain repository ports: `BiWorkspaceRepository`, `BiDatasetRepository`, `BiChartRepository`, `BiDashboardRepository`, and `BiPermissionRepository`.

old classes migrated:
- Representative BI resource model: legacy dataset/chart/dashboard resource behavior is represented by `BiDataset`, `BiChart`, `BiDashboard`, `BiDatasetField`, `BiMetric`, `BiResourceKey`, and `BiResourceStatus`.
- Representative permission behavior: legacy explicit deny precedence and action/subject matching moved into `BiPermissionPolicy`.
- Representative dashboard readiness behavior moved into `BiDashboardReadinessPolicy`.
- Representative `Bi*DO` and `Bi*Mapper` ownership moved into `adapter.persistence` for workspace, dataset, dataset field, metric, chart, dashboard, dashboard widget, resource permission, datasource schema snapshot, and datasource health snapshot.

new public api:
- `org.chovy.canvas.bi.api.BiCatalogFacade`
- `org.chovy.canvas.bi.api.BiWorkspaceCommand` / `BiWorkspaceView`
- `org.chovy.canvas.bi.api.BiDatasetCommand` / `BiDatasetView`
- `org.chovy.canvas.bi.api.BiChartCommand` / `BiChartView`
- `org.chovy.canvas.bi.api.BiDashboardCommand` / `BiDashboardView` / `BiDashboardReadModelView`
- `org.chovy.canvas.bi.api.BiPermissionGrantCommand` / `BiPermissionGrantView` / `BiPermissionDecisionView`
- `org.chovy.canvas.bi.api.BiDatasourceView`

domain model changes:
- Introduced normalized BI resource keys and resource status value handling.
- Kept dataset/chart/dashboard readiness and permission decisions in the domain layer with no Spring Web, MyBatis, or old `canvas-engine` imports.
- Added datasource health as a domain port/read model placeholder for later datasource runtime integration.

persistence ownership changes:
- Added MyBatis DO and mapper types under `org.chovy.canvas.bi.adapter.persistence`, including `bi_dashboard_widget` for dashboard chart membership.
- Added `BiPersistenceConverter` for domain-to-row conversions and representative JSON field handling.
- Added `MybatisBiCatalogRepository` implementing the BI domain repository ports.

tests run:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`

verification result:
- BI module Maven tests passed: 10 tests, 0 failures, 0 errors.
- DDD guardrails passed; only the pre-existing risk `TypeCompatibility` advisory was reported.

verification output summary/path:
- Maven output: `canvas-context-bi` build success, 10 tests run.
- Guardrail output: package/import/pom checks passed; advisory unchanged from previous risk module output.

evidence artifact paths:
- docs/program-coordination/evidence/dispatch-DDD-W05-bi-20260610-024814/worker-return.md

guardrail checks:
- Domain has no infrastructure imports.
- Non-persistence code does not import DO or Mapper classes.
- New modules do not import old `canvas-engine` internals.
- New module poms do not depend on `canvas-engine`.

failure modes reviewed:
- Missing dataset for chart creation.
- Archived dataset cannot be used for chart creation.
- Explicit user deny overrides role and global allow.
- Missing dashboard chart blocks readiness.
- Draft or missing chart dataset blocks readiness.
- Persistence mapping owns representative legacy BI tables in the BI adapter package.

compatibility evidence:
- `BiCatalogFacade` exposes stable read views intended for later `canvas-web` cutover.
- Persistence adapter keeps representative legacy BI table names intact.
- No direct imports from CDP or warehouse persistence were introduced.

temporary bridges:
- none

open risks:
- Full BI migration remains broad. Portal, subscription, spreadsheet, big screen, query execution, AI planning, publish approval, collaboration, movement, favorites, export, embed tickets, datasource onboarding/runtime, dataset acceleration, and legacy controller cutover remain outside this pilot.
- Dashboard chart membership uses the existing `bi_dashboard_widget` table in the pilot adapter; old controller integration and richer widget layout behavior still need cutover work.
- Datasource onboarding still depends on non-`Bi*` legacy datasource configuration ownership and should be handled by a later coordinator-approved cross-context/API contract.

coordinator actions needed:
- Keep DDD-W05 marked `DONE_WITH_CONCERNS`.
- Assign full BI web/controller cutover and remaining old BI service migration to a later integration/cutover task, likely DDD-C09 or a follow-up BI worker with exact reservations.
- Continue to DDD-W06 conversation if coordination checks remain clean and reservations are disjoint.

ledger update:
- Set DDD-W05 to `DONE_WITH_CONCERNS`.
- Clear active dispatch registry.
- Record BI module tests and DDD guardrail evidence.
- Keep current backend target on the second-wave conversation decision after W05 closure.

rollback path:
- Revert files under `backend/canvas-context-bi/**`.
- Revert `docs/program-coordination/progress-ledger.md`, `docs/program-coordination/dispatch-state.json`, and `docs/program-coordination/evidence/dispatch-DDD-W05-bi-20260610-024814/**` if rolling back the dispatch record.
- Pre-rewrite restore point: `backup/pre-ddd-osg-20260609-222054`.
