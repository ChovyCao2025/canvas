package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade;
import org.junit.jupiter.api.Test;

class CdpWarehouseEnterpriseOlapEvidenceApplicationServiceTest {

    @Test
    void recordsLatestProofAndCollectionRunsPerTenantWithDefaultTenant() {
        CdpWarehouseEnterpriseOlapEvidenceFacade service =
                new CdpWarehouseEnterpriseOlapEvidenceApplicationService();

        Map<String, Object> recorded = service.record(null, command("backup_restore", "PASS"), "alice");
        service.record(9L, command("backup_restore", "FAIL"), "bob");

        assertThat(recorded)
                .containsEntry("tenantId", 0L)
                .containsEntry("evidenceKey", "backup_restore")
                .containsEntry("source", "operator")
                .containsEntry("createdBy", "alice");

        Map<String, Object> latest = service.latest(null);
        assertThat(latest)
                .containsEntry("tenantId", 0L)
                .containsEntry("status", "FAIL");
        assertThat((List<Map<String, Object>>) latest.get("evidence"))
                .extracting(row -> row.get("evidenceKey"))
                .contains("backup_restore");

        assertThat(service.proof(null))
                .extracting(row -> row.get("key"))
                .contains("enterprise_olap:backup_restore");
        assertThat(service.latest(9L)).containsEntry("status", "FAIL");

        Map<String, Object> firstRun = service.collect(null, "MANUAL", "alice");
        Map<String, Object> secondRun = service.collect(null, "manual", "alice");
        Map<String, Object> thirdRun = service.collect(null, "MANUAL", "alice");

        assertThat(firstRun)
                .containsEntry("tenantId", 0L)
                .containsEntry("triggerType", "MANUAL")
                .containsEntry("status", "PASS")
                .containsEntry("createdBy", "alice");
        assertThat(service.collections(null, 2))
                .extracting(row -> row.get("id"))
                .containsExactly(thirdRun.get("id"), secondRun.get("id"));
    }

    @Test
    void validatesEvidenceKeyAndDefaultsCollectionLimit() {
        CdpWarehouseEnterpriseOlapEvidenceFacade service =
                new CdpWarehouseEnterpriseOlapEvidenceApplicationService();

        assertThatThrownBy(() -> service.record(0L, command(" ", "PASS"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidenceKey is required");
        service.collect(0L, "MANUAL", "alice");
        assertThat(service.collections(0L, 0)).hasSize(1);
    }

    private static CdpWarehouseEnterpriseOlapEvidenceFacade.EvidenceCommand command(String key, String status) {
        return new CdpWarehouseEnterpriseOlapEvidenceFacade.EvidenceCommand(
                key,
                status,
                "operator supplied proof",
                "2026-06-15T02:40:00",
                "2026-06-15T03:40:00",
                "{\"ok\":true}");
    }
}
