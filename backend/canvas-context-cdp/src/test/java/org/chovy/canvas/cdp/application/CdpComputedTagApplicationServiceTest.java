package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedTagFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpComputedTagApplicationService 的核心行为。
 */
class CdpComputedTagApplicationServiceTest {

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
     * 执行 managesComputedTagLifecycleRunsLineageAndImpactWithinTenant 对应的 CDP 业务操作。
     */
    @Test
    void managesComputedTagLifecycleRunsLineageAndImpactWithinTenant() {
        CdpComputedTagFacade service = new CdpComputedTagApplicationService(CLOCK);

        Map<String, Object> tag = service.create(7L, Map.of(
                "tagCode", "vip_score",
                "displayName", "VIP Score",
                "valueType", "NUMBER",
                "computeType", "SQL",
                "expressionJson", "{\"sql\":\"select 1\"}",
                "refreshMode", "DAILY",
                "dependencies", List.of("profile_level")), "operator-1");
        Map<String, Object> page = service.list(7L);
        Map<String, Object> preview = service.preview(7L, "vip_score");
        Map<String, Object> activated = service.activate(7L, "vip_score", "operator-2");
        Map<String, Object> run = service.runNow(7L, "vip_score", "operator-3");
        Map<String, Object> runs = service.listRuns(7L, "vip_score", 10);
        Map<String, Object> lineage = service.lineage(7L, "vip_score");
        Map<String, Object> impact = service.impactCheck(7L, "vip_score", Map.of(
                "oldValueType", "NUMBER",
                "newValueType", "STRING"), "operator-4");
        Map<String, Object> paused = service.pause(7L, "vip_score", "operator-5");

        assertThat(tag).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("tagCode", "vip_score")
                .containsEntry("displayName", "VIP Score")
                .containsEntry("valueType", "NUMBER")
                .containsEntry("computeType", "SQL")
                .containsEntry("refreshMode", "DAILY")
                .containsEntry("status", "DRAFT")
                .containsEntry("updatedBy", "operator-1");
        assertThat(page).containsEntry("total", 1L);
        assertThat((List<?>) page.get("records")).hasSize(1);
        assertThat(preview).containsEntry("tagCode", "vip_score")
                .containsEntry("matchedProfileCount", 42L);
        assertThat(activated).containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-2");
        assertThat(run).containsEntry("runId", "vip_score-run-1")
                .containsEntry("triggeredBy", "operator-3")
                .containsEntry("status", "SUCCESS");
        assertThat(runs).containsEntry("total", 1L);
        assertThat(lineage).containsEntry("tagCode", "vip_score");
        assertThat(lineage.get("dependencies")).isEqualTo(List.of("profile_level"));
        assertThat(impact).containsEntry("compatible", false)
                .containsEntry("checkedBy", "operator-4");
        assertThat(paused).containsEntry("status", "PAUSED")
                .containsEntry("updatedBy", "operator-5");

        assertThat(service.list(8L)).containsEntry("total", 0L);
    }

    /**
     * 执行 validationDefaultsAndRunLimitsAreStable 对应的 CDP 业务操作。
     */
    @Test
    void validationDefaultsAndRunLimitsAreStable() {
        CdpComputedTagFacade service = new CdpComputedTagApplicationService(CLOCK);
        service.create(null, Map.of("tagCode", "churn_risk"), "");

        assertThat(service.runNow(null, "churn_risk", null))
                .containsEntry("tenantId", 0L)
                .containsEntry("triggeredBy", "system");
        assertThat(service.listRuns(null, "churn_risk", null)).containsEntry("total", 1L);
        assertThatThrownBy(() -> service.create(7L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tagCode is required");
        assertThatThrownBy(() -> service.preview(8L, "churn_risk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Computed tag not found");
        assertThatThrownBy(() -> service.impactCheck(0L, "churn_risk", Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldValueType is required");
    }
}
