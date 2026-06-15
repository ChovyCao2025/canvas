# DDD-C09AP Reservation Note

Date: 2026-06-14T03:42:00+08:00
Coordinator: main
Dispatch: dispatch-DDD-C09AP-bi-query-operations-routes-20260614-034200
Task: DDD-C09AP BI query operations route batch

## Scope

Reserved exact files:

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCompileResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryResultView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryExplainResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCancelResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGateCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGateResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryContractGateCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryHistoryItemView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryHistoryDetailView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernanceSummaryView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernancePolicyCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernancePolicyView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernanceAuditEntryView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCachePolicyCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCachePolicyView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheInvalidationCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheInvalidationResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheStatsView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthSnapshotView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthSloView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketVerifyCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketPayloadView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedQueryCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketCleanupResult.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQueryOperationsCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

Target routes:

- `POST /canvas/bi/query/compile`
- `POST /canvas/bi/query/execute`
- `POST /canvas/bi/query/explain`
- `POST /canvas/bi/query/cancel/{sqlHash}`
- `POST /canvas/bi/query/execute-gated`
- `POST /canvas/bi/query/execute-contract-gated`
- `GET /canvas/bi/query/history`
- `GET /canvas/bi/query/history/{historyId}`
- `GET /canvas/bi/query/governance-summary`
- `GET /canvas/bi/query/governance-policy`
- `POST /canvas/bi/query/governance-policy`
- `GET /canvas/bi/query/governance-audit`
- `GET /canvas/bi/query/cache-policy`
- `POST /canvas/bi/query/cache-policy`
- `POST /canvas/bi/query/cache/invalidate`
- `GET /canvas/bi/query/cache-stats`
- `GET /canvas/bi/datasources/health`
- `GET /canvas/bi/datasources/health/history`
- `GET /canvas/bi/datasources/health/slo`
- `POST /canvas/bi/embed-tickets`
- `POST /canvas/bi/embed-tickets/verify`
- `POST /canvas/bi/embed/query/execute`
- `POST /canvas/bi/embed-tickets/cleanup`

## Pre-Dispatch Evidence

- Active dispatch registry was empty after DDD-C09AO closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 15 controllers / 136 endpoints and
  `route:/canvas/bi` 96 current endpoints out of 169 old endpoints.
- G0B backup manifest exists.
- Base branch: `main`
- Base SHA: `2a1cdec07ec27a5298958822014aa28d9312869c`

## Scheduling Rule

The coordinator will spawn a real code-writing worker before moving this
dispatch to `RUNNING`. After one meaningful worker wait timeout, the coordinator
will inspect changed paths, evidence, and focused tests instead of repeatedly
waiting idle.

## RUNNING Update

- Worker spawned: `Poincare 019ec27f-1092-70c0-bdb5-5a892a29f5be`
- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09AP .`
  succeeded.
- Dispatch state and ledger were updated from `RESERVED` to `RUNNING` with the
  actual worker id.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  RUNNING update.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed after RUNNING update.
- Scoped `git diff --check` for DDD-C09AP coordination files passed.
- Legacy route scan confirmed 23 target mappings in
  `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`.
- Legacy domain read-scope inventory confirmed query/embed value objects under:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query`
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed`

## Coordinator Progress After First Wait Timeout

- The first worker wait timed out, so the coordinator inspected changed paths
  and ran focused Maven instead of waiting again.
- Worker output was present in the exact reserved scope: query/embed API records
  and `BiCatalogApplicationServiceTest` assertions had been added.
- Focused Maven verification was RED at `canvas-context-bi` test compile:
  `BiCatalogApplicationService` did not yet expose the query operations methods
  asserted by the new application test.
- Missing service methods included `compileQuery`, `executeQuery`,
  `executeGatedQuery`, `explainQuery`, `listQueryHistory`,
  `queryHistoryDetail`, `cancelQuery`, `queryGovernanceSummary`,
  `queryGovernancePolicy`, `updateQueryCachePolicy`, `invalidateQueryCache`,
  `queryCacheStats`, `datasourceHealth`, `datasourceHealthSlo`,
  `createEmbedTicket`, `verifyEmbedTicket`, `executeEmbedQuery`, and
  `cleanupEmbedTickets`.
- The coordinator sent this RED summary to Poincare
  `019ec27f-1092-70c0-bdb5-5a892a29f5be` and continued non-overlapping
  evidence/verification preparation.

## Coordinator Recovery Closeout

- Poincare did not return a final worker packet before coordinator recovery
  completed. The coordinator closed the worker with `previous_status: running`;
  the later subagent notification reported `shutdown`.
- The coordinator continued inside the exact DDD-C09AP reserved scope instead
  of repeatedly waiting on the worker.
- Recovered/verified the query operations vertical:
  - query/embed/datasource API records under `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api`
  - `BiQueryOperationsCatalog`
  - `BiCatalogFacade`
  - `BiCatalogApplicationService`
  - `BiCatalogController`
  - focused application/controller/API compatibility tests
- Removed a duplicate controller route block introduced during recovery after
  compile feedback showed the earlier route block was already present.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  passed: `BiCatalogApplicationServiceTest` 36/36 and web focused tests 44/44,
  for 80 focused tests total.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  passed as a diagnostic and reported `canvas-web` 15 controllers / 159
  endpoints, with `route:/canvas/bi` advancing from 96 to 119 current endpoints
  out of 169 old endpoints.
- Strict old-coupling scan over the DDD-C09AP production target paths was clean:
  no `canvas-engine`, old `org.chovy.canvas.domain.bi`, `BiQueryController`,
  `BiQueryExecutionService`, `BiEmbedTicketService`,
  `BiQueryGovernancePolicyService`, `BiQueryCachePolicyService`, or
  `BiDatasourceHealthProvider` references were found.
- Scoped `git diff --check` passed for the DDD-C09AP code/test/evidence paths.

## Closeout

Status: `DONE_WITH_CONCERNS`

Accepted concerns:

- No normal Poincare worker-return packet exists because the worker was still
  running when closed.
- Query, governance, cache, datasource health, and embed behavior are compact
  deterministic in-memory compatibility seeds.
- Broader BI route parity and global cutover readiness remain out of scope for
  this dispatch; preflight still reports `cutoverReady: false`.
