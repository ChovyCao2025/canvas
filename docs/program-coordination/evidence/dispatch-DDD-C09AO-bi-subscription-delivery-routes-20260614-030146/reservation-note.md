# DDD-C09AO Reservation Note

Date: 2026-06-14T03:01:46+08:00
Coordinator: main
Dispatch: dispatch-DDD-C09AO-bi-subscription-delivery-routes-20260614-030146
Task: DDD-C09AO BI subscription and delivery route batch

## Scope

Reserved exact files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSubscriptionCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSubscriptionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAlertRuleCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAlertRuleView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryRunResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryLogView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAuditSummary.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryRetryResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentDownload.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentCleanupResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliverySchedulerResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSubscriptionDeliveryCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

Target routes:

- `GET /canvas/bi/subscriptions`
- `POST /canvas/bi/subscriptions`
- `DELETE /canvas/bi/subscriptions/{id}`
- `POST /canvas/bi/subscriptions/{id}/run`
- `GET /canvas/bi/alerts`
- `POST /canvas/bi/alerts`
- `DELETE /canvas/bi/alerts/{id}`
- `POST /canvas/bi/alerts/{id}/run`
- `GET /canvas/bi/delivery-logs`
- `GET /canvas/bi/delivery-audit`
- `POST /canvas/bi/delivery-logs/retry`
- `GET /canvas/bi/delivery-attachments`
- `GET /canvas/bi/delivery-attachments/{id}/download`
- `POST /canvas/bi/delivery-attachments/cleanup`
- `POST /canvas/bi/delivery-scheduler/run`

## Pre-Dispatch Evidence

- Active dispatch registry was empty after DDD-C09AN closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 15 controllers / 121 endpoints and
  `route:/canvas/bi` 81 current endpoints out of 169 old endpoints.
- Local inspection confirmed current `BiCatalogController` has no
  subscription, alert, delivery log, delivery attachment, or scheduler routes.
- `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` exists.
- Base branch: `main`
- Base SHA: `2a1cdec07ec27a5298958822014aa28d9312869c`

## Scheduling Rule

The coordinator will spawn a real code-writing worker before moving this
dispatch to `RUNNING`. After one meaningful worker wait timeout, the coordinator
will inspect changed paths, evidence, and focused tests instead of repeatedly
waiting idle.

## RUNNING Update

- Worker spawned: `Boole 019ec264-48c0-7cb2-a55d-fb6ebbc367dd`
- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09AO .` succeeded.
- Dispatch state and ledger were updated from `RESERVED` to `RUNNING` with the
  actual worker id.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  RUNNING update.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed after RUNNING update.
- Scoped `git diff --check` for DDD-C09AO coordination files passed.
- Legacy route scan confirmed 15 target mappings for the compact
  `BiCatalogController` migration target.
- Preflight baseline while worker runs:
  - `canvas-web`: 15 controllers / 121 endpoints
  - `route:/canvas/bi`: 81 current endpoints out of 169 old endpoints
  - `cutoverReady=false`

## Coordinator Progress After First Wait Timeout

- The first worker wait timed out, so the coordinator inspected changed paths
  and ran verification instead of waiting again.
- Initial focused Maven verification was RED at `canvas-context-bi` compile:
  `BiCatalogApplicationService` had subscription/delivery `@Override`
  methods before matching `BiCatalogFacade` signatures were visible.
- The coordinator sent the RED summary to worker `Boole
  019ec264-48c0-7cb2-a55d-fb6ebbc367dd` and continued non-overlapping local
  checks.
- A later local inspection showed `BiCatalogFacade` contained the 15 expected
  subscription, alert, delivery log/audit/retry, attachment, download, cleanup,
  and scheduler signatures.
- Focused Maven then reached `canvas-web` test compile and failed only because
  `BiCatalogControllerCompatibilityTest.RecordingBiCatalogFacade` lacked the
  new subscription/delivery test-stub fields and `runDeliveryScheduler`.
- The coordinator fixed that reserved compatibility test file locally to avoid
  idle waiting.

## Coordinator Verification While Worker Runs

- Focused Maven passed:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  - `BiCatalogApplicationServiceTest`: 34 tests, 0 failures/errors.
  - `BiApiCompatibilityTest`: 16 tests, 0 failures/errors.
  - `BiCatalogControllerCompatibilityTest`: 27 tests, 0 failures/errors.
- Cutover preflight after route batch:
  - `canvas-web`: 15 controllers / 136 endpoints.
  - `route:/canvas/bi`: 96 current endpoints out of 169 old endpoints.
  - `cutoverReady=false`.
- Forbidden old-coupling scan had no matches across:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
  - `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api`
  - `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSubscriptionDeliveryCatalog.java`
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- Scoped `git diff --check` for DDD-C09AO implementation/evidence files passed.
