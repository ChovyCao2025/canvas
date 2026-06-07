# Flink Realtime Warehouse Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a runnable Flink realtime warehouse execution layer that feeds Doris and reports checkpoint evidence into the existing canvas-engine realtime pipeline control plane.

**Architecture:** Add a separate `canvas-flink-jobs` Maven module under `backend/`. The module owns Flink job config, SQL assets, a Table API launcher, and an HTTP checkpoint reporter; canvas-engine remains the control plane and receives evidence through existing APIs.

**Tech Stack:** Java 21, Maven, Apache Flink 1.20-compatible APIs, Flink CDC 3.4 connector packaging guidance, Apache Doris Flink connector, JUnit 5, AssertJ, Docker Compose.

---

## File Structure

- Modify: `backend/pom.xml` — add `canvas-flink-jobs` module and Flink/Doris connector version properties.
- Create: `backend/canvas-flink-jobs/pom.xml` — Flink job module dependencies and test config.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkJobMain.java` — CLI entrypoint.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkJobConfig.java` — typed environment/system-property config.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkPipelineRegistry.java` — pipeline key to SQL asset mapping.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkSqlTemplateLoader.java` — classpath SQL loader and placeholder renderer.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkSqlJobRunner.java` — Table API SQL executor.
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkCheckpointReporter.java` — HTTP checkpoint reporter.
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/*.sql` — four first-slice SQL pipeline assets.
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/*Test.java` — focused tests.
- Modify: `docker-compose.local.yml` — add optional Flink JobManager and TaskManager services.
- Create: `docs/runbooks/flink-realtime-warehouse.md` — local runbook.
- Create: `scripts/verify-flink-realtime-warehouse-focus.sh` — focused verification command.

## Task 1: Add Module Skeleton

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/canvas-flink-jobs/pom.xml`

- [ ] **Step 1: Write expected module verification**

Run before implementation:

```bash
cd backend
mvn -pl canvas-flink-jobs test
```

Expected RED: Maven fails because `canvas-flink-jobs` is not listed as a module.

- [ ] **Step 2: Add parent module entry**

Add this to `backend/pom.xml` under `<modules>`:

```xml
<module>canvas-flink-jobs</module>
```

Add properties:

```xml
<flink.version>1.20.0</flink.version>
<flink.cdc.version>3.4.0</flink.cdc.version>
<doris.flink.connector.version>24.0.1</doris.flink.connector.version>
```

- [ ] **Step 3: Create module POM**

Create `backend/canvas-flink-jobs/pom.xml` with parent `canvas-parent`, artifact id `canvas-flink-jobs`, Java 21, Flink API dependencies, Doris connector dependency, Jackson, JUnit 5, and AssertJ.

- [ ] **Step 4: Verify module compiles**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs test
```

Expected GREEN: module exists and runs with no tests yet.

## Task 2: Config Validation

**Files:**
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkJobConfigTest.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkJobConfig.java`

- [ ] **Step 1: Write failing tests**

Test:

- missing `CANVAS_FLINK_JOB_PIPELINE_KEY` fails;
- missing MySQL URL/user/password fails for MySQL source pipelines;
- missing Doris FE/BE/JDBC/user fails;
- missing canvas checkpoint endpoint fails;
- valid config exposes placeholder values for SQL rendering.

- [ ] **Step 2: Verify RED**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs -Dtest=CanvasFlinkJobConfigTest test
```

Expected RED: config class does not exist.

- [ ] **Step 3: Implement config**

Implement `from(Map<String, String> env)` and `placeholders()` with explicit required-field validation.

- [ ] **Step 4: Verify GREEN**

Run the same test. Expected: all config tests pass.

## Task 3: Pipeline Registry And SQL Assets

**Files:**
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkPipelineRegistryTest.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkPipelineRegistry.java`
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/mysql_cdp_event_log_to_doris_ods.sql`
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/mysql_canvas_trace_to_doris_ods.sql`
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/doris_ods_cdp_event_to_dwd_fact.sql`
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/doris_dwd_user_fact_to_dws_metric_daily.sql`

- [ ] **Step 1: Write failing tests**

Test that all four pipeline keys resolve to a SQL asset path and each SQL asset contains its expected sink table.

- [ ] **Step 2: Verify RED**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs -Dtest=CanvasFlinkPipelineRegistryTest test
```

Expected RED: registry and SQL assets are missing.

- [ ] **Step 3: Implement registry and SQL files**

Add immutable mappings for:

- `mysql_cdp_event_log_to_doris_ods` -> `sql/mysql_cdp_event_log_to_doris_ods.sql`
- `mysql_canvas_trace_to_doris_ods` -> `sql/mysql_canvas_trace_to_doris_ods.sql`
- `doris_ods_cdp_event_to_dwd_fact` -> `sql/doris_ods_cdp_event_to_dwd_fact.sql`
- `doris_dwd_user_fact_to_dws_metric_daily` -> `sql/doris_dwd_user_fact_to_dws_metric_daily.sql`

- [ ] **Step 4: Verify GREEN**

Run the same test. Expected: all registry tests pass.

## Task 4: SQL Template Loader

**Files:**
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkSqlTemplateLoaderTest.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkSqlTemplateLoader.java`

- [ ] **Step 1: Write failing tests**

Test:

- loader reads classpath SQL;
- renderer replaces `${PLACEHOLDER}` tokens;
- renderer fails if a placeholder has no value;
- renderer leaves no `${...}` token in final SQL.

- [ ] **Step 2: Verify RED**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs -Dtest=CanvasFlinkSqlTemplateLoaderTest test
```

Expected RED: loader does not exist.

- [ ] **Step 3: Implement loader**

Use classloader resources and deterministic placeholder replacement.

- [ ] **Step 4: Verify GREEN**

Run the same test. Expected: all loader tests pass.

## Task 5: Checkpoint Reporter

**Files:**
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkCheckpointReporterTest.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkCheckpointReporter.java`

- [ ] **Step 1: Write failing tests**

Test that a PASS report serializes the controller-compatible JSON payload and posts to `/warehouse/realtime/pipelines/checkpoints`.

- [ ] **Step 2: Verify RED**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs -Dtest=CanvasFlinkCheckpointReporterTest test
```

Expected RED: reporter class does not exist.

- [ ] **Step 3: Implement reporter**

Use Java `HttpClient`, Jackson, bounded timeout, and clear failure messages.

- [ ] **Step 4: Verify GREEN**

Run the same test. Expected: reporter tests pass.

## Task 6: SQL Job Runner And Main

**Files:**
- Create: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkJobMainTest.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkSqlJobRunner.java`
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkJobMain.java`

- [ ] **Step 1: Write failing tests**

Test that CLI args select a pipeline key and hand rendered SQL to a runner abstraction.

- [ ] **Step 2: Verify RED**

Run:

```bash
cd backend
mvn -pl canvas-flink-jobs -Dtest=CanvasFlinkJobMainTest test
```

Expected RED: main and runner do not exist.

- [ ] **Step 3: Implement runner and main**

Implement a small `SqlExecutor` seam so tests can verify wiring without starting Flink. Production runner uses `StreamTableEnvironment.executeSql` for each semicolon-delimited statement.

- [ ] **Step 4: Verify GREEN**

Run the same test. Expected: main wiring tests pass.

## Task 7: Local Deployment And Runbook

**Files:**
- Modify: `docker-compose.local.yml`
- Create: `docs/runbooks/flink-realtime-warehouse.md`
- Create: `scripts/verify-flink-realtime-warehouse-focus.sh`

- [ ] **Step 1: Add compose services**

Add optional `flink-jobmanager` and `flink-taskmanager` services using an explicit `flink:1.20.0-java17` image and do not make backend depend on them.

- [ ] **Step 2: Add runbook**

Document:

- local infra startup;
- building `canvas-flink-jobs`;
- submitting a pipeline;
- required env vars;
- how to inspect canvas-engine checkpoint status;
- explicit production caveats.

- [ ] **Step 3: Add verification script**

Script runs:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-flink-jobs test
JAVA_HOME=$(/usr/libexec/java_home -v 21) scripts/verify-enterprise-olap-focus.sh
git diff --check
```

- [ ] **Step 4: Verify**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) scripts/verify-flink-realtime-warehouse-focus.sh
```

Expected: focused Flink tests pass, existing enterprise OLAP focused tests pass, and diff check passes.

## Self-Review

- Spec coverage: Tasks cover module creation, config, SQL assets, template loading, checkpoint reporting, launcher wiring, Docker Compose, runbook, and verification.
- Placeholder scan: No `TBD` or `TODO` steps are present.
- Type consistency: Class names and file paths match the design doc and task list.
