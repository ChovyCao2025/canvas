package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseIncidentFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseIncidentApplicationService 的核心行为。
 */
class CdpWarehouseIncidentApplicationServiceTest {

    /**
     * 查询Normalizes Status Bounds Limit Sorts And Isolates Tenant列表。
     */
    @Test
    void listNormalizesStatusBoundsLimitSortsAndIsolatesTenant() {
        CdpWarehouseIncidentFacade service = new CdpWarehouseIncidentApplicationService();

        List<Map<String, Object>> tenantIncidents = service.listIncidents(9L, " open ", 500);

        assertThat(tenantIncidents).hasSize(2)
                .extracting(row -> row.get("id"))
                .containsExactly(9002L, 9001L);
        assertThat(tenantIncidents).allSatisfy(row -> assertThat(row)
                .containsEntry("tenantId", 9L)
                .containsEntry("status", "OPEN")
                .containsKeys("incidentKey", "sourceType", "sourceId", "severity", "title", "description",
                        "occurrenceCount", "firstSeenAt", "lastSeenAt", "acknowledgedBy", "acknowledgedAt",
                        "resolvedBy", "resolvedAt"));

        assertThat(service.listIncidents(8L, "OPEN", 20)).isEmpty();
        assertThat(service.listIncidents(0L, null, 0)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("tenantId", 0L));
    }

    /**
     * 执行 ackAndResolveRespectLifecycleTenantAndOperatorDefaults 对应的 CDP 业务操作。
     */
    @Test
    void ackAndResolveRespectLifecycleTenantAndOperatorDefaults() {
        CdpWarehouseIncidentFacade service = new CdpWarehouseIncidentApplicationService();

        assertThat(service.acknowledge(9L, 9001L, " alice ")).isTrue();
        assertThat(service.listIncidents(9L, "ACKNOWLEDGED", 20)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", 9001L)
                        .containsEntry("status", "ACKNOWLEDGED")
                        .containsEntry("acknowledgedBy", "alice"));

        assertThat(service.acknowledge(9L, 9001L, "bob")).isFalse();
        assertThat(service.resolve(9L, 9001L, null)).isTrue();
        assertThat(service.listIncidents(9L, "RESOLVED", 20)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", 9001L)
                        .containsEntry("resolvedBy", "operator"));

        assertThat(service.resolve(8L, 9002L, "bob")).isFalse();
        assertThat(service.resolve(9L, 9001L, "bob")).isFalse();
        assertThatThrownBy(() -> service.acknowledge(9L, 0L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");
    }
}
