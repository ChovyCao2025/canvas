# Production Flink Realtime Warehouse Runbook

This runbook covers the production deployment assets for the Marketing Canvas realtime warehouse Flink layer.

This is not a production readiness claim without live evidence. Production readiness still requires a real cluster deployment, successful job submissions, fresh checkpoint evidence from the `canvas-boot` runtime behind the stable `canvas-engine` service name, Doris row-level validation, monitoring targets scraped by Prometheus, and an approved cutover policy from the direct Doris Stream Load fallback.

## Artifacts

- Helm chart values and templates: `deploy/helm/canvas/values*.yaml`, `deploy/helm/canvas/templates/flink-*.yaml`
- Static Kubernetes manifests: `deploy/k8s/canvas-flink-*.yaml`
- Runtime secret contract: `canvas-flink-runtime`
- Prometheus alerts: `deploy/observability/prometheus/canvas-flink-alert-rules.yml`
- Job jar: `backend/canvas-flink-jobs/target/canvas-flink-jobs-1.0.0-SNAPSHOT.jar`

## Required Secret

Create `canvas-flink-runtime` in the `canvas` namespace. Use the example file as a key contract, not as production secret material:

```bash
kubectl apply -f deploy/k8s/canvas-flink-secret.example.yaml --dry-run=server
```

Required keys:

- `canvas-flink-mysql-url`
- `canvas-flink-mysql-username`
- `canvas-flink-mysql-password`
- `canvas-flink-doris-fe-nodes`
- `canvas-flink-doris-be-nodes`
- `canvas-flink-doris-jdbc-url`
- `canvas-flink-doris-username`
- `canvas-flink-doris-password`
- `canvas-flink-checkpoint-endpoint`
- `canvas-flink-internal-api-token`

`canvas-flink-internal-api-token` must match `CANVAS_INTERNAL_API_TOKEN` on the `canvas-boot` runtime behind the stable `canvas-engine` service name. The checkpoint endpoint is intentionally routed through the internal API token filter, not through operator JWT auth.

## Build And Publish Job Image

The static manifests assume the Flink image contains:

```text
/opt/flink/usrlib/canvas-flink-jobs-1.0.0-SNAPSHOT.jar
```

Build the jar first:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-flink-jobs package
```

For production, publish a derived Flink image that copies the jar into `/opt/flink/usrlib/`, then set the Helm value `flink.image.repository` and `flink.image.tag`.

## Static Kubernetes Deployment

Before applying cluster resources, run the static production preflight from the repository root:

```bash
scripts/verify-flink-production-deployment.sh
```

This preflight is static and non-destructive. It checks that Helm and static Kubernetes manifests use the production `canvas-flink-jobs:prod` image instead of a bare Flink runtime image, verifies all four submitter Jobs and cutover pipeline keys, validates the `canvas-flink-runtime` Secret key contract, and checks Prometheus alert coverage. If `helm` or `promtool` are installed locally, it also runs `helm template` and `promtool check rules`; otherwise those optional checks are skipped with an explicit message.

Apply the cluster resources after the preflight passes and after replacing the example secret with real secret material:

```bash
kubectl apply -f deploy/k8s/canvas-flink-configmap.yaml
kubectl apply -f deploy/k8s/canvas-flink-pvc.yaml
kubectl apply -f deploy/k8s/canvas-flink-service.yaml
kubectl apply -f deploy/k8s/canvas-flink-jobmanager-deployment.yaml
kubectl apply -f deploy/k8s/canvas-flink-taskmanager-deployment.yaml
kubectl apply -f deploy/k8s/canvas-flink-network-policy.yaml
kubectl apply -f deploy/k8s/canvas-flink-job-submitter.yaml
```

## Helm Deployment

Render with the production values after setting the image and secret outside the chart:

```bash
helm upgrade --install canvas deploy/helm/canvas \
  --namespace canvas \
  --create-namespace \
  -f deploy/helm/canvas/values-prod.yaml \
  --set flink.image.repository=registry.example.com/marketing-canvas/canvas-flink-jobs \
  --set flink.image.tag=prod
```

## Operational Checks

Check Flink pods and REST service:

```bash
kubectl -n canvas get pods -l app.kubernetes.io/name=canvas-flink
kubectl -n canvas port-forward svc/canvas-flink-jobmanager 8081:8081
```

Check submitted jobs in Flink:

```bash
kubectl -n canvas exec deploy/canvas-flink-jobmanager -- flink list -m canvas-flink-jobmanager:8081
```

Check Canvas checkpoint status:

```bash
curl 'http://canvas-engine.canvas.svc.cluster.local:8080/warehouse/realtime/pipelines/status?recentLimit=5'
```

Run the source-table synthetic data-path proof after the MySQL CDC pipeline is active:

```bash
curl -X POST \
  'http://canvas-engine.canvas.svc.cluster.local:8080/warehouse/data-path-probes/synthetic-ods/run?sourceMode=MYSQL_CDC&strict=true&verifyAttempts=10&verifyDelayMs=1000'
```

The realtime warehouse is not accepted until the checkpoint endpoint shows fresh PASS evidence for the submitted pipelines, the synthetic probe returns `sourceMode=MYSQL_CDC`, `sourceStatus=PASS`, `sinkStatus=SKIPPED`, and `odsStatus=PASS`, Doris inspection confirms the expected CDP event ODS/DWD/DWS rows, and a trace probe row is visible in `canvas_ods.canvas_execution_trace`.

For local pre-production rehearsal, run `scripts/verify-flink-realtime-warehouse-live.sh` with `CANVAS_RUN_LIVE_FLINK_E2E=true`. By default it verifies the CDP event ODS/DWD/DWS path and the trace ODS path; keep `CANVAS_LIVE_VERIFY_DERIVED_LAYERS=true` unless intentionally limiting the rehearsal to the CDP event ODS CDC path. That script is a local integration proof only; do not use it as production acceptance evidence unless the same checks are repeated against the target cluster and recorded with production endpoints, job IDs, metrics, and certification run IDs.

For persisted promotion evidence, run certification with realtime and data-path requirements enabled:

```bash
curl -X POST \
  'http://canvas-engine.canvas.svc.cluster.local:8080/warehouse/e2e-certification/runs?requirePhysical=true&requireRealtime=true&requireDataPathProof=true&contractKey=audience_12'
```

The saved certification run must be PASS, and its `dataPathProofJson` must show `sourceMode=MYSQL_CDC`. A PASS using `sourceMode=DIRECT_SINK` proves only the direct Doris Stream Load fallback.

Check the final cutover gate before changing traffic policy away from the direct Stream Load fallback:

```bash
curl \
  'http://canvas-engine.canvas.svc.cluster.local:8080/warehouse/realtime/cutover-readiness?targetMode=FLINK_FIRST&pipelineKey=mysql_cdp_event_log_to_doris_ods&pipelineKey=mysql_canvas_trace_to_doris_ods&pipelineKey=doris_ods_cdp_event_to_dwd_fact&pipelineKey=doris_dwd_user_fact_to_dws_metric_daily&contractKey=audience_12&certificationMode=HYBRID&maxCertificationAgeMinutes=60'
```

The response must return `status=PASS` and `allowed=true`. Any `FAIL` or `WARN` gate means production remains on the direct Stream Load fallback or hybrid diagnostic mode until the failed evidence is repaired.

## Savepoint And Restore

Before disruptive upgrades, trigger a savepoint:

```bash
kubectl -n canvas exec deploy/canvas-flink-jobmanager -- \
  flink savepoint <job-id> file:///opt/flink/savepoints
```

Restore with the same job jar and the generated savepoint path:

```bash
kubectl -n canvas exec deploy/canvas-flink-jobmanager -- \
  flink run -d \
    -m canvas-flink-jobmanager:8081 \
    -s file:///opt/flink/savepoints/<savepoint-id> \
    -c org.chovy.canvas.flink.CanvasFlinkJobMain \
    /opt/flink/usrlib/canvas-flink-jobs-1.0.0-SNAPSHOT.jar \
    --pipeline-key=mysql_cdp_event_log_to_doris_ods
```

## Monitoring Gates

Load `deploy/observability/prometheus/canvas-flink-alert-rules.yml` into Prometheus. At minimum, production acceptance requires:

- JobManager target up.
- At least one TaskManager registered.
- No checkpoint failures.
- Checkpoint duration below the alert threshold.
- Backpressure below the alert threshold.
- Fresh Canvas checkpoint evidence for each active pipeline.
