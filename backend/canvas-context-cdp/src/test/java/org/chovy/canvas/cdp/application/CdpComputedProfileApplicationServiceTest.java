package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedProfileFacade;
import org.junit.jupiter.api.Test;

class CdpComputedProfileApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T04:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void managesComputedProfileLifecycleRunsAndChangesWithinTenant() {
        CdpComputedProfileFacade service = new CdpComputedProfileApplicationService(CLOCK);

        Map<String, Object> attribute = service.create(7L, Map.of(
                "attributeCode", "ltv_band",
                "attributeName", "LTV Band",
                "valueType", "STRING",
                "expression", "case when ltv > 100 then 'high' end"), "operator-1");
        Map<String, Object> page = service.list(7L);
        Map<String, Object> preview = service.preview(7L, 1L);
        Map<String, Object> activated = service.activate(7L, 1L, "operator-2");
        Map<String, Object> run = service.runNow(7L, 1L, "operator-3");
        Map<String, Object> runs = service.listRuns(7L, 1L, 10);
        Map<String, Object> changes = service.listChanges(7L, 1L, "user-1", 10);
        Map<String, Object> paused = service.pause(7L, 1L, "operator-4");

        assertThat(attribute).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("attributeCode", "ltv_band")
                .containsEntry("attributeName", "LTV Band")
                .containsEntry("valueType", "STRING")
                .containsEntry("status", "DRAFT")
                .containsEntry("updatedBy", "operator-1");
        assertThat(page).containsEntry("total", 1L);
        assertThat((List<?>) page.get("records")).hasSize(1);
        assertThat(preview).containsEntry("id", 1L)
                .containsEntry("sampleProfileCount", 3L);
        assertThat(activated).containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-2");
        assertThat(run).containsEntry("runId", "computed-profile-1-run-1")
                .containsEntry("triggeredBy", "operator-3")
                .containsEntry("status", "SUCCESS");
        assertThat(runs).containsEntry("total", 1L);
        assertThat(changes).containsEntry("total", 1L)
                .containsEntry("userId", "user-1");
        assertThat(paused).containsEntry("status", "PAUSED")
                .containsEntry("updatedBy", "operator-4");

        assertThat(service.list(8L)).containsEntry("total", 0L);
    }

    @Test
    void validationDefaultsAndLimitsAreStable() {
        CdpComputedProfileFacade service = new CdpComputedProfileApplicationService(CLOCK);
        service.create(null, Map.of("attributeCode", "churn_score"), "");

        assertThat(service.runNow(null, 1L, null))
                .containsEntry("tenantId", 0L)
                .containsEntry("triggeredBy", "system");
        assertThat(service.listRuns(null, 1L, null)).containsEntry("limit", 100);
        assertThat(service.listChanges(null, 1L, null, 800)).containsEntry("limit", 500);
        assertThatThrownBy(() -> service.create(7L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attributeCode is required");
        assertThatThrownBy(() -> service.preview(8L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Computed profile attribute not found");
        assertThatThrownBy(() -> service.activate(7L, 0L, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id must be positive");
    }
}
