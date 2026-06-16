package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseE2eCertificationApplicationService 的核心行为。
 */
class CdpWarehouseE2eCertificationApplicationServiceTest {

    /**
     * 执行 certifiesWarehouseEvidenceAndCreatesQueryableRuns 对应的 CDP 业务操作。
     */
    @Test
    void certifiesWarehouseEvidenceAndCreatesQueryableRuns() {
        CdpWarehouseE2eCertificationApplicationService service = new CdpWarehouseE2eCertificationApplicationService();

        Map<String, Object> certification = service.certify(9L, "2026-06-15T01:00:00", "2026-06-15T02:00:00",
                "hybrid", List.of("orders", "audience"), true, true, true);
        Map<String, Object> run = service.run(9L, "2026-06-15T01:00:00", "2026-06-15T02:00:00",
                "HYBRID", List.of("orders", "audience"), true, true, true, "qa");
        Map<String, Object> gate = service.gate(9L, "HYBRID", List.of("orders"), true, true, true, 60L);

        assertThat(certification).containsEntry("tenantId", 9L)
                .containsEntry("status", "PASS")
                .containsEntry("mode", "HYBRID")
                .containsKeys("evidence", "productionReadiness", "liveTableInspection",
                        "realtimePipelineStatus", "realtimeJobStatus", "dataPathProof");
        assertThat((List<Map<String, Object>>) certification.get("evidence"))
                .extracting(item -> item.get("key"))
                .contains("production_readiness", "live_table_contracts", "realtime_pipeline_status",
                        "data_path_proof");

        assertThat(run).containsEntry("tenantId", 9L)
                .containsEntry("status", "PASS")
                .containsEntry("requestedBy", "qa")
                .containsEntry("requirePhysical", true)
                .containsEntry("requireRealtime", true)
                .containsEntry("requireDataPathProof", true)
                .containsKeys("contractKeysJson", "evidenceJson", "productionReadinessJson",
                        "liveTableInspectionJson", "realtimePipelineStatusJson", "realtimeJobStatusJson",
                        "dataPathProofJson");

        assertThat(gate).containsEntry("tenantId", 9L)
                .containsEntry("status", "PASS")
                .containsEntry("matchedRunStatus", "PASS")
                .containsEntry("mode", "HYBRID");
        assertThat(service.recent(9L, 20)).extracting(item -> item.get("id")).containsExactly(run.get("id"));
        assertThat(service.get(9L, (Long) run.get("id"))).containsEntry("id", run.get("id"));
    }

    /**
     * 返回默认的s Inputs And Rejects Missing Runs。
     */
    @Test
    void defaultsInputsAndRejectsMissingRuns() {
        CdpWarehouseE2eCertificationApplicationService service = new CdpWarehouseE2eCertificationApplicationService();

        Map<String, Object> run = service.run(null, null, null, null,
                List.of("orders", "orders", " "), true, true, true, null);

        assertThat(run).containsEntry("tenantId", 0L)
                .containsEntry("mode", "HYBRID")
                .containsEntry("requestedBy", "system")
                .containsEntry("contractKeysJson", "[\"orders\"]");
        assertThat(service.recent(null, 0)).hasSize(1);
        assertThatThrownBy(() -> service.get(0L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("certification run not found: 999");
    }
}
