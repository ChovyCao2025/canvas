# QuickBI-Like Analytics Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete QuickBI-like analytics platform package embedded in Marketing Canvas, starting with BI metadata schema, marketing datasets, and a safe query compiler.

**Architecture:** The BI platform is a separate subsystem under `domain/bi` and `web/bi`. Canvas consumes it through internal routes and embed tokens; the query engine consumes only dataset semantics and never accepts frontend SQL.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway, Doris/MySQL JDBC, React, Ant Design, Recharts in phases 1-2, ECharts plugin support in phase 3.

---

## File Structure

- Create: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md` — complete product and technical design.
- Create: `docs/superpowers/plans/2026-06-05-quickbi-platform.md` — this implementation plan.
- Create: `backend/canvas-engine/src/main/resources/db/migration/V191__bi_platform_foundation.sql` — BI metadata tables and marketing seed data.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryRequest.java` — structured query request DTO.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFilter.java` — filter model and supported operators.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiSort.java` — sort model.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpec.java` — immutable dataset semantic model.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFieldSpec.java` — dataset field metadata.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiMetricSpec.java` — metric metadata.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiCompiledQuery.java` — compiled SQL and bound parameters.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCompiler.java` — safe SQL compiler.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java` — built-in Canvas marketing dataset definitions.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCompilerTest.java` — TDD coverage for query compilation.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistryTest.java` — TDD coverage for built-in datasets.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/*` — built-in dashboard preset model.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/MarketingBiDashboardPresetRegistryTest.java` — preset dashboard coverage.
- Create: `frontend/src/pages/bi/embed.tsx` — anonymous BI embed render page.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java` — allow anonymous ticket verification only.
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java` — security route coverage for embed verification.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java` — safe query execution orchestration.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResult.java` — BI query execution result DTO.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryExecutor.java` — JDBC/Doris query executor.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryHistoryRecorder.java` — query history recorder.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryHistoryReader.java` — query history reader.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/InMemoryBiQueryResultCache.java` — local query result cache.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/InMemoryBiQueryResultCacheTest.java` — result cache TTL coverage.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java` — execution and history coverage.

## Task 1: Write BI Design Spec

**Files:**
- Create: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`

- [x] **Step 1: Capture official Quick BI references**

Use Alibaba Cloud Quick BI official references for capabilities: data sources, datasets, dashboards, query controls, drill/filter/link interactions, workspace roles, row/column permissions, subscriptions, embedding, and Smart Q agents.

- [x] **Step 2: Define Canvas-specific BI positioning**

Define the platform as "generic BI core + marketing preset package".

- [x] **Step 3: Define phased implementation**

Phase order: BI core, chart/dashboard, self-service/export/subscription, portal/embed, AI agents.

## Task 2: Add BI Metadata Schema

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V191__bi_platform_foundation.sql`

- [x] **Step 1: Add metadata tables**

Create workspace, dataset, field, metric, chart, dashboard, widget, portal, permission, query history, export, subscription, alert, embed token, audit log tables.

- [x] **Step 2: Add marketing seed workspace and dataset**

Seed `marketing_canvas` workspace, `canvas_daily_stats` dataset, and core marketing metrics.

- [x] **Step 3: Verify migration numbering**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=FlywayMigrationPolicyTest test
```

Expected: migration policy test passes or fails only because unrelated dirty worktree changes already break it.

Observed on 2026-06-05: command compiled and ran, then failed on an existing non-BI duplicate version: `V189 -> [V189__project_governance.sql, V189__cdp_olap_audience_materialization.sql]`. The BI migration uses `V191__bi_platform_foundation.sql` and no `V190__bi_platform_foundation.sql` remains.

## Task 3: Implement BI Query Model With TDD

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCompilerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFilter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiSort.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpec.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFieldSpec.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiMetricSpec.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiCompiledQuery.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCompiler.java`

- [x] **Step 1: Write failing compiler tests**

Test behaviors:

- dimensions and metrics compile into a grouped SQL query.
- tenant filter is always injected.
- `between`, `eq`, `in`, `gte`, `lte`, and `contains` filters use bound parameters.
- unknown field fails before SQL generation.
- unknown metric fails before SQL generation.
- unsafe limit fails before SQL generation.

- [x] **Step 2: Run compiler tests and verify RED**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=BiQueryCompilerTest test
```

Expected: fails because BI query classes do not exist.

- [x] **Step 3: Implement minimal query compiler**

Compile only dataset-scoped, whitelist-based SQL with parameter binding and deterministic aliases.

- [x] **Step 4: Run compiler tests and verify GREEN**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=BiQueryCompilerTest test
```

Expected: tests pass.

## Task 4: Add Marketing Dataset Registry

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistryTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java`

- [x] **Step 1: Write failing registry tests**

Test that `canvas_daily_stats` contains dimensions `stat_date`, `canvas_id`, `canvas_name`, `trigger_type` and metrics `total_executions`, `success_count`, `fail_count`, `unique_users`, `avg_duration_ms`, `success_rate`.

- [x] **Step 2: Run registry tests and verify RED**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=MarketingBiDatasetRegistryTest test
```

Expected: fails because registry does not exist.

- [x] **Step 3: Implement registry**

Create immutable built-in dataset definitions for current Canvas OLAP tables.

- [x] **Step 4: Run registry tests and verify GREEN**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=MarketingBiDatasetRegistryTest test
```

Expected: tests pass.

## Task 5: Add Query API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java`

- [x] **Step 1: Write failing controller tests**

Test that `POST /canvas/bi/query/compile` compiles a safe request for the current tenant and rejects invalid fields.

- [x] **Step 2: Run controller tests and verify RED**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=BiQueryControllerTest test
```

Expected: fails because controller does not exist.

Observed on 2026-06-05: failed because `BiQueryController` did not exist.

- [x] **Step 3: Implement compile endpoint**

Expose compile-only endpoint first. Do not execute arbitrary SQL in this task. Also expose `GET /canvas/bi/datasets` and `GET /canvas/bi/datasets/{datasetKey}` so the frontend can load semantic metadata.

- [x] **Step 4: Run controller tests and verify GREEN**

Run:

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=BiQueryControllerTest test
```

Expected: tests pass.

Observed on 2026-06-05: `BiQueryControllerTest`, `MarketingBiDatasetRegistryTest`, and `BiQueryCompilerTest` passed together with 10 tests.

## Task 6: Frontend BI Shell

**Files:**
- Create: `frontend/src/services/biApi.ts`
- Create: `frontend/src/pages/bi/index.tsx`
- Create: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [x] **Step 1: Write frontend tests for presentation helpers**

Create tests for BI workspace navigation model and default marketing datasets.

- [x] **Step 2: Implement BI workbench shell**

Add routes for workbench, datasets, dashboard editor, self-service, subscriptions, and AI assistant. Initial pages can render stable navigation and empty states backed by service contracts.

- [x] **Step 3: Run frontend tests**

Run:

```bash
cd frontend && npm test -- biWorkbench
```

Expected: tests pass.

Observed on 2026-06-05: `npm test -- biWorkbench` passed with 5 tests after the page started loading backend dataset metadata with preset fallback.

## Task 7: Verification

**Files:**
- No new files unless failures require focused fixes.

- [x] **Step 1: Run targeted backend tests**

```bash
mvn -f backend/canvas-engine/pom.xml -Dtest=BiQueryCompilerTest,MarketingBiDatasetRegistryTest test
```

Observed on 2026-06-05: `BiQueryCompilerTest`, `MarketingBiDatasetRegistryTest`, and `BiQueryControllerTest` passed together with 10 tests. Migration-policy verification is blocked by the existing duplicate `V189` migrations, which are outside the BI files.

- [x] **Step 2: Run targeted frontend tests after UI shell**

```bash
cd frontend && npm test -- biWorkbench
```

Observed on 2026-06-05: passed with 5 tests. `npm run build` also completed successfully.

- [x] **Step 3: Inspect git diff**

```bash
git diff -- docs/superpowers/specs/2026-06-05-quickbi-platform-design.md docs/superpowers/plans/2026-06-05-quickbi-platform.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/main/resources/db/migration/V191__bi_platform_foundation.sql
```

Confirm the diff only contains BI-related additions.

Observed on 2026-06-05: new BI files are isolated. `frontend/src/App.tsx` and `frontend/src/components/layout/AppLayout.tsx` already contained unrelated dirty worktree changes; the BI-specific edits in those files are the `/bi` route, lazy page import, selected key handling, and `BI 工作台` menu entry.

## Task 8: Add QuickBI-Like Dashboard Presets And Canvas Entry

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardPreset.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardWidget.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardFilter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardInteraction.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/MarketingBiDashboardPresetRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/MarketingBiDashboardPresetRegistryTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/canvas-stats/index.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [x] **Step 1: Write failing preset tests**

Test that `canvas-effect` exposes KPI, line, bar, table widgets, query controls, linkage, drill, hyperlink, subscription channels, and embed scopes.

Observed on 2026-06-05: failed because dashboard preset model did not exist.

- [x] **Step 2: Implement dashboard preset registry and API**

Expose `GET /canvas/bi/dashboards/presets` and `GET /canvas/bi/dashboards/presets/{dashboardKey}`.

- [x] **Step 3: Implement QuickBI-like frontend designer**

Replace the BI shell with a designer layout: top toolbar, left resource/component panel, center dashboard grid, right data/style/interaction panel, query controls, subscriptions, and embedding indicators.

- [x] **Step 4: Connect Canvas pages**

Add BI entry from canvas stats header and canvas list row menu using `/bi?dashboard=canvas-effect&canvasId={id}`.

- [x] **Step 5: Verify**

Observed on 2026-06-05: backend BI tests passed with 12 tests; `npm test -- biWorkbench` passed with 8 tests; `npm run build` completed successfully after the designer and Canvas entry changes.

## Task 9: Add BI Embed Ticket Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicket.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketPayload.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Write failing embed ticket tests**

Test tenant/user/resource/scope/filter binding, TTL capping, HMAC verification, tamper rejection, weak-secret rejection, and unsafe resource key rejection.

Observed on 2026-06-05: failed because embed ticket classes did not exist.

- [x] **Step 2: Implement HMAC embed ticket service**

Ticket payload includes tenant, username, resource type, resource key, scope, filters, nonce, issuedAt, and expiresAt. The service signs payload with HMAC-SHA256 using `canvas.bi.embed-secret` or `canvas.jwt.secret`.

- [x] **Step 3: Add ticket API**

Expose `POST /canvas/bi/embed-tickets` and bind ticket creation to current tenant context.

- [x] **Step 4: Add frontend embed flow**

Add `biApi.createEmbedTicket`, build request from dashboard/canvas context, and show the generated short-lived embed URL in the BI designer interaction panel.

- [x] **Step 5: Verify**

Observed on 2026-06-05: backend BI + embed tests passed with 16 tests; `npm test -- biWorkbench` passed with 9 tests; `npm run build` completed successfully.

## Task 10: Add Embed Render Page And Ticket Verification Endpoint

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketVerifyRequest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`
- Create: `frontend/src/pages/bi/embed.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/services/biApi.ts`

- [x] **Step 1: Add anonymous ticket verification API**

Expose `POST /canvas/bi/embed-tickets/verify` and return the verified ticket payload. This endpoint must not create tickets and must not execute BI queries.

- [x] **Step 2: Keep public surface minimal**

Allow anonymous access only for `POST /canvas/bi/embed-tickets/verify`. Keep `POST /canvas/bi/embed-tickets` and `POST /canvas/bi/query/compile` authenticated.

- [x] **Step 3: Add frontend anonymous embed route**

Register `/bi/embed/:resourceType/:resourceKey` outside the authenticated app shell. The page reads `ticket`, verifies it, checks that the payload resource matches the route, and renders a stripped report view.

- [x] **Step 4: Verify route behavior**

Add security tests proving ticket verification is anonymous while ticket creation remains authenticated.

## Task 11: Add BI Query Execution Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryColumn.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryContext.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutor.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryEntry.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryRecorder.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryExecutor.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryHistoryRecorder.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add execution service**

Execute only parameterized SQL produced by `BiQueryCompiler`. Build result columns from dataset semantics and return rows, row count, duration, and SQL hash.

- [x] **Step 2: Add JDBC/Doris executor**

Route `canvas_dws.*` and `canvas_ods.*` datasets to optional Doris JDBC. Route future primary-database datasets to the main `JdbcTemplate`. If a required datasource is disabled, fail explicitly.

- [x] **Step 3: Add query history recorder**

Write `bi_query_history` with request JSON, SQL hash, row count, duration, status, and error summary. Recorder failures are logged but do not break report reads.

- [x] **Step 4: Expose execute endpoint**

Expose `POST /canvas/bi/query/execute` behind normal authentication. Keep anonymous execution out of scope until embed query policy is implemented.

- [x] **Step 5: Connect dashboard widgets**

Build widget-level structured query requests from dashboard presets, call `executeQuery`, and render live rows when available. Keep existing preview render as a fallback when Doris/local datasource is unavailable.

- [x] **Step 6: Verify**

Observed on 2026-06-05: backend BI/security targeted tests passed with 23 tests; `npm test -- biWorkbench` passed with 10 tests; `npm run build` completed successfully.

## Task 12: Add Query Governance Visibility

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryItem.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryReader.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealth.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealthProvider.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryHistoryReader.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryExecutor.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add history reader**

Read recent `bi_query_history` entries by tenant, parse dataset keys from request JSON, and cap result size.

- [x] **Step 2: Add datasource health provider**

Expose health for primary MySQL and optional Doris JDBC using lightweight `SELECT 1` probes.

- [x] **Step 3: Expose governance APIs**

Expose `GET /canvas/bi/query/history` and `GET /canvas/bi/datasources/health` behind normal authentication.

- [x] **Step 4: Add workbench visibility panel**

Show datasource health and recent query history in the BI workbench. This gives analysts immediate feedback for datasource outages, failed queries, row counts, latency, and SQL hash correlation.

- [x] **Step 5: Verify**

Observed on 2026-06-05: backend BI/security targeted tests passed with 25 tests; `npm test -- biWorkbench` passed with 10 tests; `npm run build` completed successfully.

## Task 13: Add Query Result Cache Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResultCache.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/bi/InMemoryBiQueryResultCache.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/InMemoryBiQueryResultCacheTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResult.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add cache contract**

Add a query result cache interface keyed by compiled SQL hash. Keep no-op behavior available for focused tests.

- [x] **Step 2: Add configurable local cache**

Add an in-memory cache with `canvas.bi.query.cache.enabled`, `ttl-seconds`, and `max-size` controls. Default is enabled, 300 seconds, 500 entries.

- [x] **Step 3: Integrate execution service**

Check cache before datasource execution. On hit, return `cached=true`, skip datasource execution, and record `CACHE_HIT` in query history.

- [x] **Step 4: Expose cache status in UI**

Add `cached` to the frontend query result contract. Dashboard widgets show real-time/cache state, row count, and query duration.

- [x] **Step 5: Verify**

Observed on 2026-06-05: backend BI/security/cache targeted tests passed with 28 tests; `npm test -- biWorkbench` passed with 10 tests; `npm run build` completed successfully.

## Task 14: Add Persisted Dashboard Resource Lifecycle

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiWorkspaceDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardWidgetDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiWorkspaceMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardWidgetMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResource.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`

- [x] **Step 1: Add dashboard persistence model**

Map `bi_workspace`, `bi_dashboard`, and `bi_dashboard_widget` to MyBatis-Plus DOs and mappers.

- [x] **Step 2: Add dashboard resource service**

Implement list, get, save draft, and publish. Persistent resources take priority; unsaved dashboards fall back to preset definitions.

- [x] **Step 3: Expose dashboard lifecycle API**

Expose:

```text
GET /canvas/bi/dashboards/resources
GET /canvas/bi/dashboards/resources/{dashboardKey}
POST /canvas/bi/dashboards/resources/{dashboardKey}/draft
POST /canvas/bi/dashboards/resources/{dashboardKey}/publish
```

- [x] **Step 4: Connect frontend save and publish**

Load persisted dashboard resources before presets. Add workbench save/publish actions and show source, status, and version in the toolbar.

- [x] **Step 5: Verify**

Observed on 2026-06-05: backend dashboard/query/embed/security targeted tests passed with 34 tests; `npm test -- biWorkbench` and `npm run build` completed successfully.

## Task 15: Add Persisted Chart Resource Lifecycle

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiChartDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiChartMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResource.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiChartControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add chart persistence model**

Map `bi_chart` and the existing `bi_dataset` metadata table. Keep chart query definitions as structured BI query JSON, not SQL.

- [x] **Step 2: Add chart resource service**

Implement list, get, save draft, publish, and archive. Saving a draft validates the chart query with `BiQueryCompiler` before writing metadata.

- [x] **Step 3: Expose chart lifecycle API**

Expose:

```text
GET /canvas/bi/charts/resources
GET /canvas/bi/charts/resources/{chartKey}
POST /canvas/bi/charts/resources/{chartKey}/draft
POST /canvas/bi/charts/resources/{chartKey}/publish
DELETE /canvas/bi/charts/resources/{chartKey}
```

- [x] **Step 4: Connect frontend chart assets**

Add chart resource API contracts and show persisted chart assets in the workbench left-side chart tab alongside the QuickBI-like chart palette.

## Task 16: Add Persisted Dataset Resource Lifecycle

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetFieldDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiMetricDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetFieldMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiMetricMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpecResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/*`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDatasetControllerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add dataset metadata model**

Map `bi_dataset_field` and `bi_metric`, and extend `BiDatasetMapper` with upsert, publish, and archive operations.

- [x] **Step 2: Add dataset resource service**

Implement list, get, save draft, publish, archive, validation, and conversion from persisted metadata to `BiDatasetSpec`.

- [x] **Step 3: Add dataset spec resolver**

Introduce `BiDatasetSpecResolver` so query compile, query execution, and chart save validation can resolve tenant-scoped persisted datasets before falling back to built-in marketing datasets.

- [x] **Step 4: Expose dataset lifecycle API**

Expose:

```text
GET /canvas/bi/datasets/resources
GET /canvas/bi/datasets/resources/{datasetKey}
POST /canvas/bi/datasets/resources/{datasetKey}/draft
POST /canvas/bi/datasets/resources/{datasetKey}/publish
DELETE /canvas/bi/datasets/resources/{datasetKey}
```

- [x] **Step 5: Connect frontend dataset assets**

Add dataset resource API contracts and show persisted dataset assets in the BI workbench alongside built-in marketing semantic datasets.

## Task 17: Add Persisted Portal Resource Lifecycle

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalMenuDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalMenuMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/*`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`

- [x] **Step 1: Add portal persistence model**

Map `bi_portal` and `bi_portal_menu`. Keep menu resource references normalized as `resource_id` for internal resources and `external_url` for links.

- [x] **Step 2: Add portal resource service**

Implement list, get, save draft, publish, archive, menu validation, dashboard/chart key resolution, and safe external URL validation.

- [x] **Step 3: Expose portal lifecycle API**

Expose:

```text
GET /canvas/bi/portals/resources
GET /canvas/bi/portals/resources/{portalKey}
POST /canvas/bi/portals/resources/{portalKey}/draft
POST /canvas/bi/portals/resources/{portalKey}/publish
DELETE /canvas/bi/portals/resources/{portalKey}
```

- [x] **Step 4: Connect frontend portal assets**

Add portal resource API contracts and show persisted portal assets in the BI workbench, including status, theme, menu count, and menu resource types.

## Task 18: Add BI Permission Execution Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourcePermissionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiRowPermissionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiColumnPermissionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiAuditLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourcePermissionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiRowPermissionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiColumnPermissionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiAuditLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`

- [x] **Step 1: Map BI permission and audit tables**

Map resource permission, row permission, column permission, and audit log tables with MyBatis-Plus DOs and mappers.

- [x] **Step 2: Add permission preparation service**

Implement a shared service that enforces dataset resource permission, appends row-permission filters, evaluates column `DENY` and `MASK`, applies result masking, produces a cache permission signature, and exposes portal menu visibility filtering.

- [x] **Step 3: Connect query compile and execute paths**

Run permission preparation before SQL compilation in `POST /canvas/bi/query/compile` and `POST /canvas/bi/query/execute`. Keep existing CDP field governance as an additional check after BI row/column policies.

- [x] **Step 4: Add focused tests**

Cover row filter injection, resource denial, column denial with audit, column masking, menu visibility filtering, and compatibility with existing query controller/execution behavior.

- [x] **Step 5: Verify**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The standard Maven targeted test command is currently blocked during `testCompile` by unrelated non-BI test sources in the dirty worktree that reference missing classes. To isolate this BI change, the affected BI test classes were compiled with Java 21 and run directly through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiPermissionServiceTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 20 tests passed.

## Task 19: Add BI Permission Management API And Workbench UI

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiResourcePermissionCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiResourcePermissionView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiRowPermissionCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiRowPermissionView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiColumnPermissionCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiColumnPermissionView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add permission management service**

Implement resource permission, row permission, and column permission list/upsert/delete operations. Support resource key resolution for datasets, dashboards, charts, and portals so callers do not need raw database IDs.

- [x] **Step 2: Expose permission management API**

Expose:

```text
GET /canvas/bi/permissions/resources
POST /canvas/bi/permissions/resources
DELETE /canvas/bi/permissions/resources/{id}
GET /canvas/bi/permissions/rows
POST /canvas/bi/permissions/rows
DELETE /canvas/bi/permissions/rows/{id}
GET /canvas/bi/permissions/columns
POST /canvas/bi/permissions/columns
DELETE /canvas/bi/permissions/columns/{id}
```

- [x] **Step 3: Add workbench contracts and UI**

Add frontend API types and render a QuickBI-like permission governance band with resource authorization, row permission, and column permission tables. Add template actions for dataset `USE` grants, Canvas row filters, and `canvas_name` masking.

- [x] **Step 4: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The same dirty-worktree Maven `testCompile` limitation from Task 18 remains. Isolated BI tests were compiled with Java 21 and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiPermissionAdminServiceTest,BiPermissionControllerTest,BiPermissionServiceTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 29 tests passed.

- [x] **Step 5: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin` because the default Node 18.20.8 cannot run current Vite/Vitest dependencies requiring `node:util.styleText`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 20: Add Self-Service Preview And Export Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiExportJobMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServicePreviewRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportDownload.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Map export job metadata**

Map the existing `bi_export_job` table to MyBatis-Plus DO/Mapper and keep exports as task-shaped records.

- [x] **Step 2: Add self-service preview and export service**

Implement preview through `BiQueryExecutionService` so previews inherit tenant isolation, row permission, column permission, masking, field governance, query history, and cache behavior. Implement export creation with explicit dataset `EXPORT` resource permission enforcement, then execute the same structured query and write CSV/JSON/XLSX files under `canvas.bi.export.dir`.

- [x] **Step 3: Expose self-service API**

Expose:

```text
POST /canvas/bi/self-service/preview
POST /canvas/bi/self-service/exports
GET /canvas/bi/self-service/exports
GET /canvas/bi/self-service/exports/{id}/download
```

- [x] **Step 4: Add workbench self-service UI**

Add frontend API contracts and a QuickBI-like self-service band with preview, export CSV, export task list, and download action. Add a permission shortcut for dataset `EXPORT` grants.

- [x] **Step 5: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The same dirty-worktree Maven `testCompile` limitation remains. Isolated BI tests were compiled with Java 21 and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiSelfServiceExportServiceTest,BiSelfServiceControllerTest,BiPermissionAdminServiceTest,BiPermissionControllerTest,BiPermissionServiceTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 35 tests passed.

- [x] **Step 6: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 21: Add Portal Runtime API And Menu Visibility

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalRuntimeService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalRuntimeController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalRuntimeServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalRuntimeControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add runtime portal service**

Add a read-only runtime layer over portal resources. Runtime list/get only return `PUBLISHED` portals and rebuild the portal DTO with menus filtered through `BiPermissionService.visibleMenus(...)`.

- [x] **Step 2: Expose runtime portal API**

Expose:

```text
GET /canvas/bi/portals/runtime
GET /canvas/bi/portals/runtime/{portalKey}
```

The controller passes current tenant, username, and role into `BiQueryContext`, so `visibility_json.roles/users/denyRoles/denyUsers` rules are evaluated for the actual caller.

- [x] **Step 3: Add workbench runtime portal UI**

Add frontend API contracts and a “门户查看态” table beside management-state “数据门户资产”. The view shows published portals and the currently visible menu count, making menu permission effects visible in the workbench.

- [x] **Step 4: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
```

passed.

The dirty-worktree Maven `testCompile` limitation remains, so BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalRuntimeServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalRuntimeControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiPortalRuntimeServiceTest,BiPortalRuntimeControllerTest,BiPortalResourceServiceTest,BiPortalControllerTest,BiSelfServiceExportServiceTest,BiSelfServiceControllerTest,BiPermissionAdminServiceTest,BiPermissionControllerTest,BiPermissionServiceTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 47 tests passed.

- [x] **Step 5: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 22: Add Subscription And Alert Management Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiSubscriptionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiAlertRuleDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiSubscriptionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiAlertRuleMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiAlertRuleCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiAlertRuleView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Map subscription and alert metadata**

Map existing `bi_subscription` and `bi_alert_rule` tables to MyBatis-Plus DO/Mapper classes. Keep schedule, receivers, delivery, and alert condition as structured JSON maps so schedulers and delivery workers can consume the same task definitions.

- [x] **Step 2: Add subscription and alert admin service**

Implement list/upsert/delete for subscriptions and alert rules. Resolve dashboard/chart/portal/dataset resources by key or ID, validate dataset metrics for alerts, validate receiver channels and schedules, and enforce `BiPermissionService.ACTION_SUBSCRIBE` during creation/update.

- [x] **Step 3: Expose subscription and alert API**

Expose:

```text
GET /canvas/bi/subscriptions
POST /canvas/bi/subscriptions
DELETE /canvas/bi/subscriptions/{id}
GET /canvas/bi/alerts
POST /canvas/bi/alerts
DELETE /canvas/bi/alerts/{id}
```

- [x] **Step 4: Add workbench subscription and alert UI**

Add frontend contracts and a QuickBI-like distribution band with subscription task list, alert rule list, daily dashboard subscription shortcut, and metric threshold alert shortcut.

- [x] **Step 5: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The dirty-worktree Maven `testCompile` limitation remains. New and related BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiSubscriptionAdminServiceTest,BiSubscriptionControllerTest,BiPortalRuntimeServiceTest,BiPortalRuntimeControllerTest,BiSelfServiceExportServiceTest,BiSelfServiceControllerTest,BiPermissionServiceTest,BiPermissionControllerTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 41 tests passed.

- [x] **Step 6: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 23: Add Subscription And Alert Delivery Runtime Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V203__bi_delivery_log.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliveryLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryLogView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRunResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add delivery log schema**

Create `bi_delivery_log` for subscription and alert runtime traces. Store job type/id/key, channel, receiver payload, delivery payload, metric value, status, error, and triggered user.

- [x] **Step 2: Add delivery runtime service**

Implement manual subscription run and manual alert evaluation. Subscription run creates per-channel logs and sends notification-center messages for `IN_APP` channels. Alert run queries the BI semantic layer for the configured metric, evaluates threshold operators, writes skipped/triggered evaluation logs, and creates channel delivery logs when matched.

- [x] **Step 3: Expose runtime API**

Expose:

```text
POST /canvas/bi/subscriptions/{id}/run
POST /canvas/bi/alerts/{id}/run
GET /canvas/bi/delivery-logs
```

- [x] **Step 4: Add workbench runtime UI**

Add run/detect buttons for subscription and alert tasks. Add a delivery log table showing job type, job key, channel, status, metric value, and message/error.

- [x] **Step 5: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The dirty-worktree Maven `testCompile` limitation remains. New and related BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiDeliveryRuntimeServiceTest,BiSubscriptionAdminServiceTest,BiSubscriptionControllerTest,BiPortalRuntimeServiceTest,BiSelfServiceExportServiceTest,BiPermissionServiceTest,BiQueryExecutionServiceTest,BiQueryControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 37 tests passed.

- [x] **Step 6: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 24: Add Subscription And Alert Scheduler Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add due-check scheduler service**

Add `BiDeliverySchedulerService` with automatic scheduling disabled by default via `canvas.bi.delivery.scheduler.enabled:false`. It checks enabled subscription and alert rules for one configured tenant, prevents overlapping local runs, evaluates `HOURLY`、`DAILY`、`WEEKLY`、`MONTHLY`、`intervalMinutes`、`checkIntervalMinutes` and `cronExpression`, and uses `bi_delivery_log` latest job run to avoid duplicate deliveries inside the same schedule window.

- [x] **Step 2: Reuse delivery runtime**

The scheduler does not duplicate delivery logic. Due subscriptions call `BiDeliveryRuntimeService.runSubscription(...)`; due alerts call `BiDeliveryRuntimeService.runAlert(...)`. The returned `BiDeliverySchedulerResult` counts checked, triggered, skipped, and failed jobs.

- [x] **Step 3: Expose manual scheduler API and workbench control**

Expose:

```text
POST /canvas/bi/delivery-scheduler/run
```

The endpoint uses current tenant, username, and role, and runs one due-check cycle even when the background scheduler is disabled. The BI workbench now includes a “调度” button in the delivery history area and shows the latest scheduler result summary.

- [x] **Step 4: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
```

passed.

The dirty-worktree Maven `testCompile` limitation remains. New and directly related BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiDeliverySchedulerServiceTest,BiDeliveryRuntimeServiceTest,BiSubscriptionAdminServiceTest,BiSubscriptionControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: 17 tests passed.

- [x] **Step 5: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 25: Add Delivery Adapters And Retry Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRetryResult.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add delivery adapter service**

Add `BiDeliveryAdapterService` so delivery runtime no longer treats every external channel as a placeholder. The adapter supports real HTTP POST delivery for `WEBHOOK`、`LARK`、`FEISHU`、`DINGTALK` and `DING`, with Lark/Feishu and DingTalk robot text payload shapes. It accepts URL fields such as `webhookUrl`、`larkWebhookUrl`、`feishuWebhookUrl`、`dingtalkWebhookUrl`、`callbackUrl` and nested `webhook` config maps.

- [x] **Step 2: Integrate adapters into runtime logs**

`BiDeliveryRuntimeService` now delegates non-IN_APP channels to the adapter service when present. Adapter success writes `DELIVERED`, missing channel configuration writes `PENDING_ADAPTER`, and HTTP or network failure writes `FAILED` with the error summary.

- [x] **Step 3: Add retry foundation**

Expose:

```text
POST /canvas/bi/delivery-logs/retry?limit=20
```

The endpoint replays recent `PENDING_ADAPTER` and `FAILED` delivery logs through the same delivery path, records fresh retry logs, and returns checked/retried/delivered/pending/failed counters. The BI workbench now includes a “重试” button and retry summary tag in the delivery history area.

- [x] **Step 4: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

The dirty-worktree Maven `testCompile` limitation remains. BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest="$TESTS" \
  -DfailIfNoTests=false surefire:test
```

Observed result: 103 tests passed.

- [x] **Step 5: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 26: Add SMTP Email And Enterprise WeChat Delivery Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailDeliveryRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailDeliveryClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClient.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClientTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterServiceTest.java`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add dependency-free SMTP email client**

Add `BiEmailDeliveryClient` and `BiSmtpEmailDeliveryClient` so the BI package can send plain-text subscription/alert emails without pulling a new mail dependency from external artifact repositories. The SMTP adapter is disabled by default and becomes active only when `canvas.bi.delivery.email.enabled:true`, `canvas.bi.delivery.email.host`, and `canvas.bi.delivery.email.from` are configured. It supports basic SMTP, SSL, STARTTLS, and AUTH LOGIN.

- [x] **Step 2: Integrate Email into delivery adapter**

`BiDeliveryAdapterService` now sends `EMAIL` deliveries through `BiEmailDeliveryClient` when configured. It extracts recipients from `emails`、`email`、`to`、`recipients` and email-shaped `users`, uses receiver-level `from` or `canvas.bi.delivery.email.from`, and records `DELIVERED`、`PENDING_ADAPTER` or `FAILED` based on adapter outcome.

- [x] **Step 3: Add enterprise WeChat robot adapter**

Extend webhook delivery to support `WECOM`、`WE_COM`、`WECHAT_WORK`、`WECHATWORK`、`ENTERPRISE_WECHAT` and `QYWX` channels. Enterprise WeChat uses robot text message payloads and recognizes `wecomWebhookUrl`、`wechatWorkWebhookUrl`、`enterpriseWechatWebhookUrl` and `qyWechatWebhookUrl`.

- [x] **Step 4: Update workbench channel model**

The built-in `canvas-effect` preset now advertises `EMAIL`、`LARK`、`WECOM` and `WEBHOOK` as subscription channels, matching the common Quick BI subscription push model while still requiring concrete receiver/channel configuration before real external delivery.

- [x] **Step 5: Verify backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
```

passed.

BI tests were compiled in isolation and run through Surefire:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest="$TESTS" \
  -DfailIfNoTests=false surefire:test
```

Observed result: 108 tests passed.

- [x] **Step 6: Verify frontend**

Observed on 2026-06-05 with Node 25 from `/opt/homebrew/bin`:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` 10 tests passed and production build completed.

## Task 27: Full BI Platform Next Milestones

These items are intentionally not marked complete. They are the remaining work to turn the current foundation into a complete QuickBI-like platform.

- [ ] **Production query governance**

Add distributed cache, slow query detail drilldown, datasource health history, execution cancellation, and per-dataset quota controls.

- [ ] **Complete BI resource CRUD**

Dataset draft/publish/archive/version history/restore, dashboard draft/publish/clone/archive/import/export/resource-package file upload/download/version history/restore, chart draft/publish/archive/version history/restore, portal draft/publish/archive/version history/restore, resource movement foundations/workbench controls, resource transfer foundations/workbench controls, favorites, comments, edit lock foundations, publishing approval, save/publish permission checks, edit-lock binding for draft save and version restore mutations, dashboard widget drag movement, collision-aware grid positioning, explicit CSS grid placement, resize handles, and undo/redo helpers are done. Continue with managed screenshot execution, notification audit, and export/object-storage hardening.

- [ ] **Permission management and cross-channel enforcement**

Query-time resource permission, row permission, column denial, column masking, cache isolation, menu visibility filtering, denial audit, management API, workbench foundation UI, and subscription creation checks are done. Continue with full permission editor forms, workspace member role execution, permission requests, export policy, and the same permission path for real delivery, embed, and AI agents.

- [ ] **Self-service extraction and export**

Preview, CSV/JSON/XLSX task-shaped export, storage-backed download, export resource permission enforcement, export approval foundation, configurable retention, expired download rejection, download audit, cleanup endpoint, progress/retry metadata, failed export retry endpoint, and workbench task list are done. Continue with field drag/drop extraction builder, true async queue, PDF export, external object-storage provider, and audit detail pages.

- [ ] **Production subscription and alert delivery**

Subscription and alert management APIs, manual runtime, threshold evaluation, recent-run baseline anomaly checks, notification-center delivery, SMTP email, Webhook/Lark/Feishu/DingTalk/enterprise-WeChat HTTP adapters, pending/failed retry with backoff, due-check scheduler, manual scheduler endpoint, storage-backed delivery attachment foundation, MIME email attachments, retry-time email attachment replay, configurable HTTP browser screenshot renderer, attachment retention/download audit, delivery audit summary, scheduler lease/distributed locking, and workbench delivery history are done. Continue with a managed screenshot execution cluster, external object-storage provider, and multi-page PDF hardening.

- [ ] **Big screen and spreadsheet resources**

Add large-screen freeform layout, spreadsheet-style reports, and mobile layout variants.

- [ ] **AI SmartQ-like agents**

Implement semantic-layer-only ask-data, chart interpretation, report generation, dashboard draft generation, and anomaly insight agents.

## Task 28: Add Subscription Snapshot And Attachment Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V207__bi_delivery_attachment.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryAttachmentDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliveryAttachmentMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentDownload.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Capture Quick BI subscription and interaction references**

Official Quick BI docs confirm subscription content models around screenshot/snapshot, report links, and data attachments; delivery channels include email, DingTalk, enterprise WeChat, Feishu/Lark, and custom channels; non-email channels commonly rely on report links for attachment download. Dashboard interaction references require drilling, linkage, and hyperlink behaviors; portal references require menu-level authorization.

- [x] **Step 2: Add delivery attachment metadata**

Add `bi_delivery_attachment` to persist tenant, workspace, job, resource, attachment type, file name, content type, file path, file URL, size, status, and errors.

- [x] **Step 3: Generate server-side delivery artifacts**

`BiDeliveryAttachmentService` generates HTML snapshot files for `SNAPSHOT`/`SNAPSHOT_LINK` content and CSV/JSON/XLSX/PDF delivery summary attachments for configured `attachment` or `attachments`. Files are stored under `canvas.bi.delivery.attachment.dir`, defaulting to the JVM temp directory.

- [x] **Step 4: Integrate attachments into subscription runtime**

`BiDeliveryRuntimeService` creates attachments before channel delivery and injects attachment metadata into the delivery payload. `BiDeliveryAdapterService` includes attachment links in email/robot text messages.

- [x] **Step 5: Add attachment APIs and workbench UI**

Add list and download endpoints under `/canvas/bi/delivery-attachments`. The BI workbench lists attachment counts in delivery history and exposes download buttons from each delivery log payload.

- [x] **Step 6: Verify backend and frontend**

Run focused backend compile/tests and frontend `biWorkbench` tests/build. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 113 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: replace HTML summary snapshots with browser-rendered report screenshots, add multi-page PDF rendering, object storage retention, download audit, retry-time historical attachment replay, and distributed scheduler lease.

## Task 29: Send Real MIME Email Attachments For BI Subscriptions

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailAttachment.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailDeliveryRequest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClient.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterRequest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClientTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add email attachment payload model**

Add `BiEmailAttachment` and extend `BiEmailDeliveryRequest` with attachment support while keeping the existing text-only constructor compatible.

- [x] **Step 2: Generate multipart/mixed SMTP messages**

`BiSmtpEmailDeliveryClient` now writes `multipart/mixed` messages when attachments are present, keeps text-only messages for simple emails, and uses base64 transfer encoding for generated BI files.

- [x] **Step 3: Pass generated files to email adapter**

`BiDeliveryRuntimeService` materializes completed delivery attachments into `BiEmailAttachment` objects for `EMAIL` deliveries. `BiDeliveryAdapterService` passes those attachments to the mail client and keeps download links in the text body for channel consistency.

- [x] **Step 4: Cover adapter, runtime, and SMTP formatting**

Tests cover adapter propagation of generated attachments, runtime download-and-pass behavior, and SMTP `multipart/mixed` output.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 116 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: browser-rendered dashboard screenshots, multi-page PDF rendering, object storage retention, download audit, and distributed scheduler lease.

## Task 30: Replay Historical Email Attachments During Delivery Retry

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Resolve historical attachment IDs from delivery payload**

`BiDeliveryRuntimeService` now reads `payload.extra.attachments[].id` from retryable delivery logs. This keeps retry behavior compatible with previously generated subscription payloads and avoids regenerating files with a different timestamp or content.

- [x] **Step 2: Rehydrate email MIME attachments during retry**

For `EMAIL` retries, the runtime downloads completed historical attachments through `BiDeliveryAttachmentService.download(...)` and passes them to `BiDeliveryAdapterService` as `BiEmailAttachment` objects. Non-email channels continue to use text download links.

- [x] **Step 3: Cover retry-time attachment replay**

Add a runtime test that retries a failed email delivery log with historical attachment metadata and verifies the adapter receives the original CSV attachment bytes.

- [x] **Step 4: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 117 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: managed browser screenshot execution cluster, multi-page PDF rendering, object storage retention, download audit, retry backoff policy, and distributed scheduler lease.

## Task 31: Add Configurable Browser Screenshot Renderer For Subscription Snapshots

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/HttpBiSnapshotRenderer.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/HttpBiSnapshotRendererTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentServiceTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add snapshot renderer SPI**

Introduce `BiSnapshotRenderer` with request/result DTOs so the BI delivery layer can request image snapshots without binding the backend directly to a specific browser automation implementation.

- [x] **Step 2: Add HTTP renderer adapter**

`HttpBiSnapshotRenderer` is disabled by default and becomes active with `canvas.bi.delivery.snapshot.renderer.enabled:true` and `canvas.bi.delivery.snapshot.renderer.url`. It posts HTML, resource URL, PNG/JPEG format, viewport width/height, scale, and metadata to an internal renderer service and expects JSON with base64 image data.

- [x] **Step 3: Support PNG/JPEG subscription snapshot attachments**

`BiDeliveryAttachmentService` now accepts `snapshotFormat`/`screenshotFormat` or `attachments: ["PNG"|"JPEG"]`. It renders image snapshots through the configured renderer and keeps HTML as the default local fallback for `SNAPSHOT`/`SNAPSHOT_LINK`.

- [x] **Step 4: Cover renderer and attachment integration**

Tests cover HTTP renderer configuration/response decoding and attachment generation using a fake configured renderer.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 121 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: managed Playwright/Browserless renderer deployment, pixel-level visual regression checks, multi-page PDF rendering, object storage retention, download audit, retry backoff policy, and distributed scheduler lease.

## Task 32: Add Delivery Attachment Retention And Download Audit

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V208__bi_delivery_attachment_retention.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentCleanupResult.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryAttachmentDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Extend attachment persistence metadata**

Add `retention_days`, `expires_at`, `download_count`, and `last_downloaded_at` to `bi_delivery_attachment`. The domain object and view now expose those fields so runtime payloads and UI can show retention state.

- [x] **Step 2: Enforce expiry and write download audit**

`BiDeliveryAttachmentService.download(...)` now rejects expired completed files, marks them `EXPIRED`, and increments `download_count` plus `last_downloaded_at` after successful reads.

- [x] **Step 3: Add cleanup operation**

`BiDeliveryAttachmentService.cleanupExpiredAttachments(...)` scans expired completed/failed attachments for the current tenant, deletes local files under the configured attachment root, marks rows `EXPIRED`, and returns checked/expired/deleted/failed counters. The controller exposes `POST /canvas/bi/delivery-attachments/cleanup`.

- [x] **Step 4: Surface retention in the BI workbench**

The delivery log attachment tooltip now shows status, expiry, retention days, and download count. The subscription area shows attachment/download/expired summaries and includes a cleanup button.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 124 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: object-storage-backed attachment retention, managed Playwright/Browserless renderer deployment, pixel-level visual regression checks, multi-page PDF rendering, retry backoff policy, and distributed scheduler lease.

## Task 33: Add Distributed Lease For BI Delivery Scheduler

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V209__bi_delivery_scheduler_lease.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliverySchedulerLeaseDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliverySchedulerLeaseMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerLeaseService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerLeaseServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerServiceTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add BI scheduler lease table**

`bi_delivery_scheduler_lease` stores tenant-scoped lease keys, owner IDs, lease expiry, and last acquired time. The unique key on `(tenant_id, lease_key)` makes scheduler ownership atomic across application instances.

- [x] **Step 2: Add lease service**

`BiDeliverySchedulerLeaseService` supports acquire/release with configurable owner ID (`canvas.bi.delivery.scheduler.lease-owner-id`) and validates positive TTL. Acquisition only succeeds when the lease is expired or already owned by the same owner.

- [x] **Step 3: Guard automatic scheduler cycles**

`BiDeliverySchedulerService.runScheduledOnce(...)` now acquires `BI_DELIVERY_SCHEDULER` before running due checks. If another instance owns the lease, the cycle returns a skipped result and does not query subscriptions or alerts. Manual `runDueOnce(...)` remains available for operator-triggered catch-up.

- [x] **Step 4: Cover lease behavior**

Tests cover successful acquisition, denied acquisition, owner-only release, invalid lease requests, automatic scheduler execution under an acquired lease, and skipped execution when the lease is held elsewhere.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 130 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: object-storage-backed attachment retention, managed Playwright/Browserless renderer deployment, pixel-level visual regression checks, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 34: Add Backoff Policy For Failed BI Delivery Retries

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V210__bi_delivery_retry_backoff.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryLogDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryLogView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Extend delivery log retry metadata**

Add `retry_count`, `max_retry_count`, `next_retry_at`, `last_retry_at`, and `retry_exhausted_at` to `bi_delivery_log`, plus a retry due index for tenant/status/next time lookup.

- [x] **Step 2: Apply configurable retry backoff**

`BiDeliveryRuntimeService` now schedules retryable `FAILED` and `PENDING_ADAPTER` logs with default max attempts 4, initial delay 30 minutes, multiplier 2, and max delay 1440 minutes. These are configurable through `canvas.bi.delivery.retry.max-attempts`, `initial-delay-minutes`, `backoff-multiplier`, and `max-delay-minutes`.

- [x] **Step 3: Retry only due, non-exhausted logs**

`retryPendingDeliveries(...)` selects only non-evaluation logs whose `next_retry_at` is due or absent, whose `retry_count` is below the configured maximum, and whose `retry_exhausted_at` is still empty. Each retry creates a new log with incremented `retry_count`, `last_retry_at`, and either a new `next_retry_at` or `retry_exhausted_at`.

- [x] **Step 4: Surface retry state in the workbench**

The delivery history status cell now shows retry count, max retry count, next retry time, and exhausted state for failed/pending deliveries.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 135 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: object-storage-backed attachment retention, managed Playwright/Browserless renderer deployment, pixel-level visual regression checks, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 35: Add Retention And Download Audit For Self-Service Exports

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V211__bi_export_job_retention.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportCleanupResult.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Extend export task metadata**

Add `retention_days`, `expires_at`, `download_count`, and `last_downloaded_at` to `bi_export_job`, plus an expiry lookup index. The domain object and view expose these fields so the task list can show retention and audit state.

- [x] **Step 2: Enforce expiry and audit downloads**

`BiSelfServiceExportService.createExport(...)` now assigns configurable retention (`canvas.bi.export.retention-days`, default 7 days). `download(...)` rejects expired completed exports, marks them `EXPIRED`, and increments download count plus last downloaded time after successful reads.

- [x] **Step 3: Add export cleanup operation**

`cleanupExpiredExports(...)` scans expired completed/failed exports for the current tenant, deletes deterministic local files under the configured export root, marks tasks `EXPIRED`, and returns checked/expired/deleted/failed counters. The controller exposes `POST /canvas/bi/self-service/exports/cleanup`.

- [x] **Step 4: Surface export retention in the BI workbench**

The self-service export task list now shows task counts, download counts, expired counts, cleanup results, per-task expiry/retention tooltips, and disables download for expired or not-ready tasks.

- [x] **Step 5: Verify backend and frontend**

Run focused backend compile/tests and frontend checks. Record observed results after verification.

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 138 tests, frontend `biWorkbench` passed with 10 tests, and frontend production build completed.

Remaining production work after this task: async export queue, export approval, object-storage-backed export retention, million-row partitioning, progress polling, retry, audit detail pages, managed screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 36: Add QuickBI-Style Dashboard Designer Interactions

**Files:**
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Capture official UI/interaction references**

Use Quick BI dashboard creation, chart toolbar, dashboard management, query control, and drill/link/jump docs as the design reference for the workbench surface.

- [x] **Step 2: Add testable dashboard designer operations**

Add pure helpers for widget duplication, deletion with dependent interaction cleanup, directional layout nudging within the 20-column canvas, and shared designer resource search.

- [x] **Step 3: Surface component actions in the workbench**

The dashboard canvas widget cards now expose SQL view, copy, delete, and directional move actions. The left resource panel supports a shared search across fields, chart assets, chart components, and controls.

- [x] **Step 4: Add data/SQL inspection**

The right data panel now shows query result status, row count, duration, SQL hash, first rows, and can call the backend semantic compiler to show parameterized SQL for the selected widget.

- [x] **Step 5: Verify frontend and backend**

Run focused frontend tests/build and the BI backend suite. Record observed results after verification.

Observed on 2026-06-05:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
```

Observed result: frontend `biWorkbench` passed with 15 tests, frontend production build completed, backend clean compile succeeded, isolated BI test compilation succeeded, and BI Surefire suite passed with 138 tests.

Remaining production work after this task: drag-and-drop resizing, undo/redo, clone/archive/import/export, portal integration, publishing approval, resource permission checks, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 37: Add Dashboard Publish Version History

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V212__bi_dashboard_version_history.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardVersionView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Capture Quick BI dashboard lifecycle reference**

Quick BI dashboard management requires published-resource lifecycle awareness around share/embed/copy/delete operations. Canvas BI should preserve publish-time snapshots so future rollback, clone/export, approval, and deletion impact analysis have a stable source of truth.

- [x] **Step 2: Add dashboard version persistence**

Add `bi_dashboard_version` with tenant/workspace/dashboard identity, version number, status, full preset JSON, publisher, and created time. Publishing a persisted dashboard now inserts a version snapshot after the dashboard version increments.

- [x] **Step 3: Expose version history API**

Add `GET /canvas/bi/dashboards/resources/{dashboardKey}/versions?limit=...` and cap list size to 100. The API resolves the current tenant and returns typed `BiDashboardVersionView` entries with deserialized dashboard presets.

- [x] **Step 4: Surface publish history in the workbench**

Add `biApi.listDashboardVersions`, load version history for the active dashboard, refresh it after publish, and render a QuickBI-like release history band with version, status, snapshot summary, publisher, and publish time.

- [x] **Step 5: Verify frontend and backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean compile succeeded, isolated BI test compilation succeeded, BI Surefire suite passed with 143 tests, frontend `biWorkbench` passed with 15 tests, and frontend production build completed.

Remaining production work after this task: dashboard clone/archive/import/export, version rollback, chart/dataset/portal version snapshots, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 38: Add Dashboard Clone and Archive Management

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardCloneCommand.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Align with Quick BI dashboard management**

Quick BI dashboard management includes copy and delete/offline style operations. Canvas BI now models those as clone-to-draft and archive, preserving tenant/workspace boundaries and avoiding destructive deletes.

- [x] **Step 2: Add backend clone/archive operations**

Add `BiDashboardCloneCommand`, `BiDashboardMapper.archive`, `BiDashboardResourceService.cloneResource`, and `BiDashboardResourceService.archive`. The list API hides `ARCHIVED` dashboards. Clone supports copying persisted dashboards and built-in presets, validates safe resource keys, and refuses to overwrite an existing dashboard key.

- [x] **Step 3: Expose lifecycle endpoints**

Add `POST /canvas/bi/dashboards/resources/{dashboardKey}/clone` and `DELETE /canvas/bi/dashboards/resources/{dashboardKey}`. Controller tests verify current tenant and username propagation for clone and current tenant propagation for archive.

- [x] **Step 4: Add workbench management actions**

Add `cloneDashboard` and `archiveDashboard` frontend API calls, QuickBI-like toolbar actions for copy and archive, a deterministic clone command helper, loading states, archive disablement for built-in presets, and navigation to the copied dashboard key.

- [x] **Step 5: Verify frontend and backend**

Observed on 2026-06-05:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
```

Observed result: frontend `biWorkbench` passed with 16 tests, frontend production build completed, backend clean compile succeeded, isolated BI test compilation succeeded, and BI Surefire suite passed with 147 tests.

Remaining production work after this task: dashboard import/export, version rollback, chart/dataset/portal version snapshots, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 39: Add Dashboard Version Restore

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Design restore semantics**

Restore reads a published snapshot, writes it back through the existing save-draft path, preserves the current dashboard resource version number, and leaves publication as an explicit later action.

- [x] **Step 2: Add backend restore API**

Add `BiDashboardResourceService.restoreVersion` and `POST /canvas/bi/dashboards/resources/{dashboardKey}/versions/{version}/restore`. The service validates positive version numbers, resolves the snapshot by tenant/workspace/dashboard id/version, deserializes the saved preset JSON, and rewrites widgets through `saveDraft`.

- [x] **Step 3: Add workbench restore action**

Add `biApi.restoreDashboardVersion` and a release-history table action. Restoring a row updates the active dashboard resource, swaps the preset, selects the first restored widget, and refreshes release history.

- [x] **Step 4: Verify frontend and backend**

Observed on 2026-06-05:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
```

Observed result: frontend `biWorkbench` passed with 16 tests, frontend production build completed, backend clean compile succeeded, isolated BI test compilation succeeded, and BI Surefire suite passed with 149 tests.

Remaining production work after this task: chart/dataset/portal version snapshots, resource-package file upload/download, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 40: Add Dashboard Resource Package Import and Export

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardExportPackage.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardImportCommand.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Align with Quick BI resource package semantics**

Quick BI supports resource-package migration for published report resources. Canvas BI maps that to a typed dashboard JSON package that can move dashboard definitions across workspace/tenant contexts without carrying query result data.

- [x] **Step 2: Add backend package model and API**

Add `BiDashboardExportPackage` and `BiDashboardImportCommand`. Export requires a published dashboard and returns resource type, schema version, source dashboard key/version, full preset, exporter, and export time. Import validates resource type/schema version, validates target key safety, refuses accidental overwrite, and writes the package back as a draft through the existing save-draft path.

- [x] **Step 3: Add workbench package actions**

Add `biApi.exportDashboard`, `biApi.importDashboard`, QuickBI-like toolbar actions for export/import, typed frontend package contracts, deterministic import command generation, loading states, published-only export disablement, import disablement until a package is available, and a release-history tag showing the latest exported package version.

- [x] **Step 4: Verify frontend and backend**

Observed on 2026-06-05:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
```

Observed result: frontend `biWorkbench` passed with 17 tests, frontend production build completed, backend clean compile succeeded, isolated BI test compilation succeeded, and BI Surefire suite passed with 153 tests.

Remaining production work after this task: chart/dataset/portal version snapshots, resource-package file upload/download, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 41: Add Chart Publish Version History and Restore

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V240__bi_chart_version_history.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiChartVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiChartVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartVersionView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiChartControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Align with Quick BI publish and historical-version semantics**

Quick BI dashboard history and restore behavior establishes that published resources need durable snapshots, not only mutable draft rows. Canvas BI applies the same lifecycle expectation to reusable chart assets so a chart can be published, audited, selected from history, and restored into a draft without overwriting the published snapshot.

- [x] **Step 2: Add chart version persistence and service behavior**

Add `bi_chart_version` with tenant/workspace/chart/version uniqueness. `BiChartResourceService.publish` now writes a chart JSON snapshot with publisher attribution, derives the next version from the latest chart-version row, and keeps compatibility with existing callers. `listVersions` returns recent published snapshots, and `restoreVersion` validates the target version, deserializes the saved chart resource, and rewrites the current chart through the existing draft-save validation path.

- [x] **Step 3: Add chart version API and workbench restore action**

Add `GET /canvas/bi/charts/resources/{chartKey}/versions` and `POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore`. The workbench now tracks the selected chart asset, highlights it in the chart resource list, loads its publish history, and exposes a “恢复为草稿” action in the “图表发布历史” table.

- [x] **Step 4: Verify frontend and backend**

Observed on 2026-06-05:

```bash
cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
mkdir -p backend/canvas-engine/target/test-classes
rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | xargs "$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false surefire:test
```

Observed result: frontend `biWorkbench` passed with 17 tests, frontend production build completed, backend clean compile succeeded, isolated BI test compilation succeeded, and BI Surefire suite passed with 158 tests.

Remaining production work after this task: dataset/portal version snapshots, resource-package file upload/download, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 42: Add Dataset And Portal Publish Version History and Restore

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V241__bi_dataset_portal_version_history.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetVersionView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalVersionView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDatasetControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add dataset and portal version persistence**

Add `bi_dataset_version` and `bi_portal_version` with tenant/workspace/resource/version uniqueness. Dataset and portal publish now write JSON snapshots with publisher attribution and monotonically increasing resource-local versions.

- [x] **Step 2: Add service list and restore behavior**

`BiDatasetResourceService` and `BiPortalResourceService` now expose `listVersions` and `restoreVersion`. Restore validates the requested version, deserializes the saved resource payload, and routes it through the existing draft-save validation path.

- [x] **Step 3: Add dataset and portal version APIs and workbench history tables**

Add:

```text
GET /canvas/bi/datasets/resources/{datasetKey}/versions
POST /canvas/bi/datasets/resources/{datasetKey}/versions/{version}/restore
GET /canvas/bi/portals/resources/{portalKey}/versions
POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore
```

The workbench now exposes typed `biApi` methods, selected dataset/portal asset state, radio selection in asset tables, and “数据集发布历史” / “门户发布历史” tables with restore actions.

- [x] **Step 4: Verify frontend and backend**

Observed on 2026-06-05:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=BiDatasetResourceServiceTest,BiPortalResourceServiceTest,BiDatasetControllerTest,BiPortalControllerTest test
TESTS=$(rg --files backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi | sed 's#.*/##; s#\.java$##' | paste -sd, -)
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest="$TESTS" -DfailIfNoTests=false test

cd frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean compile succeeded; dataset/portal focused tests passed with 24 tests; broader BI backend suite passed with 168 tests; frontend `biWorkbench` passed with 17 tests; frontend production build completed.

Remaining production work after this task: resource-package file upload/download, movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 43: Add Dashboard Resource Package File Upload and Download

**Files:**
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add package helper tests**

Extend `biWorkbench` tests for deterministic backend-aligned `.bi-dashboard.json` filenames and client-side package validation before upload. The test was first run red against the looser `.canvas-bi-dashboard.json` helper and missing resource-type validation.

- [x] **Step 2: Wire backend file endpoints into the workbench**

Add `biApi.exportDashboardFile` and `biApi.importDashboardFile`. The workbench export action now downloads bytes from `GET /canvas/bi/dashboards/resources/{dashboardKey}/export-file` after loading package metadata, and the import action opens a file picker, validates the selected JSON package, then uploads it through `POST /canvas/bi/dashboards/resources/import-file`.

- [x] **Step 3: Update product spec**

Document dashboard resource-package file download/upload endpoints, filename convention, and frontend behavior. Move resource-package file upload/download out of the remaining lifecycle gap list.

- [x] **Step 4: Verify frontend and backend coverage**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest='Bi*Test,MarketingBi*Test' -DfailIfNoTests=false test
```

Observed result: frontend `biWorkbench` passed with 19 tests, frontend production build completed, and the broader BI backend suite passed with 167 tests including dashboard file export/import controller and service coverage.

Remaining production work after this task: movement, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 44: Add BI Resource Movement Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V243__bi_resource_location.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceLocationDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceLocationMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceMoveCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLocationView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceMovementController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceMovementControllerTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED movement tests**

Add service tests for tenant/workspace-scoped dashboard movement, missing-resource rejection, and location listing. Add controller tests for tenant/user propagation on move and tenant-scoped location listing. The first targeted Maven run failed on missing `BiResourceMovementService`, command/view records, location DO/mapper, and controller classes.

- [x] **Step 2: Add movement persistence**

Add `bi_resource_location` with tenant/workspace/resource uniqueness and folder/sort indexes. Add `BiResourceLocationDO` and `BiResourceLocationMapper.upsert` so repeated moves update the same resource location rather than creating duplicates.

- [x] **Step 3: Add movement service and API**

Add `BiResourceMovementService` and `BiResourceMovementController`. `POST /canvas/bi/resources/move` validates resource type, resource key, folder key, sort order, tenant/workspace scope, and resource existence for datasets, dashboards, charts, and portals. `GET /canvas/bi/resources/locations` returns folder/sort ordered location rows with optional resource type filtering.

- [x] **Step 4: Verify movement coverage**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/canvas-engine/pom.xml -DskipTests compile
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceMovementControllerTest.java
mvn -f backend/canvas-engine/pom.xml -Dtest=BiResourceMovementServiceTest,BiResourceMovementControllerTest -DfailIfNoTests=false surefire:test
mvn -f backend/canvas-engine/pom.xml -Dtest='Bi*Test,MarketingBi*Test' -DfailIfNoTests=false surefire:test
```

Observed result: backend compile succeeded; isolated movement tests passed with 9 tests, 0 failures, 0 errors, 0 skipped; broader compiled BI Surefire suite passed with 174 tests, 0 failures, 0 errors, 0 skipped. A normal `mvn ... test` run is currently blocked at full test compilation by unrelated new canvas attribution/control-group tests in the dirty worktree.

Remaining production work after this task: workbench movement controls, transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 45: Add Workbench Resource Movement Controls

**Files:**
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add frontend resource movement contracts and helpers**

Add `BiResourceMoveCommand`, `BiResourceLocationView`, `biApi.listResourceLocations`, and `biApi.moveResource`, aligned to `GET /canvas/bi/resources/locations` and `POST /canvas/bi/resources/move`. Add workbench helpers for resource location indexing, folder labels, normalized folder keys, and deterministic move command generation.

- [x] **Step 2: Add workbench controls**

Load resource locations, expose a resource location band, allow moving the current dashboard or selected chart/dataset/portal to a folder key, and update the local location table after a successful move.

- [x] **Step 3: Verify frontend movement coverage**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceMovementControllerTest.java
JAVA_HOME="$JAVA_HOME" PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH" mvn -f backend/canvas-engine/pom.xml \
  -Dtest=BiResourceMovementServiceTest,BiResourceMovementControllerTest \
  -DfailIfNoTests=false surefire:test
```

Observed result: `biWorkbench` passed with 21 tests; frontend production build completed; backend main compile completed; isolated movement service/controller tests passed with 9 tests, 0 failures, 0 errors, 0 skipped. A direct Maven targeted `test` run is still blocked at full `testCompile` by unrelated dirty-worktree tests outside BI movement.

Remaining production work after this task: transfer, favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 46: Add BI Resource Transfer Foundation And Workbench Controls

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V244__bi_resource_ownership.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceOwnershipDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceOwnershipMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceOwnershipView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceTransferController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceTransferControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED transfer tests**

Add service tests for transfer migration audit columns, tenant/workspace-scoped dashboard transfer, archived-resource rejection, and ownership listing. Add controller tests for `/canvas/bi/resources`, `POST /transfer`, `GET /ownerships`, and tenant/user propagation. The first Maven run failed at test compilation because `BiResourceOwnershipDO`, `BiResourceOwnershipMapper`, `BiResourceTransferCommand`, `BiResourceOwnershipView`, `BiResourceTransferService`, and `BiResourceTransferController` did not exist.

- [x] **Step 2: Add transfer persistence**

Add `bi_resource_ownership` with tenant/workspace/resource uniqueness, current `owner_user`, `transferred_by`, and `transferred_at`. Add `BiResourceOwnershipDO` and `BiResourceOwnershipMapper.upsert` so repeated transfers update the current owner while preserving the latest transfer audit metadata.

- [x] **Step 3: Add transfer service and API**

Add `BiResourceTransferService` and `BiResourceTransferController`. `POST /canvas/bi/resources/transfer` validates resource type, resource key, owner user, tenant/workspace scope, resource existence, and archived-resource status for datasets, dashboards, charts, and portals. `GET /canvas/bi/resources/ownerships` returns current ownership rows with optional resource type filtering.

- [x] **Step 4: Add frontend transfer controls**

Add `BiResourceTransferCommand`, `BiResourceOwnershipView`, `biApi.listResourceOwnerships`, and `biApi.transferResource`. Add workbench helpers for ownership indexing, owner labels, and normalized transfer commands. The resource-management band now shows current owners for the selected dashboard/chart/dataset/portal and can transfer the selected resource to a new owner account.

- [x] **Step 5: Verify transfer coverage**

Observed on 2026-06-05: the first targeted backend test run was blocked at full `testCompile` by unrelated dirty-worktree tests. Isolated transfer tests passed with 9 tests, 0 failures, 0 errors, 0 skipped. A broader compiled BI Surefire suite passed with 185 tests, 0 failures, 0 errors, 0 skipped. `biWorkbench` passed with 22 tests, and frontend production build completed.

Remaining production work after this task: favorites, comments, collaboration locks, publishing approval, resource permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 47: Add BI Resource Favorite Foundation And Workbench Controls

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V245__bi_resource_favorite.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceFavoriteDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceFavoriteMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceFavoriteController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceFavoriteControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED favorite tests**

Add service tests for user-scoped favorite uniqueness, tenant/workspace-scoped resource validation, archived-resource rejection, current-user listing, and unfavorite deletion. Add controller tests for `/canvas/bi/resources/favorites` routes and tenant/user propagation. The RED pass failed because favorite persistence, service, command/view, and controller classes did not exist or did not yet expose the `favorite` state flag.

- [x] **Step 2: Add favorite persistence and service**

Add `bi_resource_favorite` with tenant/workspace/resource/user uniqueness. `BiResourceFavoriteService` validates resource type/key, resolves the default workspace, rejects archived resources, supports favorite/unfavorite toggles, and lists favorites for the current user.

- [x] **Step 3: Expose favorite API and workbench controls**

Expose `GET /canvas/bi/resources/favorites`, `POST /canvas/bi/resources/favorites`, and `DELETE /canvas/bi/resources/favorites/{resourceType}/{resourceKey}`. Add frontend API contracts, favorite indexing/label helpers, favorite command normalization, and a workbench favorites band that can favorite or unfavorite the selected dashboard/chart/dataset/portal.

- [x] **Step 4: Verify favorite coverage**

Observed on 2026-06-05: backend main compile succeeded; isolated favorite/controller tests passed with 10 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed after favorite helpers were present.

Remaining production work after this task: comments, collaboration locks, publishing approval, save/publish permission checks, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 48: Add BI Resource Comments And Collaboration Locks

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V246__bi_resource_collaboration.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceCommentDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceLockDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceCommentMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceLockMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCommentCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCommentView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLockCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLockView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCollaborationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceCollaborationControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED collaboration tests**

Add migration, service, and controller tests for resource/widget-scoped comments, soft delete, archived-resource rejection, lock acquisition with TTL, active foreign-lock rejection, current lock lookup, and token/user-bound lock release. The RED pass failed because collaboration persistence, commands/views, service, controller, and routes did not exist.

- [x] **Step 2: Add comments and lock persistence**

Add `bi_resource_comment` for resource-level and widget-level comments with creator soft delete. Add `bi_resource_lock` with resource uniqueness, lock token, lock holder, locked time, and expiry. The lock mapper conditionally upserts so expired locks, same-user locks, and same-token locks can be renewed while active foreign locks remain unchanged.

- [x] **Step 3: Expose collaboration API and workbench controls**

Expose `GET /canvas/bi/resources/comments`, `POST /canvas/bi/resources/comments`, `DELETE /canvas/bi/resources/comments/{commentId}`, `GET /canvas/bi/resources/locks`, `POST /canvas/bi/resources/locks/acquire`, and `POST /canvas/bi/resources/locks/release`. Add frontend API contracts, comment/lock helpers, workbench comment composer/list/delete controls, and lock acquire/release controls for the selected dashboard/chart/dataset/portal.

- [x] **Step 4: Verify collaboration coverage**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"   mvn -f backend/canvas-engine/pom.xml -DskipTests compile

JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
find backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi -name '*.java' -print0 |   xargs -0 "$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes

JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"   mvn -f backend/canvas-engine/pom.xml -Dtest='Bi*Test,MarketingBi*Test' -DfailIfNoTests=false surefire:test

PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend main compile succeeded; focused favorite/collaboration tests passed with 23 tests, 0 failures, 0 errors, 0 skipped; broader compiled BI Surefire suite passed with 209 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 25 tests; frontend production build completed. A normal Maven targeted `test` command is still blocked by unrelated dirty-worktree test compilation outside the BI package.

Remaining production work after this task: publish approval enforcement, save/publish permission checks, edit-lock enforcement inside dataset/dashboard/chart/portal save endpoints, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 49: Add BI Publish Approval Foundation And Workbench Controls

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V248__bi_publish_approval.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPublishApprovalDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPublishApprovalMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalRequestCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalReviewCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPublishApprovalControllerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED publish approval tests**

Add migration, service, and controller tests for a generic BI publish approval request/review/list lifecycle across dataset/dashboard/chart/portal resource keys. The RED pass failed because `BiPublishApprovalDO`, `BiPublishApprovalMapper`, request/review commands, view, service, controller, and migration did not exist.

- [x] **Step 2: Add publish approval persistence and service**

Add `bi_publish_approval` with tenant/workspace/resource/status indexes, request metadata, reviewer metadata, and review comment. `BiPublishApprovalService` validates resource type/key, resolves the default BI workspace, rejects archived resources, creates `PENDING` requests, reviews pending requests to `APPROVED` or `REJECTED`, and lists approvals with resource/status filters.

- [x] **Step 3: Expose publish approval API and workbench controls**

Expose `GET /canvas/bi/resources/publish-approvals`, `POST /canvas/bi/resources/publish-approvals`, and `POST /canvas/bi/resources/publish-approvals/{approvalId}/review`. Add frontend API contracts, helper tests for normalized request/review commands and status labels, and a workbench publish-approval band for the selected dashboard/chart/dataset/portal.

- [x] **Step 4: Fix existing BI constructor compile blocker**

During full backend compile, `BiDatasetResourceService` had an ambiguous `null` constructor delegation between `ObjectProvider<BiResourcePermissionGuard>` and `BiResourcePermissionGuard`. The delegation now casts the `null` to `BiResourcePermissionGuard`, preserving behavior while allowing Java 21 compilation.

- [x] **Step 5: Verify publish approval coverage**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/canvas-engine/pom.xml -DskipTests compile
mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=/Users/photonpay/project/canvas/backend/canvas-engine/target/test-classpath.txt dependency:build-classpath
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPublishApprovalControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceFavoriteControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceCollaborationControllerTest.java
mvn -f backend/canvas-engine/pom.xml -Dtest=BiPublishApprovalServiceTest,BiPublishApprovalControllerTest,BiResourceFavoriteServiceTest,BiResourceFavoriteControllerTest,BiResourceCollaborationServiceTest,BiResourceCollaborationControllerTest -DfailIfNoTests=false surefire:test
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend main compile succeeded; backend source-level test compilation succeeded; resource lifecycle Surefire slice passed with 36 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 26 tests; frontend production build completed.

Remaining production work after this task: publish approval enforcement, save/publish permission checks, edit-lock enforcement inside dataset/dashboard/chart/portal save endpoints, drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 50: Enforce BI Publish Approval Gate

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add approval gate tests**

Add tests that a latest approved approval is accepted only when it is not stale relative to the resource `updatedAt`, and that stale/missing approvals fail with a publish-approval-required conflict.

- [x] **Step 2: Gate all resource publish flows**

Connect `BiPublishApprovalService.requireApprovedApproval(...)` to dataset, dashboard, chart, and portal `publish(...)` methods after publish-permission checks and before mapper publish calls. Non-admin roles must have a fresh approved approval; `ADMIN`, `SUPER_ADMIN`, and `TENANT_ADMIN` can publish without the approval gate, matching the Quick BI workspace-admin skip behavior.

- [x] **Step 3: Verify publish approval enforcement**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=target/test-classpath.txt dependency:build-classpath
JAVA_HOME=$(/usr/libexec/java_home -v 21)
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
find backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi -name '*.java' -print0 | xargs -0 "$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest='Bi*Test,MarketingBi*Test' -DfailIfNoTests=false surefire:test
npm test -- biWorkbench
npm run build
```

Observed result: backend clean compile succeeded; BI test source compilation succeeded; broader compiled BI Surefire suite passed with 234 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 26 tests; frontend production build completed.

Remaining production work after this task: drag-and-drop resizing, undo/redo, managed screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, export approval, object-storage-backed exports/attachments, and AI BI agents.

## Task 51: Enforce BI Save/Publish Permissions

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourcePermissionGuard.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDatasetControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiChartControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalControllerTest.java`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED permission and role propagation tests**

Add service tests proving existing-resource draft saves require `EDIT` and publish requires `PUBLISH`. Add controller tests proving tenant/user/role context is passed to dataset, dashboard, chart, and portal save/publish/restore service calls. The RED pass failed on the missing shared permission guard and missing role-aware service overloads.

- [x] **Step 2: Add shared resource permission guard**

Add `BiResourcePermissionGuard` as the resource lifecycle gate over `BiPermissionService.enforceResourceAccess(...)`. It builds a `BiQueryContext` from tenant, username, and role, defaults blank users to `system`, defaults blank roles to `OPERATOR`, and intentionally no-ops when a resource ID is not yet available so first-time creation remains possible.

- [x] **Step 3: Wire permissions into resource lifecycle**

Dataset, dashboard, chart, and portal draft saves now require `EDIT` for existing resources; publish requires `PUBLISH`; restore re-enters draft saving with the current role and therefore follows the same edit permission path. First-time resource creation remains allowed before a resource ID exists.

- [x] **Step 4: Verify backend and frontend**

Observed on 2026-06-05:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=/tmp/canvas-engine-test-classpath.txt dependency:build-classpath
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
CP="backend/canvas-engine/target/classes:$(cat /tmp/canvas-engine-test-classpath.txt):backend/canvas-engine/target/test-classes"
find backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi -name '*.java' -print0 | xargs -0 "$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d backend/canvas-engine/target/test-classes
cd backend/canvas-engine
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -Dtest='Bi*Test,MarketingBi*Test' -DfailIfNoTests=false -DforkCount=0 surefire:test
cd ../../frontend
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend main compile succeeded; BI test sources compiled with the regenerated test classpath; the BI Surefire suite passed with 234 tests, 0 failures, 0 errors, 0 skipped when run from the `canvas-engine` module directory with `forkCount=0`; `biWorkbench` passed with 26 tests; frontend production build completed. A forked Surefire run from the repository root was blocked by an environment-level temporary `target/surefire` booter jar open error before tests completed.

Remaining production work after this task: drag-and-drop resizing, undo/redo, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, and AI BI agents.

## Task 52: Enforce BI Edit Locks On Draft Save And Restore Mutations

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED edit-lock save tests**

Add tests for `BiResourceCollaborationService.requireCurrentLock(...)` and for dataset, dashboard, chart, and portal existing-resource draft saves. The RED pass failed on the missing lock-required exception, missing `requireCurrentLock` method, missing collaboration-service constructor injection, and missing `saveDraft(..., lockToken)` overloads.

- [x] **Step 2: Add current-lock verification**

`BiResourceCollaborationService.requireCurrentLock(...)` now validates tenant/workspace/resource scope, active non-expired lock row, matching `lockedBy`, and matching lock token. It returns a locked view on success and throws `BiResourceLockRequiredException` for missing, expired, foreign, or mismatched locks.

- [x] **Step 3: Wire edit locks into draft save endpoints**

Dataset, dashboard, chart, and portal services now expose lock-token-aware draft-save and version-restore overloads. The HTTP draft and restore endpoints read `X-BI-LOCK-TOKEN` and call those overloads so existing-resource writes require a current user lock after `EDIT` permission passes. `ADMIN`, `SUPER_ADMIN`, and `TENANT_ADMIN` bypass the edit-lock gate. Clone/import flows keep their internal save path because they create new draft resources rather than editing an existing resource.

- [x] **Step 4: Update frontend save and restore plumbing**

The BI API wrapper accepts optional lock tokens for dataset, dashboard, chart, and portal draft saves and version restores and sends them as `X-BI-LOCK-TOKEN`. The workbench uses a shared `resourceLockTokenFor(...)` helper so dashboard save and dashboard/chart/dataset/portal restore actions pass the acquired lock token only when the active lock matches the resource being mutated.

- [x] **Step 5: Verify edit-lock save enforcement**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21)
PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=/Users/photonpay/project/canvas/backend/canvas-engine/target/test-classpath.txt dependency:build-classpath
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt)"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP:backend/canvas-engine/target/test-classes" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java
mvn -f backend/canvas-engine/pom.xml -DskipTests compile
mvn -f backend/canvas-engine/pom.xml -Dtest=BiResourceCollaborationServiceTest,BiDatasetResourceServiceTest,BiDashboardResourceServiceTest,BiChartResourceServiceTest,BiPortalResourceServiceTest -DfailIfNoTests=false surefire:test
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: RED test source compilation failed on the expected missing lock APIs/overloads; after implementation, backend source-level test compilation succeeded, backend main compile succeeded, follow-up controller tests were updated to assert `X-BI-LOCK-TOKEN` propagation for draft save and version restore, the isolated compiled BI Surefire suite passed with 188 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 28 tests; frontend production build completed. A normal Maven `test` invocation is currently blocked before test execution by an unrelated dirty-worktree compile error in `CdpWarehouseSyntheticDataPathProbeServiceTest`.

Remaining production work after this task: dashboard drag movement, screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, export approval, object-storage-backed exports/attachments, and AI BI agents.

## Task 53: Add Dashboard Drag Movement, Resize Handles, And Undo/Redo Toolbar

**Files:**
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED drag, resize, and history helper tests**

Add tests proving dashboard widgets move and resize within the 20-column grid, pixel drag deltas convert to grid deltas, visual CSS grid placement reflects `gridX/gridY/gridW/gridH`, and dashboard preset history supports undo and redo. The RED pass failed because `moveDashboardWidgetByPixels`, `dashboardWidgetGridPlacement`, `resizeDashboardWidget`, `resizeDashboardWidgetByPixels`, and dashboard history helpers were not exported.

- [x] **Step 2: Implement drag, resize, and history helpers**

Add collision-aware bounded widget movement, bounded widget resizing with minimum width/height, pixel-to-grid conversion, explicit CSS grid placement generation, and a small immutable dashboard history model with push, undo, and redo operations. Add `undo` and `redo` to the QuickBI designer action list.

- [x] **Step 3: Wire dashboard canvas UI**

Dashboard widget cards now expose a title/header pointer drag handle that converts pointer movement into collision-aware grid position deltas and a bottom-right pointer drag handle that converts pointer movement into grid resize deltas. Workbench and embed rendering use `dashboardWidgetGridPlacement(...)` so `gridX/gridY/gridW/gridH` are reflected by CSS grid column and row placement. Local duplicate, remove, move, and resize edits are pushed into the dashboard history stack. The top toolbar now shows Undo/Redo buttons, disables them when no matching history exists, and resets history after server-loaded/saved/published/imported/restored presets.

- [x] **Step 4: Verify frontend**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: the RED `biWorkbench` run failed on the expected missing drag/resize/history helper exports; after implementation, `biWorkbench` passed with 29 tests and frontend production build completed.

Remaining production work after this task: screenshot execution cluster, multi-page PDF rendering, anomaly checks, notification audit, export approval, object-storage-backed exports/attachments, and AI BI agents.

## Task 54: Add BI Alert Baseline Anomaly Checks

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED anomaly runtime tests**

Add tests proving an `ANOMALY_DROP` alert uses recent evaluation logs as a baseline, triggers when the current metric drops beyond the configured sensitivity, writes anomaly evidence into payload, and skips without channel delivery when the baseline has fewer than `minSamples`. The RED run failed on the expected unsupported `ANOMALY_DROP` operator.

- [x] **Step 2: Implement baseline anomaly evaluation**

`BiDeliveryRuntimeService` now routes anomaly conditions separately from static threshold conditions. It supports `ANOMALY_DROP`, `ANOMALY_RISE`, mode/type `ANOMALY`, `baselineWindow`, `minSamples`, `sensitivity`, `minDelta`, and `minDeltaPercent`. Runtime loads recent non-null `EVALUATION` metric values for the same alert, computes baseline average, standard deviation, delta, delta percent, and threshold, writes those values into the delivery payload, and only delivers channels when the anomaly condition matches.

- [x] **Step 3: Accept anomaly alert definitions and expose a workbench entry**

Alert administration validation now accepts anomaly conditions without a static `threshold` while still validating positive baseline/sensitivity settings. The BI workbench can create an anomaly alert for the selected metric and displays anomaly condition summaries in the alert table.

- [x] **Step 4: Verify anomaly checks**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=BiDeliveryRuntimeServiceTest,BiSubscriptionAdminServiceTest -DfailIfNoTests=false test
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: the RED run failed on unsupported `ANOMALY_DROP`; after implementation, the focused backend service/admin suite passed with 15 tests, 0 failures, 0 errors, 0 skipped; backend main compile succeeded; `biWorkbench` passed with 29 tests; frontend production build completed.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering, notification audit, export approval, object-storage-backed exports/attachments, and AI BI agents.

## Task 55: Add Collision-Aware Dashboard Grid Positioning

**Files:**
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add collision-aware layout tests**

Update `biWorkbench` tests so invalid moves into occupied grid slots stay on the nearest valid placement, valid free-space moves still apply, and resize operations keep the resized widget size while moving overlapped widgets downward.

- [x] **Step 2: Implement grid collision helpers**

Add normalized widget rectangles, overlap checks, nearest available placement search, and resize overlap resolution. Dashboard movement now avoids occupied cells, pixel movement resolves to valid grid positions, and resizing reflows colliding widgets below the resized widget.

- [x] **Step 3: Render the dashboard with real CSS grid coordinates**

The workbench canvas now uses fixed 20-column grid placement with `gridX/gridY/gridW/gridH`, `gridAutoRows`, and row spans so saved layout coordinates are visually represented instead of only affecting metadata.

- [x] **Step 4: Verify frontend and local runtime**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
PATH="/opt/homebrew/bin:$PATH" npm run dev -- --host 127.0.0.1
```

Observed result: `biWorkbench` passed with 29 tests, frontend production build completed, and Vite started at `http://127.0.0.1:3004/` because ports 3000-3003 were already in use. Browser screenshot verification could not complete in this session because the in-app browser registry returned no available browser instances.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering, notification audit, export approval, object-storage-backed exports/attachments, mobile dashboard layout controls, multi-select alignment/snap guides, and AI BI agents.

## Task 56: Add BI Delivery Notification Audit Summary

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAuditSummary.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED delivery audit tests**

Add tests proving runtime audit summarizes a filtered delivery-log window and that the controller passes tenant, job type, status, channel, job ID, and limit into the runtime audit method. The RED run failed on the missing `BiDeliveryAuditSummary` model.

- [x] **Step 2: Implement audit summary API**

`BiDeliveryRuntimeService.auditDeliveries(...)` now filters `bi_delivery_log` by tenant, job type, status, channel, job ID, and limit, returns recent log views, and summarizes total, delivered, triggered, skipped, pending, failed, retryable, and retry-exhausted counts. `GET /canvas/bi/delivery-audit` exposes the summary through the existing tenant context.

- [x] **Step 3: Wire frontend audit visibility**

The BI API wrapper now exposes `auditDeliveryLogs(...)`; the workbench reloads the audit summary alongside delivery logs and shows audit, failed, retryable, and retry-exhausted counters in the delivery-record header.

- [x] **Step 4: Verify delivery audit**

Observed on 2026-06-05:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=/Users/photonpay/project/canvas/backend/canvas-engine/target/test-classpath.txt dependency:build-classpath
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-classpath.txt):backend/canvas-engine/target/test-classes"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java
mvn -f backend/canvas-engine/pom.xml -Dtest=BiDeliveryRuntimeServiceTest,BiSubscriptionControllerTest -DfailIfNoTests=false surefire:test
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: broad Maven `test` is currently blocked during unrelated test compilation by missing `AudienceSnapshotService` in dirty-worktree tests; focused main compile, targeted test javac, and Surefire slice passed with 23 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 29 tests; frontend production build completed.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering, object-storage-backed exports/attachments, mobile dashboard layout controls, multi-select alignment/snap guides, and AI BI agents.

## Task 57: Verify And Align Export Approval Foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportApprovalReviewCommand.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V255__bi_export_approval.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobCommand.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Verify current export approval implementation**

Inspect current `BiSelfServiceExportService`, `BiSelfServiceController`, frontend API types, and tests. Current worktree already includes approval-required/sensitive/row-threshold gating, `PENDING_APPROVAL`, approve/reject review, reviewer role checks, and `/canvas/bi/self-service/exports/{id}/review`.

- [x] **Step 2: Wire export approval model, API, and workbench controls**

Add export approval fields to `BiExportJobCommand`, `BiExportJobView`, and `BiExportJobDO`; persist them through `V255__bi_export_approval.sql`; expose `BiExportApprovalReviewCommand` and controller review endpoint; add workbench API types, sensitive export button, approval tags, and approve/reject actions in the export task table.

- [x] **Step 3: Fix brittle restored-query assertion**

Freshly compiled `BiSelfServiceExportServiceTest` exposed that an approved export restored from JSON converts numeric filter values from `Long` to `Integer`, making whole-record equality too strict even though the semantic query is equivalent. The test now captures `BiQueryRequest` and verifies dataset, fields, limit, filter field/operator/value semantics, and execution context directly.

- [x] **Step 4: Verify export approval slice**

Observed on 2026-06-05:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DincludeScope=test -Dmdep.outputFile=/tmp/canvas-engine-test-classpath.txt dependency:build-classpath
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" javac --release 21 -encoding UTF-8 -cp "backend/canvas-engine/target/classes:backend/canvas-engine/target/test-classes:$(cat /tmp/canvas-engine-test-classpath.txt)" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=BiSelfServiceExportServiceTest,BiSelfServiceControllerTest -DfailIfNoTests=false -DforkCount=0 surefire:test
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend main compile succeeded; targeted export test sources compiled; the focused export service/controller slice passed with 13 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 29 tests. Frontend production build is currently blocked in the dirty worktree by unrelated `src/pages/canvas-editor/messagePreview.test.ts` importing missing `./messagePreview`.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering, object-storage-backed exports/attachments, mobile dashboard layout controls, multi-select alignment/snap guides, async export queue/progress/retry, and AI BI agents.

## Task 58: Add Storage-Backed BI Exports And Delivery Attachments

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiFileStorage.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiStoredFile.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/LocalBiFileStorage.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V258__bi_file_storage_keys.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryAttachmentDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED storage abstraction tests**

Add export and delivery-attachment service tests that construct the services with a non-local `BiFileStorage`, require generated files to be written under deterministic storage keys, expose `storageProvider`/`storageKey` in views, and download bytes through storage instead of directly reading local filesystem paths. The RED focused compile failed because `org.chovy.canvas.domain.bi.storage.BiFileStorage` and `BiStoredFile` did not exist.

- [x] **Step 2: Add storage SPI, local provider, and schema fields**

Add `BiFileStorage`, `BiStoredFile`, and `LocalBiFileStorage`. Add `storage_provider` and `storage_key` columns to `bi_export_job` and `bi_delivery_attachment` through `V258__bi_file_storage_keys.sql`, and map them through DO/view/API types.

- [x] **Step 3: Route export and attachment read/write/delete through storage**

`BiSelfServiceExportService` now writes export bytes through `BiFileStorage`, stores provider/key on completed jobs, downloads through `storage_key`, and cleans up expired storage objects while retaining legacy local-path cleanup for older rows. `BiDeliveryAttachmentService` applies the same storage path to generated subscription attachments and retry-time downloads.

- [x] **Step 4: Verify storage-backed export and attachment slice**

Observed on 2026-06-05:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" javac --release 21 -encoding UTF-8 -cp "backend/canvas-engine/target/classes:backend/canvas-engine/target/test-classes:$(cat /tmp/canvas-engine-test-classpath.txt)" -d backend/canvas-engine/target/test-classes \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiFileStorage.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiStoredFile.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/LocalBiFileStorage.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryAttachmentDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentView.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=BiSelfServiceExportServiceTest,BiDeliveryAttachmentServiceTest,BiSelfServiceControllerTest,BiSubscriptionControllerTest,BiDeliveryRuntimeServiceTest -DfailIfNoTests=false -DforkCount=0 surefire:test
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: backend clean main compile succeeded; focused compile succeeded; focused export/attachment/controller/runtime slice passed with 45 tests, 0 failures, 0 errors, 0 skipped; `biWorkbench` passed with 29 tests; frontend production build completed.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering hardening, mobile dashboard layout controls, multi-select alignment/snap guides, async export queue/progress/retry, external S3/OSS/MinIO storage provider, and AI BI agents.

## Task 59: Add Dashboard Multi-Select Alignment And Snap Guides

**Files:**
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED layout alignment and snap tests**

Add `biWorkbench` tests proving multiple dashboard widgets can be aligned on left, bottom, and center lines while invalid selections are ignored, and proving near-edge drag placement snaps to nearby vertical/horizontal guide lines. The RED run failed with `alignDashboardWidgets is not a function` and `snapDashboardWidgetPlacement is not a function`.

- [x] **Step 2: Implement alignment and snap helpers**

Add `DashboardWidgetAlignment`, exported placement/snap guide types, `alignDashboardWidgets(...)`, and `snapDashboardWidgetPlacement(...)`. Alignment uses the selected widget bounding box and clamps results to the 20-column grid. Snap matching keeps center-to-center and edge-to-edge matching separate, prefers nearer guides, and uses perpendicular overlap to resolve ties.

- [x] **Step 3: Wire workbench multi-select and alignment controls**

The BI workbench now tracks selected widget keys, supports Ctrl/Shift/Meta click to toggle multi-select, highlights all selected cards, exposes icon-only alignment controls in the canvas toolbar, and routes alignment mutations through the existing dashboard history path. Pixel drag movement now runs through the snap helper before the existing nearest-available placement/collision path.

- [x] **Step 4: Verify frontend layout slice**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` passed with 31 tests; frontend production build completed. Browser-level visual verification was not run because this environment did not expose an in-app Browser control tool during tool discovery.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering hardening, mobile dashboard layout controls, async export queue/progress/retry, external S3/OSS/MinIO storage provider, and AI BI agents.

## Task 60: Add Mobile Dashboard Layout Preview Controls

**Files:**
- Modify: `frontend/src/pages/bi/biWorkbench.ts`
- Modify: `frontend/src/pages/bi/biWorkbench.test.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED responsive layout tests**

Add `biWorkbench` tests proving dashboard widgets map to desktop, tablet, and mobile layouts: desktop remains 20 columns, tablet scales into a 12-column non-overlapping layout, and mobile stacks widgets into a single column in visual order. The RED run failed because `dashboardResponsiveWidgets` did not exist.

- [x] **Step 2: Implement responsive layout helpers**

Add `DashboardLayoutMode`, `dashboardLayoutColumns(...)`, `dashboardResponsiveWidgets(...)`, and `dashboardWidgetGridPlacementForColumns(...)`. Desktop preserves normalized 20-column placement, tablet scales 20-column coordinates into 12 columns and reflows collisions, and mobile renders a deterministic one-column stack without mutating the persisted dashboard preset.

- [x] **Step 3: Wire layout preview controls**

The BI workbench canvas toolbar now exposes a compact segmented control for `桌面`、`平板`、`手机`. Rendering uses responsive display widgets and dynamic grid columns while existing save/publish/history paths continue using the original 20-column dashboard model.

- [x] **Step 4: Verify mobile layout slice**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
```

Observed result: `biWorkbench` passed with 32 tests; frontend production build completed. Browser-level visual verification was not run because this environment did not expose an in-app Browser control tool during tool discovery.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering hardening, async export queue/progress/retry, external S3/OSS/MinIO storage provider, and AI BI agents.

## Task 61: Add Self-Service Export Progress And Retry

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportRetryResult.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V260__bi_export_progress_retry.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java`
- Modify: `frontend/src/services/biApi.ts`
- Modify: `frontend/src/pages/bi/index.tsx`
- Modify: `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md`
- Modify: `docs/superpowers/plans/2026-06-05-quickbi-platform.md`

- [x] **Step 1: Add RED export progress/retry tests**

Add service tests proving failed exports retain progress metadata, schedule retry state, and due failed exports can be retried through the original structured query. Add a controller test proving `POST /canvas/bi/self-service/exports/retry` uses the current tenant, username, and role. The RED focused compile failed on the expected missing `BiExportRetryResult` symbol.

- [x] **Step 2: Persist export progress and retry metadata**

Add `progress_percent`, `retry_count`, `max_retry_count`, `next_retry_at`, `last_retry_at`, and `retry_exhausted_at` to `bi_export_job` through `V260__bi_export_progress_retry.sql`, and expose those fields through the DO, backend view, and frontend API type.

- [x] **Step 3: Implement failed export retry**

`BiSelfServiceExportService` now sets `QUEUED`/`RUNNING`/`COMPLETED` progress, schedules failed exports with configurable retry backoff (`canvas.bi.export.retry.*`), and exposes `retryFailedExports(...)` to rerun due failed jobs from their persisted original request JSON. `BiSelfServiceController` exposes `POST /canvas/bi/self-service/exports/retry`.

- [x] **Step 4: Surface export retry in the workbench**

The BI workbench export task list now shows progress bars and failed-export retry metadata, shows retryable and retry-result summary tags, and adds a compact “重试失败” action beside cleanup.

- [x] **Step 5: Verify export progress/retry slice**

Observed on 2026-06-05:

```bash
PATH="/opt/homebrew/bin:$PATH" npm test -- biWorkbench
PATH="/opt/homebrew/bin:$PATH" npm run build
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -DskipTests clean compile
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" javac --release 21 -encoding UTF-8 -cp "backend/canvas-engine/target/classes:backend/canvas-engine/target/test-classes:$(cat /tmp/canvas-engine-test-classpath.txt)" -d backend/canvas-engine/target/test-classes backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=BiSelfServiceExportServiceTest,BiSelfServiceControllerTest -DfailIfNoTests=false -DforkCount=0 surefire:test
```

Observed result: `biWorkbench` passed with 32 tests; frontend production build completed; backend clean main compile succeeded; focused export service/controller tests passed with 17 tests, 0 failures, 0 errors, 0 skipped. During clean compile verification, Maven initially failed to delete generated `target/classes/org/chovy/canvas/domain/warehouse`; removing generated `backend/canvas-engine/target` and rerunning clean compile succeeded.

Remaining production work after this task: managed screenshot execution cluster, multi-page PDF rendering hardening, true async export queue, external S3/OSS/MinIO storage provider, and AI BI agents.
