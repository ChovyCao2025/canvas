package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseQualityFacade;
import org.junit.jupiter.api.Test;

class CdpWarehouseQualityApplicationServiceTest {

    @Test
    void reconcileOdsValidatesWindowNormalizesToleranceAndStoresTenantScopedChecks() {
        CdpWarehouseQualityFacade service = new CdpWarehouseQualityApplicationService();
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        Map<String, Object> result = service.reconcileOds(9L, from, to, -5L, " alice ");

        assertThat(result).containsEntry("tenantId", 9L)
                .containsEntry("checkType", "ODS_COUNT")
                .containsEntry("status", "WARN")
                .containsEntry("sourceCount", 100L)
                .containsEntry("warehouseCount", 97L)
                .containsEntry("diffCount", 3L)
                .containsEntry("thresholdValue", 0L)
                .containsEntry("createdBy", "alice")
                .containsKeys("id", "windowStart", "windowEnd", "details", "checkedAt");

        assertThat(service.recentChecks(9L, 200)).first()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", result.get("id"))
                        .containsEntry("checkType", "ODS_COUNT"));
        assertThat(service.recentChecks(8L, 20)).isEmpty();
        assertThatThrownBy(() -> service.reconcileOds(9L, to, from, 0L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must be before to");
    }

    @Test
    void aggregateLagUsesDefaultOperatorAndRecentChecksAreBoundedAndNewestFirst() {
        CdpWarehouseQualityFacade service = new CdpWarehouseQualityApplicationService();
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        Map<String, Object> lag = service.checkAggregateLag(9L, now, 30L, null);
        service.reconcileOds(9L, now.minusHours(2), now.minusHours(1), 10L, "bob");

        assertThat(lag).containsEntry("tenantId", 9L)
                .containsEntry("checkType", "AGGREGATE_LAG")
                .containsEntry("status", "WARN")
                .containsEntry("diffCount", 45L)
                .containsEntry("thresholdValue", 30L)
                .containsEntry("createdBy", "operator");
        assertThat(String.valueOf(lag.get("details"))).contains("lagMinutes=45");

        List<Map<String, Object>> recent = service.recentChecks(9L, 1);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0)).containsEntry("checkType", "ODS_COUNT");
    }
}
