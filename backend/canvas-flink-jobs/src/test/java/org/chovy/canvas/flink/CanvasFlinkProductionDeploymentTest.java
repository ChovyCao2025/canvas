package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasFlinkProductionDeploymentTest {

    private static final Path REPO_ROOT = Path.of("../..").normalize();

    @Test
    void helmChartDefinesProductionFlinkClusterAndSecrets() throws Exception {
        String values = read("deploy/helm/canvas/values.yaml");
        String prodValues = read("deploy/helm/canvas/values-prod.yaml");
        String configMap = read("deploy/helm/canvas/templates/flink-configmap.yaml");
        String jobManager = read("deploy/helm/canvas/templates/flink-jobmanager-deployment.yaml");
        String taskManager = read("deploy/helm/canvas/templates/flink-taskmanager-deployment.yaml");
        String submitter = read("deploy/helm/canvas/templates/flink-job-submitter.yaml");
        String service = read("deploy/helm/canvas/templates/flink-service.yaml");
        String networkPolicy = read("deploy/helm/canvas/templates/flink-network-policy.yaml");

        assertThat(values)
                .contains("flink:")
                .contains("canvas-flink-jobs")
                .contains("checkpointStorageSize")
                .contains("secretName: canvas-flink-runtime")
                .contains("canvasFlinkMysqlUrl")
                .contains("canvasFlinkDorisJdbcUrl")
                .contains("canvasFlinkCheckpointEndpoint")
                .contains("canvasFlinkInternalApiToken");
        assertThat(prodValues)
                .contains("flink:")
                .contains("enabled: true")
                .contains("repository: registry.example.com/marketing-canvas/canvas-flink-jobs")
                .contains("tag: prod");
        assertThat(configMap)
                .contains("flink-conf.yaml")
                .contains("jobmanager.rpc.address")
                .contains("state.checkpoints.dir")
                .contains("state.savepoints.dir");
        assertThat(jobManager)
                .contains("kind: Deployment")
                .contains("name: {{ include \"canvas.fullname\" . }}-flink-jobmanager")
                .contains("readinessProbe")
                .contains("livenessProbe");
        assertThat(taskManager)
                .contains("kind: Deployment")
                .contains("name: {{ include \"canvas.fullname\" . }}-flink-taskmanager")
                .contains("taskmanager.numberOfTaskSlots")
                .contains("flink-checkpoints");
        assertThat(submitter)
                .contains("kind: Job")
                .contains("CANVAS_FLINK_CHECKPOINT_ENDPOINT")
                .contains("CANVAS_FLINK_INTERNAL_API_TOKEN")
                .contains("CANVAS_FLINK_DORIS_LABEL_SUFFIX")
                .contains("CANVAS_FLINK_JOB_PIPELINE_KEY")
                .contains("$root.Values.flink.jobJarPath")
                .contains("--pipeline-key=");
        assertThat(service)
                .contains("name: rest")
                .contains("port: 8081")
                .contains("name: rpc")
                .contains("port: 6123");
        assertThat(networkPolicy)
                .contains("kind: NetworkPolicy")
                .contains("canvas-engine")
                .contains("data-platform")
                .contains("port: 8081");
    }

    @Test
    void staticKubernetesManifestsMirrorHelmFlinkDeploymentContract() throws Exception {
        String configMap = read("deploy/k8s/canvas-flink-configmap.yaml");
        String jobManager = read("deploy/k8s/canvas-flink-jobmanager-deployment.yaml");
        String taskManager = read("deploy/k8s/canvas-flink-taskmanager-deployment.yaml");
        String submitter = read("deploy/k8s/canvas-flink-job-submitter.yaml");
        String service = read("deploy/k8s/canvas-flink-service.yaml");
        String networkPolicy = read("deploy/k8s/canvas-flink-network-policy.yaml");
        String secretExample = read("deploy/k8s/canvas-flink-secret.example.yaml");
        List<String> requiredPipelines = List.of(
                "mysql_cdp_event_log_to_doris_ods",
                "mysql_canvas_trace_to_doris_ods",
                "doris_ods_cdp_event_to_dwd_fact",
                "doris_dwd_user_fact_to_dws_metric_daily");

        assertThat(configMap)
                .contains("name: canvas-flink")
                .contains("state.backend: rocksdb")
                .contains("state.checkpoints.dir: file:///opt/flink/checkpoints")
                .contains("execution.checkpointing.interval: 60s");
        assertThat(jobManager)
                .contains("name: canvas-flink-jobmanager")
                .contains("registry.example.com/marketing-canvas/canvas-flink-jobs:prod")
                .contains("readinessProbe")
                .contains("livenessProbe");
        assertThat(taskManager)
                .contains("registry.example.com/marketing-canvas/canvas-flink-jobs:prod");
        assertThat(submitter)
                .contains("kind: Job")
                .contains("registry.example.com/marketing-canvas/canvas-flink-jobs:prod")
                .contains("CANVAS_FLINK_JOB_PIPELINE_KEY")
                .contains("CANVAS_FLINK_INTERNAL_API_TOKEN")
                .contains("CANVAS_FLINK_DORIS_LABEL_SUFFIX")
                .contains("canvas-flink-jobs-1.0.0-SNAPSHOT.jar");
        assertThat(submitter.split("kind: Job", -1).length - 1).isEqualTo(requiredPipelines.size());
        for (String pipeline : requiredPipelines) {
            assertThat(submitter).contains("canvas.chovy.org/pipeline-key: " + pipeline);
            assertThat(submitter).contains("value: " + pipeline);
            assertThat(submitter).contains("--pipeline-key=" + pipeline);
        }
        assertThat(taskManager)
                .contains("name: canvas-flink-taskmanager")
                .contains("replicas: 2")
                .contains("canvas-flink-checkpoints");
        assertThat(service)
                .contains("name: canvas-flink-jobmanager")
                .contains("port: 8081")
                .contains("port: 6123");
        assertThat(networkPolicy)
                .contains("name: canvas-flink")
                .contains("canvas-engine")
                .contains("data-platform");
        assertThat(secretExample)
                .contains("canvas-flink-runtime")
                .contains("canvas-flink-mysql-url")
                .contains("canvas-flink-doris-jdbc-url")
                .contains("canvas-flink-checkpoint-endpoint")
                .contains("canvas-flink-internal-api-token");
    }

    @Test
    void localDockerComposeEnablesCheckpointingForDorisTwoPhaseCommit() throws Exception {
        String compose = read("docker-compose.local.yml");
        String liveVerifier = read("scripts/verify-flink-realtime-warehouse-live.sh");

        assertThat(compose)
                .contains("flink-init:")
                .contains("entrypoint:")
                .contains("chown -R 9999:9999 /opt/flink/checkpoints /opt/flink/savepoints")
                .contains("flink-jobmanager:")
                .contains("flink-taskmanager:")
                .contains("condition: service_completed_successfully")
                .contains("state.checkpoints.dir: file:///opt/flink/checkpoints")
                .contains("execution.checkpointing.interval: 5s")
                .contains("execution.checkpointing.timeout: 2min")
                .contains("execution.checkpointing.tolerable-failed-checkpoints: 3");
        assertThat(liveVerifier)
                .contains("up --force-recreate flink-init")
                .contains("CANVAS_FLINK_DORIS_LABEL_SUFFIX")
                .contains("CANVAS_LIVE_VERIFY_DERIVED_LAYERS")
                .contains("TRACE_DORIS_DDL")
                .contains("mysql_canvas_trace_to_doris_ods")
                .contains("canvas_ods.canvas_execution_trace")
                .contains("INSERT INTO canvas_execution_trace")
                .contains("trace_execution_id=")
                .contains("WHERE tenant_id=${trace_tenant_id} AND execution_id='${trace_execution_id}'")
                .contains("doris_ods_cdp_event_to_dwd_fact")
                .contains("doris_dwd_user_fact_to_dws_metric_daily")
                .contains("canvas_dwd.cdp_user_event_fact")
                .contains("canvas_dws.user_event_metric_daily")
                .contains("probe_tenant_id=")
                .contains("WHERE tenant_id=${probe_tenant_id} AND message_id='${message_id}'");
    }

    @Test
    void liveVerifierReportsPassRuntimeProofOnlyAfterRowLevelValidation() throws Exception {
        String liveVerifier = read("scripts/verify-flink-realtime-warehouse-live.sh");

        assertThat(liveVerifier)
                .contains("report_pipeline_runtime_proof()")
                .contains("startup submission is not runtime checkpoint evidence")
                .doesNotContain("waiting for pipeline checkpoint evidence");
        assertThat(liveVerifier.indexOf("ods_rows=\"$(doris_exec"))
                .isLessThan(liveVerifier.indexOf("report_pipeline_runtime_proof \"${PIPELINE_KEY}\""));
        assertThat(liveVerifier.indexOf("dwd_rows=\"$(doris_exec"))
                .isLessThan(liveVerifier.indexOf("report_pipeline_runtime_proof \"${PIPELINE_DWD}\""));
        assertThat(liveVerifier.indexOf("dws_count_value=\"$(doris_exec"))
                .isLessThan(liveVerifier.indexOf("report_pipeline_runtime_proof \"${PIPELINE_DWS}\""));
        assertThat(liveVerifier.indexOf("trace_ods_rows=\"$(doris_exec"))
                .isLessThan(liveVerifier.indexOf("report_pipeline_runtime_proof \"${PIPELINE_TRACE}\""));
    }

    @Test
    void productionRunbookAndAlertsCoverFlinkOperationalGates() throws Exception {
        String runbook = read("docs/runbooks/flink-production-deployment.md");
        String alerts = read("deploy/observability/prometheus/canvas-flink-alert-rules.yml");

        assertThat(runbook)
                .contains("Production Flink Realtime Warehouse")
                .contains("canvas-flink-runtime")
                .contains("kubectl apply")
                .contains("checkpoint")
                .contains("savepoint")
                .contains("/warehouse/realtime/pipelines/status")
                .contains("not a production readiness claim without live evidence");
        assertThat(alerts)
                .contains("CanvasFlinkJobManagerDown")
                .contains("CanvasFlinkTaskManagerMissing")
                .contains("CanvasFlinkCheckpointFailures")
                .contains("CanvasFlinkBackpressureHigh")
                .contains("CanvasFlinkCheckpointDurationHigh");
    }

    @Test
    void productionPreflightScriptEnforcesFlinkDeploymentContract() throws Exception {
        String script = read("scripts/verify-flink-production-deployment.sh");
        String runbook = read("docs/runbooks/flink-production-deployment.md");

        assertThat(script)
                .startsWith("#!/usr/bin/env bash")
                .contains("set -euo pipefail")
                .contains("registry.example.com/marketing-canvas/canvas-flink-jobs:prod")
                .contains("mysql_cdp_event_log_to_doris_ods")
                .contains("mysql_canvas_trace_to_doris_ods")
                .contains("doris_ods_cdp_event_to_dwd_fact")
                .contains("doris_dwd_user_fact_to_dws_metric_daily")
                .contains("canvas-flink-mysql-url")
                .contains("canvas-flink-doris-jdbc-url")
                .contains("canvas-flink-checkpoint-endpoint")
                .contains("canvas-flink-internal-api-token")
                .contains("CanvasFlinkJobManagerDown")
                .contains("helm template")
                .contains("promtool check rules")
                .contains("reject_bare_flink_image")
                .contains("require_static_submitter_jobs")
                .contains("require_cutover_gate_pipelines");
        assertThat(runbook)
                .contains("scripts/verify-flink-production-deployment.sh")
                .contains("This preflight is static and non-destructive");
        assertThat(runScriptSyntax("scripts/verify-flink-production-deployment.sh")).isEqualTo(0);
    }

    private String read(String relativePath) throws IOException {
        Path path = REPO_ROOT.resolve(relativePath).normalize();
        assertThat(path).as(relativePath).exists();
        return Files.readString(path);
    }

    private int runScriptSyntax(String relativePath) throws IOException, InterruptedException {
        Path path = REPO_ROOT.resolve(relativePath).normalize().toAbsolutePath();
        return new ProcessBuilder("/bin/bash", "-n", path.toString())
                .directory(REPO_ROOT.toFile())
                .start()
                .waitFor();
    }
}
