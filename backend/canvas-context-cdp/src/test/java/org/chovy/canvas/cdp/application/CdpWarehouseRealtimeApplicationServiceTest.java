package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseRealtimeApplicationService 的核心行为。
 */
class CdpWarehouseRealtimeApplicationServiceTest {

    /**
     * 执行 managesRealtimeSchemasPipelinesJobsAndProbeTargetsPerTenant 对应的 CDP 业务操作。
     */
    @Test
    void managesRealtimeSchemasPipelinesJobsAndProbeTargetsPerTenant() {
        CdpWarehouseRealtimeApplicationService service = new CdpWarehouseRealtimeApplicationService();

        Map<String, Object> schema = service.registerSchema(42L, Map.of(
                "pipelineKey", "orders",
                "schemaRole", "source",
                "schemaVersion", "v1",
                "schemaJson", "{\"type\":\"object\"}"),
                "ada");
        assertThat(schema)
                .containsEntry("tenantId", 42L)
                .containsEntry("schemaKey", "schema-1")
                .containsEntry("schemaRole", "SOURCE")
                .containsEntry("active", true)
                .containsEntry("updatedBy", "ada");
        assertThat(service.latestSchema(42L, "orders", "source")).containsEntry("schemaVersion", "v1");

        Map<String, Object> contract = service.upsertPipelineContract(42L, Map.of(
                "pipelineKey", "orders",
                "displayName", "Orders Stream",
                "lifecycleStatus", "active",
                "maxLagMs", 5000L),
                "ada");
        assertThat(contract)
                .containsEntry("pipelineKey", "orders")
                .containsEntry("lifecycleStatus", "ACTIVE")
                .containsEntry("updatedBy", "ada");

        Map<String, Object> checkpoint = service.reportCheckpoint(42L, Map.of(
                "pipelineKey", "orders",
                "checkpointId", "cp-1",
                "lagMs", 120L,
                "rowCount", 300L,
                "status", "passed"),
                "flink");
        assertThat(checkpoint).containsEntry("checkpointKey", "checkpoint-1").containsEntry("status", "PASSED");
        assertThat(service.pipelineStatus(42L, 5)).containsEntry("pipelineCount", 1).containsEntry("checkpointCount", 1);
        assertThat(service.realtimeStatus(42L)).containsEntry("tenantId", 42L).containsEntry("pipelineCount", 1);

        Map<String, Object> job = service.heartbeat(42L, Map.of(
                "pipelineKey", "orders",
                "jobKey", "job-orders",
                "runtimeStatus", "running"),
                "flink");
        assertThat(job).containsEntry("jobKey", "job-orders").containsEntry("runtimeStatus", "RUNNING");

        Map<String, Object> action = service.requestAction(42L, Map.of(
                "pipelineKey", "orders",
                "jobKey", "job-orders",
                "action", "restart"),
                "ada");
        assertThat(action).containsEntry("actionId", 1L).containsEntry("status", "PENDING");
        assertThat(service.pendingActions(42L, "orders", "job-orders", 10)).hasSize(1);
        assertThat(service.acknowledgeAction(42L, 1L)).containsEntry("status", "ACKNOWLEDGED");
        assertThat(service.completeAction(42L, 1L, "done", "restarted")).containsEntry("status", "DONE");

        Map<String, Object> target = service.upsertProbeTarget(42L, Map.of(
                "pipelineKey", "orders",
                "jobKey", "job-orders",
                "engineType", "flink",
                "endpointUrl", "https://probe.example/jobs/orders"),
                "ada");
        assertThat(target).containsEntry("targetId", 1L).containsEntry("enabled", true);
        assertThat(service.setProbeTargetEnabled(42L, 1L, false)).containsEntry("enabled", false);
        assertThat(service.listProbeTargets(42L, true, 10)).hasSize(1);
    }

    /**
     * 校验s Required Keys And Defaults Actor。
     */
    @Test
    void validatesRequiredKeysAndDefaultsActor() {
        CdpWarehouseRealtimeApplicationService service = new CdpWarehouseRealtimeApplicationService();

        assertThatThrownBy(() -> service.registerSchema(7L, Map.of("schemaRole", "source"), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pipelineKey is required");

        Map<String, Object> contract = service.upsertPipelineContract(null, Map.of("pipelineKey", "profiles"), "");
        assertThat(contract).containsEntry("tenantId", 0L).containsEntry("updatedBy", "system");
    }
}
