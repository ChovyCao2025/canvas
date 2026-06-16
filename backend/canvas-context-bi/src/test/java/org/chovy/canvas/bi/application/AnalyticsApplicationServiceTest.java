package org.chovy.canvas.bi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportRequest;
import org.junit.jupiter.api.Test;
/**
 * AnalyticsApplicationServiceTest 测试类。
 */
class AnalyticsApplicationServiceTest {
    /**
     * 执行 returns Deterministic Analytics Views For Legacy Route Aliases 相关处理。
     */
    @Test
    void returnsDeterministicAnalyticsViewsForLegacyRouteAliases() {
        AnalyticsApplicationService service = new AnalyticsApplicationService();

        assertThat(service.eventCounts(7L, "2026-06-01", "2026-06-07"))
                .extracting("eventCode")
                .containsExactly("purchase", "signup", "page_view");
        assertThat(service.countEvents(7L, "2026-06-01", "2026-06-07", "purchase").total())
                .isEqualTo(96);
        assertThat(service.userTimeline(7L, "user-42", "2026-06-01", "2026-06-07", 1, 2).records())
                .hasSize(2)
                .first()
                .extracting("userId", "eventCode")
                .containsExactly("user-42", "purchase");
        assertThat(service.attributeDistribution(7L, "channel", "2026-06-01", "2026-06-07"))
                .extracting("value")
                .containsExactly("wechat", "email", "organic");
        assertThat(service.funnelResult(7L, "signup-to-purchase", "2026-06-01", "2026-06-07").steps())
                .extracting("stepKey")
                .containsExactly("visit", "signup", "purchase");
        assertThat(service.alertPreview(7L, new AlertPreviewRequest(
                "purchase-spike",
                "purchase",
                "2026-06-01",
                "2026-06-07",
                80L)).triggered())
                .isTrue();
        assertThat(service.createExport(7L, new ExportRequest(
                "events",
                "purchase",
                "2026-06-01",
                "2026-06-07",
                1000,
                "analyst")).status())
                .isEqualTo("QUEUED");
        assertThat(service.exportStatus(7L, 9001L).tenantId()).isEqualTo(7L);
    }

    @Test
    void validatesRequiredAnalyticsInputs() {
        AnalyticsApplicationService service = new AnalyticsApplicationService();

        assertThatThrownBy(() -> service.eventCounts(null, "2026-06-01", "2026-06-07"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId is required");
        assertThatThrownBy(() -> service.countEvents(7L, "", "2026-06-07", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate is required");
        assertThatThrownBy(() -> service.userTimeline(7L, "", "2026-06-01", "2026-06-07", 1, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        assertThatThrownBy(() -> service.attributeDistribution(7L, " ", "2026-06-01", "2026-06-07"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attribute is required");
        assertThatThrownBy(() -> service.exportStatus(7L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exportId is required");
    }
}
