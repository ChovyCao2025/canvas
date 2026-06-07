# Flink Realtime Warehouse Execution Design

## 1. Goal

Build the missing realtime warehouse execution layer for Marketing Canvas so the existing CDP warehouse control plane is backed by runnable Flink jobs, deployable local infrastructure, and verifiable checkpoint evidence.

This design is the first implementation slice toward the larger objective: complete offline warehouse, realtime warehouse, enterprise OLAP, and deployment readiness. It does not claim the whole platform is production-complete by itself.

## 2. Current Evidence

The current repository already has these capabilities:

- Doris ODS/DWD/DWS DDL assets in `backend/canvas-engine/src/main/resources/infrastructure/doris/`.
- Direct CDP event mirror into Doris Stream Load through `DorisCdpEventStreamLoader`.
- Offline aggregation from ODS to DWD/DWS through `CdpWarehouseAggregationService`.
- Realtime pipeline contracts, checkpoint ledgers, job heartbeats, incidents, and external job probes.
- Enterprise OLAP readiness/evidence gates that fail closed when real operating evidence is missing.

The current repository does not have:

- A Maven module for Flink jobs.
- Flink or Flink CDC dependencies.
- Runnable Flink CDC or Flink SQL job entrypoints.
- Flink deployment in local Docker Compose or production Helm/K8s.
- A job-side checkpoint reporter that calls `/warehouse/realtime/pipelines/checkpoints`.

## 3. External References

The execution design follows official Apache Flink and Apache Doris guidance:

- Apache Doris Flink Doris Connector: `https://doris.apache.org/docs/3.x/ecosystem/flink-doris-connector/`
  - Doris documents Flink read/write integration, Stream Load based writes, checkpoint-based streaming writes, and Flink CDC integration.
- Apache Flink CDC connector overview: `https://nightlies.apache.org/flink/flink-cdc-docs-release-3.0/docs/connectors/pipeline-connectors/overview/`
  - Flink CDC provides source and sink connectors through released JARs and YAML pipeline definitions, including MySQL source and Doris sink.
- Apache Flink CDC Doris connector: `https://nightlies.apache.org/flink/flink-cdc-docs-release-3.4/docs/connectors/pipeline-connectors/doris/`
  - Doris sink connector options include FE HTTP nodes, BE HTTP nodes, JDBC URL, user, password, and pipeline parallelism.
- Apache Flink Docker deployment docs: `https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/deployment/resource-providers/standalone/docker/`
  - Flink recommends explicit Docker image version tags to avoid runtime/classpath mismatch.

## 4. Scope

### 4.1 In Scope

1. Add a dedicated `backend/canvas-flink-jobs` Maven module.
2. Add Flink job configuration model and validation for:
   - MySQL connection.
   - Doris FE/BE/JDBC endpoints.
   - Canvas checkpoint endpoint.
   - Tenant id and pipeline key mapping.
3. Add SQL asset files for these jobs:
   - `mysql_cdp_event_log_to_doris_ods`
   - `mysql_canvas_trace_to_doris_ods`
   - `doris_ods_cdp_event_to_dwd_fact`
   - `doris_dwd_user_fact_to_dws_metric_daily`
4. Add a Java job launcher that executes a named SQL pipeline in Flink Table API.
5. Add a checkpoint reporter client that can publish checkpoint evidence to canvas-engine.
6. Add local Docker Compose support for Flink JobManager and TaskManager.
7. Add documentation and verification scripts for local startup and job submission.
8. Add tests that verify SQL assets, config validation, checkpoint payloads, and launch command wiring without requiring a live Flink cluster.

### 4.2 Out Of Scope For This Slice

1. Proving a real production Flink cluster is healthy.
2. Implementing a full Flink Kubernetes Operator lifecycle.
3. Building a web UI for Flink job operations.
4. Replacing the existing direct Stream Load mirror immediately.
5. Guaranteeing exactly-once semantics in production without running live Flink + Doris integration tests.

## 5. Architecture

The execution layer is a separate backend module so canvas-engine stays the API/control plane and Flink jobs stay deployable independently.

```text
MySQL canvas_db
  cdp_event_log / canvas_execution_trace
        |
        | Flink CDC / Flink SQL
        v
Doris
  canvas_ods.cdp_event_log
  canvas_ods.canvas_execution_trace
        |
        | Flink SQL transformation
        v
Doris
  canvas_dwd.cdp_user_event_fact
  canvas_dws.user_event_metric_daily
        |
        | checkpoint reporter
        v
canvas-engine
  /warehouse/realtime/pipelines/checkpoints
```

## 6. Components

### 6.1 `canvas-flink-jobs`

New Maven module under `backend/`.

Responsibilities:

- Own Flink dependencies and job entrypoints.
- Load typed configuration from environment variables or system properties.
- Load SQL assets from classpath.
- Submit configured SQL statements to Flink Table API.
- Report checkpoint/runtime evidence back to canvas-engine.

It must not depend on `canvas-engine` internals. Integration happens through HTTP and stable pipeline keys.

### 6.2 SQL Assets

SQL assets live under:

`backend/canvas-flink-jobs/src/main/resources/sql/`

Each pipeline owns one SQL file and uses placeholder replacement for environment-specific values. Placeholder validation fails before job startup if a required value is missing.

The first version keeps SQL assets explicit and reviewed instead of generating SQL dynamically from arbitrary configuration.

### 6.3 Checkpoint Reporter

The reporter posts JSON to:

`POST /warehouse/realtime/pipelines/checkpoints`

The payload includes:

- `pipelineKey`
- `checkpointId`
- `sourcePartition`
- `sourceOffset`
- `committedOffset`
- `watermarkTime`
- `checkpointTime`
- `lagMs`
- `rowCount`
- `status`
- `errorMessage`
- `reportedBy`
- `sourceSchemaVersion`
- `sinkSchemaVersion`

For the first job-side implementation, the reporter is called from job lifecycle hooks and bounded health probes rather than from custom Flink checkpoint callbacks. This gives the existing readiness gate real external evidence without introducing custom Flink operator state in the first slice.

### 6.4 Local Deployment

`docker-compose.local.yml` gains optional Flink services:

- `flink-jobmanager`
- `flink-taskmanager`

The compose file keeps Flink disabled from critical backend startup. Developers can start it explicitly with:

```bash
docker compose -f docker-compose.local.yml up -d flink-jobmanager flink-taskmanager
```

The Flink image uses an explicit version tag compatible with the Doris connector target.

## 7. Data Flow

1. CDP writes remain authoritative in MySQL.
2. Flink reads source rows through CDC or SQL connectors.
3. Flink writes ODS/DWD/DWS tables into Doris using Doris connector semantics.
4. Each job reports runtime evidence into canvas-engine using existing pipeline keys.
5. Existing readiness, incident, E2E certification, and enterprise OLAP gates consume the evidence.
6. Existing direct Stream Load mirror remains available as fallback until production cutover is proven.

## 8. Failure Handling

- Missing config fails job startup.
- Invalid SQL asset placeholders fail job startup.
- Checkpoint reporter HTTP failure is logged and retried with bounded attempts.
- A job startup failure produces a `FAIL` checkpoint report where possible.
- A job that cannot reach canvas-engine must not pretend to be healthy.
- The existing canvas-engine production gates continue to fail closed when evidence is stale or missing.

## 9. Testing

Tests do not require a live Flink cluster in this slice. Required test evidence:

- Config validation rejects missing MySQL, Doris, and checkpoint endpoint fields.
- SQL asset loader rejects missing placeholders.
- SQL assets contain the expected source/sink table names and pipeline keys.
- Checkpoint reporter serializes payloads matching `CdpWarehouseRealtimePipelineController` contract.
- Job launcher maps each known pipeline key to exactly one SQL asset.
- Maven module compiles with Java 21.

Live integration testing remains a later gate:

- Start local MySQL, Doris, Flink.
- Submit one ODS job.
- Insert a CDP event.
- Verify Doris ODS visibility.
- Verify canvas-engine receives fresh checkpoint evidence.

## 10. Acceptance Criteria

This slice is complete only when:

1. `backend/pom.xml` includes `canvas-flink-jobs`.
2. `canvas-flink-jobs` compiles and has focused unit tests.
3. Four first-slice pipeline SQL assets exist and are covered by tests.
4. Local Docker Compose includes Flink JobManager/TaskManager services.
5. A runbook explains local Flink startup and job submission.
6. Existing OLAP/readiness focused verification still passes.
7. The final status does not claim production readiness without live Flink/Doris evidence.

## 11. Follow-Up Slices

1. Production Flink Kubernetes/Helm packaging and secret wiring.
2. Live Testcontainers or environment-gated integration tests.
3. Flink checkpoint listener or metric reporter that publishes stronger checkpoint evidence.
4. Cutover policy from direct Stream Load mirror to Flink-first realtime warehouse.
5. Operator UI for job deploy/pause/resume/restart actions.
