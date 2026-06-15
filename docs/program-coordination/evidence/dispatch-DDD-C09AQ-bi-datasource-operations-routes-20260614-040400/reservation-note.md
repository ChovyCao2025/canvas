# DDD-C09AQ Reservation Note

Date: 2026-06-14T04:04:00+08:00
Coordinator: main
Dispatch: dispatch-DDD-C09AQ-bi-datasource-operations-routes-20260614-040400
Task: DDD-C09AQ BI datasource operations route batch

## Scope

Reserved exact files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceConnectorView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceOnboardingCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceOnboardingView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceFileMaterializationResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceConnectionTestResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceCredentialRotationCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceCredentialRotationView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceSchemaPreviewView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceApiPreviewCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceApiPreviewView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceSchemaSnapshotView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDatasourceOperationsCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

Target routes:

- `GET /canvas/bi/datasources/connectors`
- `GET /canvas/bi/datasources/onboarding`
- `POST /canvas/bi/datasources/onboarding`
- `POST /canvas/bi/datasources/file-upload`
- `POST /canvas/bi/datasources/file-upload/materialize`
- `PUT /canvas/bi/datasources/onboarding/{id}`
- `POST /canvas/bi/datasources/{id}/connection-test`
- `POST /canvas/bi/datasources/{id}/credential-rotation`
- `GET /canvas/bi/datasources/{id}/schema-preview`
- `POST /canvas/bi/datasources/{id}/api-preview`
- `POST /canvas/bi/datasources/{id}/schema-sync`
- `GET /canvas/bi/datasources/{id}/schema-snapshot`
- `GET /canvas/bi/datasources/{id}/schema-snapshots`

## Pre-Dispatch Evidence

- Active dispatch registry was empty after DDD-C09AP closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 15 controllers / 159 endpoints and
  `route:/canvas/bi` 119 current endpoints out of 169 old endpoints.
- Mapping diff against legacy BI controllers showed
  `BiDatasourceController.java` has 13 missing routes out of 13.
- G0B backup manifest exists.
- Base branch: `main`
- Base SHA: `2a1cdec07ec27a5298958822014aa28d9312869c`

## Scheduling Rule

The coordinator will spawn a real code-writing worker before moving this
dispatch to `RUNNING`. After one meaningful worker wait timeout, the coordinator
will inspect changed paths, evidence, and focused tests instead of repeatedly
waiting idle.

## RUNNING Update

- Worker spawned: `Darwin 019ec299-11cf-74b1-bcce-22422635d20c`
- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09AQ .`
  succeeded.
- Dispatch state and ledger were updated from `RESERVED` to `RUNNING` with the
  actual worker id.

## Coordinator Recovery Closeout

- Darwin timed out after one meaningful wait; the coordinator did not continue
  idle polling.
- Coordinator inspected changed paths and focused verification, then recovered
  the exact reserved scope locally.
- Test fixes included aligning the BI datasource compatibility test stub with
  the current compact API records and binding the datasource compatibility case
  to the production `BiCatalogController`.
- `multi_agent_v1.close_agent Darwin 019ec299-11cf-74b1-bcce-22422635d20c`
  returned `previous_status: running`; later notification reported shutdown.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  passed: `BiCatalogApplicationServiceTest` 38/38,
  `BiApiCompatibilityTest` 18/18, and
  `BiCatalogControllerCompatibilityTest` 28/28, total 84/84.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  completed; `cutoverReady` remains `false` globally, while
  `route:/canvas/bi` advanced from 119/169 to 132/169.
- Strict old-coupling scan over the DDD-C09AQ production target paths returned
  no matches for legacy `canvas-engine` BI datasource controller/service/config
  coupling.

## Closeout

- Dispatch status: `DONE_WITH_CONCERNS`.
- Active dispatch registry: cleared.
- Accepted concerns: no normal worker-return packet from Darwin, compact
  deterministic in-memory datasource operations seed, and broader BI/global
  route parity remains out of scope for this batch.
