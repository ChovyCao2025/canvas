package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AudienceFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证AudienceApplicationService的关键兼容行为。
 */
class AudienceApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证 audiences are tenant scoped and support preview ready compute and stats 场景的兼容行为。
     */
    @Test
    void audiencesAreTenantScopedAndSupportPreviewReadyComputeAndStats() {
        AudienceFacade service = new AudienceApplicationService(CLOCK);

        Map<String, Object> created = service.create(7L, Map.of(
                "name", "High value buyers",
                "dataSourceType", "cdp_profile",
                "ruleJson", "{\"segment\":\"vip\"}",
                "enabled", true), "operator-1");
        Map<String, Object> preview = service.preview(7L, Map.of(
                "dataSourceType", "cdp_profile",
                "ruleJson", "{\"segment\":\"vip\"}",
                "sampleLimit", 2));
        Map<String, Object> compute = service.compute(7L, 1L, Map.of(), "operator-2");
        Map<String, Object> stat = service.stat(7L, 1L);

        assertThat(created).containsEntry("tenantId", 7L)
                .containsEntry("id", 1L)
                .containsEntry("name", "High value buyers")
                .containsEntry("status", "READY")
                .containsEntry("createdBy", "operator-1");
        assertThat(preview).containsEntry("total", 3);
        @SuppressWarnings("unchecked")
        List<String> sampleUserIds = (List<String>) preview.get("sampleUserIds");
        assertThat(sampleUserIds).containsExactly("vip-user-1", "vip-user-2");
        assertThat(compute).containsEntry("taskId", "audience-compute-1-1")
                .containsEntry("status", "QUEUED")
                .containsEntry("operator", "operator-2");
        assertThat(stat).containsEntry("audienceId", 1L)
                .containsEntry("status", "READY")
                .containsEntry("memberCount", 3L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) service.list(7L, 1, 20).get("records");
        assertThat(records).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        List<Map<String, Object>> ready = service.ready(7L);
        assertThat(ready).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        assertThat(service.get(8L, 1L)).isEmpty();
    }

    /**
     * 验证 update delete defaults and validation follow compatibility rules 场景的兼容行为。
     */
    @Test
    void updateDeleteDefaultsAndValidationFollowCompatibilityRules() {
        AudienceFacade service = new AudienceApplicationService(CLOCK);

        Map<String, Object> created = service.create(null, Map.of("name", "Draft", "enabled", false), "");
        Map<String, Object> updated = service.update(null, 1L, Map.of("name", "Activated", "enabled", true),
                "audience-admin");

        assertThat(created).containsEntry("tenantId", 0L)
                .containsEntry("createdBy", "system")
                .containsEntry("status", "DISABLED");
        assertThat(updated).containsEntry("tenantId", 0L)
                .containsEntry("name", "Activated")
                .containsEntry("enabled", true)
                .containsEntry("updatedBy", "audience-admin")
                .containsEntry("status", "READY");
        assertThat(service.delete(null, 1L)).containsEntry("deleted", true)
                .containsEntry("audienceId", 1L);
        assertThat(service.list(null, 1, 20).get("records")).asList().isEmpty();

        assertThatThrownBy(() -> service.sourceFields("unsupported"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported CDP audience source");
        assertThatThrownBy(() -> service.update(null, 99L, Map.of("name", "missing"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Audience not found");
    }
}
