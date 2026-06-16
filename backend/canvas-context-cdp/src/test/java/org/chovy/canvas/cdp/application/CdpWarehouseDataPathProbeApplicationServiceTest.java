package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseDataPathProbeApplicationService 的核心行为。
 */
class CdpWarehouseDataPathProbeApplicationServiceTest {

    /**
     * 执行 runDefaultsDirectSinkBoundsVerifyInputsAndStoresTenantScopedRecentRuns 对应的 CDP 业务操作。
     */
    @Test
    void runDefaultsDirectSinkBoundsVerifyInputsAndStoresTenantScopedRecentRuns() {
        CdpWarehouseDataPathProbeFacade service = new CdpWarehouseDataPathProbeApplicationService();

        Map<String, Object> result = service.run(9L, new CdpWarehouseDataPathProbeFacade.RunCommand(
                " ods-cert ", null, false, 50, -1, null));

        assertThat(result).containsEntry("tenantId", 9L)
                .containsEntry("probeKey", "ods-cert")
                .containsEntry("sourceMode", "DIRECT_SINK")
                .containsEntry("eventCode", "__warehouse_probe__")
                .containsEntry("strict", false)
                .containsEntry("status", "WARN")
                .containsEntry("sourceStatus", "SKIPPED")
                .containsEntry("sinkStatus", "PASS")
                .containsEntry("odsStatus", "WARN")
                .containsEntry("odsRowCount", 0L)
                .containsEntry("verifyAttempts", 10)
                .containsEntry("verifyDelayMs", 100)
                .containsKey("messageId");

        List<Map<String, Object>> recent = service.recent(9L, 200);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0)).containsEntry("id", result.get("id"));
        assertThat(service.recent(8L, 20)).isEmpty();
    }

    /**
     * 执行 mysqlCdcAliasesAndReservedEventCodeAreValidated 对应的 CDP 业务操作。
     */
    @Test
    void mysqlCdcAliasesAndReservedEventCodeAreValidated() {
        CdpWarehouseDataPathProbeFacade service = new CdpWarehouseDataPathProbeApplicationService();

        Map<String, Object> result = service.run(9L, new CdpWarehouseDataPathProbeFacade.RunCommand(
                "cdc-cert", "__warehouse_probe_custom", true, 1, 0, "flink-cdc"));

        assertThat(result).containsEntry("sourceMode", "MYSQL_CDC")
                .containsEntry("sourceStatus", "PASS")
                .containsEntry("sinkStatus", "SKIPPED")
                .containsEntry("odsStatus", "FAIL")
                .containsEntry("status", "FAIL");
        assertThat(String.valueOf(result.get("evidenceJson"))).contains("source_mysql_write");

        assertThatThrownBy(() -> service.run(9L, new CdpWarehouseDataPathProbeFacade.RunCommand(
                "bad", "customer_signup", true, 1, 0, "DIRECT_SINK")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventCode must use reserved __warehouse_probe prefix");
        assertThatThrownBy(() -> service.run(9L, new CdpWarehouseDataPathProbeFacade.RunCommand(
                "bad", null, true, 1, 0, "bad-mode")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceMode must be DIRECT_SINK or MYSQL_CDC");
    }
}
