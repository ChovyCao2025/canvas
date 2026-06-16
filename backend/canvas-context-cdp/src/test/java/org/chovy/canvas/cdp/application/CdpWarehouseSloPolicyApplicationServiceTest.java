package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseSloPolicyFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseSloPolicyApplicationService 的核心行为。
 */
class CdpWarehouseSloPolicyApplicationServiceTest {

    /**
     * 执行 upsertAppliesDefaultsNormalizesFieldsAndEffectivePrefersTenantOverride 对应的 CDP 业务操作。
     */
    @Test
    void upsertAppliesDefaultsNormalizesFieldsAndEffectivePrefersTenantOverride() {
        CdpWarehouseSloPolicyFacade service = new CdpWarehouseSloPolicyApplicationService();

        Map<String, Object> global = service.upsertPolicy(0L, Map.of(
                "policyKey", "warehouse_readiness_default",
                "displayName", "Global readiness",
                "offlineWarnRunGapMinutes", 120,
                "offlineFailRunGapMinutes", 360));
        Map<String, Object> tenant = service.upsertPolicy(9L, Map.of(
                "policyKey", "warehouse_readiness_default",
                "offlineWarnRunGapMinutes", 30,
                "offlineFailRunGapMinutes", 90,
                "offlineWarnWatermarkLagMinutes", 10,
                "offlineFailWatermarkLagMinutes", 40,
                "audienceWarnRunGapMinutes", 60,
                "audienceFailRunGapMinutes", 180,
                "status", "active",
                "ownerName", " data-platform "));

        assertThat(global).containsEntry("tenantId", 0L)
                .containsEntry("policyKey", "WAREHOUSE_READINESS_DEFAULT")
                .containsEntry("status", "ACTIVE")
                .containsEntry("audienceFailRunGapMinutes", 4320);
        assertThat(tenant).containsEntry("tenantId", 9L)
                .containsEntry("displayName", "WAREHOUSE_READINESS_DEFAULT")
                .containsEntry("ownerName", "data-platform");

        assertThat(service.effectivePolicy(9L, "warehouse_readiness_default"))
                .containsEntry("tenantId", 9L)
                .containsEntry("offlineWarnRunGapMinutes", 30)
                .containsEntry("offlineFailRunGapMinutes", 90);
        assertThat(service.effectivePolicy(8L, "warehouse_readiness_default"))
                .containsEntry("tenantId", 0L)
                .containsEntry("displayName", "Global readiness");
    }

    /**
     * 查询Filters Status Deduplicates Tenant Override And Rejects Invalid Thresholds列表。
     */
    @Test
    void listFiltersStatusDeduplicatesTenantOverrideAndRejectsInvalidThresholds() {
        CdpWarehouseSloPolicyFacade service = new CdpWarehouseSloPolicyApplicationService();

        service.upsertPolicy(0L, Map.of("policyKey", "readiness_a", "displayName", "global"));
        service.upsertPolicy(9L, Map.of("policyKey", "readiness_a", "displayName", "tenant"));
        service.upsertPolicy(9L, Map.of("policyKey", "paused", "status", "inactive"));

        List<Map<String, Object>> active = service.listPolicies(9L, " active ");

        assertThat(active).extracting(row -> row.get("policyKey"))
                .containsExactly("READINESS_A");
        assertThat(active.get(0)).containsEntry("tenantId", 9L)
                .containsEntry("displayName", "tenant");

        assertThatThrownBy(() -> service.upsertPolicy(9L, Map.of(
                "policyKey", "bad",
                "offlineWarnRunGapMinutes", 60,
                "offlineFailRunGapMinutes", 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("offline run gap warn threshold must be <= fail threshold");
    }
}
