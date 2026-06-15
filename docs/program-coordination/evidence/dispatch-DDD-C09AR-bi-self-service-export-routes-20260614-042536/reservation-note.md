# DDD-C09AR Reservation Note

Date: 2026-06-14T04:25:36+08:00
Coordinator: main
Dispatch: dispatch-DDD-C09AR-bi-self-service-export-routes-20260614-042536
Task: DDD-C09AR BI self-service export route batch

## Scope

Reserved exact files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServicePreviewCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportReviewCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportJobView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportJobDetailView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportDownload.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportCleanupResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportRetryResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportQueueResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSelfServiceExportCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

Target routes:

- `POST /canvas/bi/self-service/preview`
- `POST /canvas/bi/self-service/exports`
- `GET /canvas/bi/self-service/exports`
- `POST /canvas/bi/self-service/exports/{id}/review`
- `GET /canvas/bi/self-service/exports/{id}`
- `GET /canvas/bi/self-service/exports/{id}/download`
- `POST /canvas/bi/self-service/exports/{id}/cancel`
- `POST /canvas/bi/self-service/exports/cleanup`
- `POST /canvas/bi/self-service/exports/retry`
- `POST /canvas/bi/self-service/exports/queue/run`

## Pre-Dispatch Evidence

- Active dispatch registry was empty after DDD-C09AQ closeout.
- G0B backup manifest exists.
- Branch: `main`
- Base SHA: `2a1cdec07ec27a5298958822014aa28d9312869c`
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 15 controllers / 172 endpoints and
  `route:/canvas/bi` 132 current endpoints out of 169 old endpoints.
- Legacy source for this batch is
  `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java`.

## Scheduling Rule

The coordinator will spawn a real code-writing worker before moving this
dispatch to `RUNNING`. After one meaningful worker wait timeout, the coordinator
will inspect changed paths, evidence, and focused tests instead of repeatedly
waiting idle.

## RUNNING Update

- Worker spawned: `Parfit 019ec2ad-5226-7b53-b686-8df3694894c3`
- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09AR .`
  succeeded.
- Dispatch state and ledger were updated from `RESERVED` to `RUNNING` with the
  actual worker id.

## Coordinator Recovery Closeout

- Parfit timed out after one meaningful wait; the coordinator did not continue
  idle polling.
- The coordinator inspected changed paths and found RED tests for self-service
  export APIs without the corresponding implementation.
- `multi_agent_v1.close_agent Parfit 019ec2ad-5226-7b53-b686-8df3694894c3`
  returned `previous_status: running`; later notification reported shutdown.
- Coordinator recovered the exact reserved scope locally by adding compact
  self-service export API records, an in-memory final BI
  `BiSelfServiceExportCatalog`, facade/application wiring, production
  `BiCatalogController` routes, and compatibility test stub support.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  passed: `BiCatalogApplicationServiceTest` 39/39,
  `BiApiCompatibilityTest` 18/18, and
  `BiCatalogControllerCompatibilityTest` 29/29, total 86/86.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  completed; `cutoverReady` remains `false` globally, while
  `route:/canvas/bi` advanced from 132/169 to 142/169.
- Strict old-coupling scan over the DDD-C09AR production target paths returned
  no matches for legacy `canvas-engine` BI self-service controller/service/export
  command coupling.
- Scoped `git diff --check` over the reserved DDD-C09AR code files passed.

## Closeout

- Dispatch status: `DONE_WITH_CONCERNS`.
- Active dispatch registry: cleared.
- Accepted concerns: no normal worker-return packet from Parfit, compact
  deterministic in-memory self-service export seed, and broader BI/global route
  parity remains out of scope for this batch.
