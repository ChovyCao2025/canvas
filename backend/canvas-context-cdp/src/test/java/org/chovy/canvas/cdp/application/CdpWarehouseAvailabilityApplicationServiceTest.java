package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseAvailabilityApplicationService 的核心行为。
 */
class CdpWarehouseAvailabilityApplicationServiceTest {

    /**
     * 执行 recordsAssetsContractsAndEvaluatesAvailabilityDeterministically 对应的 CDP 业务操作。
     */
    @Test
    void recordsAssetsContractsAndEvaluatesAvailabilityDeterministically() {
        CdpWarehouseAvailabilityApplicationService service = new CdpWarehouseAvailabilityApplicationService();

        Map<String, Object> asset = service.recordAssetAvailability(9L, Map.of(
                "assetType", "table",
                "assetKey", "dwd_orders",
                "mode", "HYBRID",
                "status", "AVAILABLE",
                "freshnessLagMinutes", 8), "alice");
        Map<String, Object> contract = service.upsertContract(9L, Map.of(
                "contractKey", "contract-orders",
                "consumerType", "bi",
                "assetKey", "dwd_orders",
                "maxFreshnessLagMinutes", 15,
                "status", "ACTIVE"), "alice");
        Map<String, Object> evaluation = service.evaluateContract(9L, "contract-orders", null, null);
        Map<String, Object> availability = service.availability(9L, null, null, "HYBRID");

        assertThat(asset).containsEntry("tenantId", 9L)
                .containsEntry("assetKey", "dwd_orders")
                .containsEntry("updatedBy", "alice");
        assertThat(contract).containsEntry("contractKey", "contract-orders")
                .containsEntry("updatedBy", "alice");
        assertThat(evaluation).containsEntry("contractKey", "contract-orders")
                .containsEntry("decision", "AVAILABLE");
        assertThat(availability).containsEntry("tenantId", 9L)
                .containsEntry("mode", "HYBRID")
                .containsEntry("decision", "AVAILABLE");
        assertThat(service.listAssetAvailability(9L, "table", "dwd_orders", "HYBRID", 10)).hasSize(1);
        assertThat(service.listContracts(9L, "bi", "ACTIVE")).hasSize(1);
    }

    /**
     * 执行 rejectsMissingRequiredCompatibilityKeys 对应的 CDP 业务操作。
     */
    @Test
    void rejectsMissingRequiredCompatibilityKeys() {
        CdpWarehouseAvailabilityApplicationService service = new CdpWarehouseAvailabilityApplicationService();

        assertThatThrownBy(() -> service.recordAssetAvailability(9L, Map.of("assetType", "table"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("assetKey is required");
        assertThatThrownBy(() -> service.upsertContract(9L, Map.of("consumerType", "bi"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contractKey is required");
        assertThatThrownBy(() -> service.evaluateContract(9L, "missing-contract", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contractKey not found");
    }

    /**
     * 执行 scanResultsUseTenantAndActorInputs 对应的 CDP 业务操作。
     */
    @Test
    void scanResultsUseTenantAndActorInputs() {
        CdpWarehouseAvailabilityApplicationService service = new CdpWarehouseAvailabilityApplicationService();

        Map<String, Object> warehouseScan = service.scanWarehouseIncidents(9L, Map.of("mode", "STRICT"), "alice");
        Map<String, Object> consumerScan = service.scanConsumerIncidents(9L, Map.of("consumerType", "bi"), "alice");

        assertThat(warehouseScan).containsEntry("tenantId", 9L)
                .containsEntry("mode", "STRICT")
                .containsEntry("operator", "alice")
                .containsEntry("incidentCount", 0);
        assertThat(consumerScan).containsEntry("tenantId", 9L)
                .containsEntry("consumerType", "bi")
                .containsEntry("operator", "alice")
                .containsEntry("incidentCount", 0);
    }
}
