# Flink Realtime Warehouse Runbook

This runbook covers the local first slice of the realtime warehouse execution layer. It gives Marketing Canvas runnable Flink SQL jobs for MySQL CDC to Doris ODS and Doris ODS/DWD/DWS transformations, plus checkpoint evidence reporting back to the `canvas-boot` runtime.

It is not a production readiness claim. Production still needs a managed Flink deployment model, real secret wiring, live Flink + Doris integration evidence, alerting, capacity sizing, and cutover policy from the direct Doris Stream Load fallback.

## Local Infrastructure

Start the dependencies from the repository root:

```bash
docker compose -f docker-compose.local.yml up -d mysql redis doris-fe doris-be
docker compose --profile flink -f docker-compose.local.yml up -d flink-jobmanager flink-taskmanager
```

Flink Web UI is exposed at `http://localhost:8082`.

The local MySQL service is configured with row binlog enabled for CDC:

- `--log-bin=mysql-bin`
- `--binlog-format=ROW`
- `--binlog-row-image=FULL`

If an old `canvas-mysql` container was created before these flags existed, recreate it before relying on CDC evidence:

```bash
docker compose -f docker-compose.local.yml up -d --force-recreate mysql
```

The local Doris FE/BE images are pinned to `apache/doris:fe-2.1.11` and
`apache/doris:be-2.1.11`. Those tags currently publish both `linux/amd64` and
`linux/arm64` manifests. Avoid reverting to the old `apache/doris:2.0.3-fe` /
`apache/doris:2.0.3-be` tags; they are not resolvable on Docker Hub and will
block live E2E verification before any warehouse logic runs.

Doris 2.1.11 entrypoints validate `FE_SERVERS` and `BE_ADDR` as IP-based
`name:ip:port` / `ip:port` values. `docker-compose.local.yml` therefore keeps
Doris FE/BE on the default compose network for service-name access from Flink,
and also attaches them to `doris-static` with fixed bootstrap IPs:

- FE: `172.30.80.2`
- BE: `172.30.80.3`

## Build The Job Jar

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-flink-jobs -DskipTests package
```

The shaded job jar includes the MySQL CDC and Doris connector dependencies. Flink runtime dependencies remain provided by the Flink cluster image.

## Required Environment

Use Docker network hostnames when submitting from inside the JobManager container:

```bash
CANVAS_FLINK_TENANT_ID=0
CANVAS_FLINK_MYSQL_URL=jdbc:mysql://mysql:3306/canvas_db?useSSL=false
CANVAS_FLINK_MYSQL_USERNAME=root
CANVAS_FLINK_MYSQL_PASSWORD=root
CANVAS_FLINK_DORIS_FE_NODES=doris-fe:8030
CANVAS_FLINK_DORIS_BE_NODES=doris-be:8040
CANVAS_FLINK_DORIS_JDBC_URL=jdbc:mysql://doris-fe:9030
CANVAS_FLINK_DORIS_USERNAME=root
CANVAS_FLINK_DORIS_PASSWORD=
CANVAS_FLINK_CHECKPOINT_ENDPOINT=http://host.docker.internal:8080/warehouse/realtime/pipelines/checkpoints
CANVAS_FLINK_INTERNAL_API_TOKEN=<same value as CANVAS_INTERNAL_API_TOKEN, if configured>
CANVAS_FLINK_REPORTED_BY=canvas-flink-jobs-local
CANVAS_FLINK_DORIS_LABEL_SUFFIX=<optional stable suffix for clean replay/cutover jobs>
CANVAS_FLINK_SOURCE_SCHEMA_VERSION=<optional registered source schema version>
CANVAS_FLINK_SINK_SCHEMA_VERSION=<optional registered sink schema version>
```

`CANVAS_FLINK_JOB_PIPELINE_KEY` can be provided as an environment variable or as `--pipeline-key=...`.
Leave `CANVAS_FLINK_DORIS_LABEL_SUFFIX` blank for normal production restarts from checkpoint/savepoint. Use a new sanitized suffix only for an intentional clean replay or disposable local verification without restoring Flink state; Doris rejects reused stream-load labels after they become visible.
Only set `CANVAS_FLINK_SOURCE_SCHEMA_VERSION` and `CANVAS_FLINK_SINK_SCHEMA_VERSION` after the versions are registered through the realtime schema API; unregistered versions intentionally downgrade checkpoint evidence to WARN.

Supported first-slice pipeline keys:

- `mysql_cdp_event_log_to_doris_ods`
- `mysql_canvas_trace_to_doris_ods`
- `doris_ods_cdp_event_to_dwd_fact`
- `doris_dwd_user_fact_to_dws_metric_daily`

## Submit A Pipeline

From the repository root after building the jar:

```bash
docker exec \
  -e CANVAS_FLINK_TENANT_ID=0 \
  -e 'CANVAS_FLINK_MYSQL_URL=jdbc:mysql://mysql:3306/canvas_db?useSSL=false' \
  -e CANVAS_FLINK_MYSQL_USERNAME=root \
  -e CANVAS_FLINK_MYSQL_PASSWORD=root \
  -e CANVAS_FLINK_DORIS_FE_NODES=doris-fe:8030 \
  -e CANVAS_FLINK_DORIS_BE_NODES=doris-be:8040 \
  -e CANVAS_FLINK_DORIS_JDBC_URL=jdbc:mysql://doris-fe:9030 \
  -e CANVAS_FLINK_DORIS_USERNAME=root \
  -e CANVAS_FLINK_DORIS_PASSWORD= \
  -e CANVAS_FLINK_CHECKPOINT_ENDPOINT=http://host.docker.internal:8080/warehouse/realtime/pipelines/checkpoints \
  -e CANVAS_FLINK_INTERNAL_API_TOKEN="${CANVAS_INTERNAL_API_TOKEN:-}" \
  -e CANVAS_FLINK_REPORTED_BY=canvas-flink-jobs-local \
  -e CANVAS_FLINK_DORIS_LABEL_SUFFIX= \
  canvas-flink-jobmanager \
  flink run -d \
    -c org.chovy.canvas.flink.CanvasFlinkJobMain \
    /opt/flink/usrlib/canvas-flink-jobs-1.0.0-SNAPSHOT.jar \
    --pipeline-key=mysql_cdp_event_log_to_doris_ods
```

Submit the other pipeline keys the same way after the prerequisite source tables are populated.

## Inspect Evidence

When `canvas-boot` is running, the job reports startup PASS/FAIL evidence to:

```text
POST /warehouse/realtime/pipelines/checkpoints
```

Read current status with the local API client or curl, adding auth headers if local security is enabled:

```bash
curl 'http://localhost:8080/warehouse/realtime/pipelines/status?recentLimit=5'
```

## Prove The CDC Data Path

After the `mysql_cdp_event_log_to_doris_ods` job is running, trigger a synthetic ODS probe through the MySQL source table:

```bash
curl -X POST \
  'http://localhost:8080/warehouse/data-path-probes/synthetic-ods/run?sourceMode=MYSQL_CDC&strict=true&verifyAttempts=10&verifyDelayMs=1000'
```

For a realtime Flink proof, the probe result must show:

- `sourceMode=MYSQL_CDC`
- `sourceStatus=PASS`
- `sinkStatus=SKIPPED`
- `odsStatus=PASS`

`sourceMode=DIRECT_SINK` is still useful for validating the direct Doris Stream Load fallback, but it does not prove the MySQL CDC to Flink to Doris path.

Before treating Flink as the primary realtime warehouse path, query the cutover gate:

```bash
curl \
  'http://localhost:8080/warehouse/realtime/cutover-readiness?targetMode=FLINK_FIRST&pipelineKey=mysql_cdp_event_log_to_doris_ods&pipelineKey=mysql_canvas_trace_to_doris_ods&pipelineKey=doris_ods_cdp_event_to_dwd_fact&pipelineKey=doris_dwd_user_fact_to_dws_metric_daily&contractKey=audience_12&certificationMode=HYBRID&maxCertificationAgeMinutes=60'
```

This endpoint requires required pipelines to be PASS and requires a fresh E2E certification whose realtime data-path proof used `MYSQL_CDC`.

## Verification

Run the focused verification script:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) scripts/verify-flink-realtime-warehouse-focus.sh
```

Passing unit tests prove the job module compiles, SQL assets render, connector packaging is configured, checkpoint payloads match the existing controller contract, and the synthetic probe can be forced through the MySQL CDC source path. They do not prove end-to-end production health without a live Flink + Doris + MySQL run.

## Live Local E2E Proof

Use the opt-in live verifier only when Docker is available and `canvas-boot` is already running locally with Doris enabled:

```bash
export CANVAS_DORIS_ENABLED=true
export CANVAS_DORIS_JDBC_URL='jdbc:mysql://localhost:9030/canvas_ods?useSSL=false&allowPublicKeyRetrieval=true'
export CANVAS_DORIS_USERNAME=root
export CANVAS_DORIS_PASSWORD=
export CANVAS_RUN_LIVE_FLINK_E2E=true
export CANVAS_LIVE_VERIFY_DERIVED_LAYERS=true
scripts/verify-flink-realtime-warehouse-live.sh
```

If `canvas-boot` is not already running, the script can start it for the duration of the verification:

```bash
export CANVAS_LIVE_START_ENGINE=true
export CANVAS_INTERNAL_API_TOKEN=local-flink-live-token
scripts/verify-flink-realtime-warehouse-live.sh
```

When `CANVAS_API_AUTH_HEADER` is not set, the script logs in with the local seeded account `admin` / `Admin@123`. Override with `CANVAS_LIVE_AUTH_USERNAME` and `CANVAS_LIVE_AUTH_PASSWORD` for non-default local data.

The script builds the Flink job jar, starts the local Docker services, verifies MySQL CDC binlog settings, applies the CDP and trace Doris DDL with `replication_num=1`, submits the `mysql_cdp_event_log_to_doris_ods` pipeline, waits for PASS checkpoint evidence, runs the synthetic probe with `sourceMode=MYSQL_CDC`, and validates the inserted probe row in `canvas_ods.cdp_event_log`.

By default (`CANVAS_LIVE_VERIFY_DERIVED_LAYERS=true`), it then submits `doris_ods_cdp_event_to_dwd_fact` and `doris_dwd_user_fact_to_dws_metric_daily`, waits for PASS evidence for each, and validates the same synthetic probe in `canvas_dwd.cdp_user_event_fact` and `canvas_dws.user_event_metric_daily`. Set `CANVAS_LIVE_VERIFY_DERIVED_LAYERS=false` only when intentionally limiting the run to the ODS CDC path.

The verifier also inserts a synthetic row into MySQL `canvas_execution_trace`, submits `mysql_canvas_trace_to_doris_ods`, waits for PASS checkpoint evidence, and validates the matching row in `canvas_ods.canvas_execution_trace`.

For local Doris defaults, the live script rewrites the DWS dynamic partition start from the production table contract of `-730` to `CANVAS_LIVE_DORIS_DWS_DYNAMIC_PARTITION_START` with a default of `-365`, because Apache Doris rejects more than 500 dynamic partitions unless FE limits are explicitly raised. Production deployments that keep 730 days online must configure Doris capacity/limits and record that as table-governance evidence.

The live verifier proves the local CDP event ODS/DWD/DWS path and the local trace ODS path. It still does not prove a target-cluster restore/cutover drill or production SLOs unless `CANVAS_LIVE_REQUIRE_CUTOVER_PASS=true` is set and the cutover gate returns `allowed=true`. A complete Flink-first production cutover still needs all required pipelines, fresh E2E certification with `sourceMode=MYSQL_CDC`, and production monitoring evidence.
