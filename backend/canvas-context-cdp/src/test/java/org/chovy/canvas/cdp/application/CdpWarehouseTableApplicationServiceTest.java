package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseTableFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseTableApplicationService 的核心行为。
 */
class CdpWarehouseTableApplicationServiceTest {

    /**
     * 执行 fixed 对应的 CDP 业务操作。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T04:00:00Z"),
            /**
             * 执行 of 对应的 CDP 业务操作。
             */
            ZoneId.of("Asia/Shanghai"));

    /**
     * 执行 managesContractsInspectionsRemediationAndIncidentScanWithinTenant 对应的 CDP 业务操作。
     */
    @Test
    void managesContractsInspectionsRemediationAndIncidentScanWithinTenant() {
        CdpWarehouseTableFacade service = new CdpWarehouseTableApplicationService(CLOCK);

        Map<String, Object> contract = service.upsertContract(7L, Map.of(
                "tableKey", "dwd_user_profile",
                "datasetKey", "user-profile",
                "layer", "DWD",
                "lifecycleStatus", "ACTIVE",
                "ownerName", "data-owner"), "operator-1");
        Map<String, Object> page = service.listContracts(7L, "DWD", "ACTIVE");
        Map<String, Object> inspection = service.inspectContract(7L, "dwd_user_profile", "operator-2", false);
        Map<String, Object> liveInspection = service.inspectAll(7L, "operator-3", true);
        Map<String, Object> plan = service.planRemediation(7L, "dwd_user_profile", true, "operator-4");
        Map<String, Object> planAll = service.planAllRemediation(7L, false, "operator-5");
        Map<String, Object> scan = service.scanIncidents(7L, true, "operator-6");

        assertThat(contract).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("tableKey", "dwd_user_profile")
                .containsEntry("datasetKey", "user-profile")
                .containsEntry("layer", "DWD")
                .containsEntry("lifecycleStatus", "ACTIVE")
                .containsEntry("updatedBy", "operator-1");
        assertThat(page).containsEntry("total", 1L);
        assertThat((List<?>) page.get("records")).hasSize(1);
        assertThat(inspection).containsEntry("tableKey", "dwd_user_profile")
                .containsEntry("live", false)
                .containsEntry("inspectedBy", "operator-2")
                .containsEntry("issueCount", 1);
        assertThat(liveInspection).containsEntry("live", true)
                .containsEntry("tableCount", 1)
                .containsEntry("inspectedBy", "operator-3");
        assertThat(plan).containsEntry("tableKey", "dwd_user_profile")
                .containsEntry("live", true)
                .containsEntry("plannedBy", "operator-4");
        assertThat(planAll).containsEntry("tableCount", 1)
                .containsEntry("plannedBy", "operator-5");
        assertThat(scan).containsEntry("tenantId", 7L)
                .containsEntry("live", true)
                .containsEntry("scannedBy", "operator-6")
                .containsEntry("incidentCount", 1);

        assertThat(service.listContracts(8L, null, null)).containsEntry("total", 0L);
    }

    /**
     * 执行 validationIsTenantScopedAndDefaultsAreStable 对应的 CDP 业务操作。
     */
    @Test
    void validationIsTenantScopedAndDefaultsAreStable() {
        CdpWarehouseTableFacade service = new CdpWarehouseTableApplicationService(CLOCK);
        service.upsertContract(null, Map.of("tableKey", "ads_order_summary"), "");

        assertThat(service.inspectContract(null, "ads_order_summary", null, true))
                .containsEntry("inspectedBy", "system")
                .containsEntry("tenantId", 0L);
        assertThatThrownBy(() -> service.upsertContract(7L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableKey is required");
        assertThatThrownBy(() -> service.inspectContract(8L, "ads_order_summary", "operator-1", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Warehouse table contract not found");
    }
}
