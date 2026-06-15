package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasStatsFacade;
import org.junit.jupiter.api.Test;

class CanvasStatsApplicationServiceTest {

    @Test
    void returnsDeterministicCompactStatsShapes() {
        CanvasStatsFacade service = new CanvasStatsApplicationService();

        assertThat(service.trace(42L, "exec-42"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("nodeId", "start")
                        .containsEntry("nodeType", "TRIGGER")
                        .containsEntry("nodeName", "Entry")
                        .containsEntry("status", 2)
                        .containsEntry("durationMs", 0L));

        assertThat(service.recentExecutions(42L, 100))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("id", "exec-42")
                        .containsEntry("triggerType", "MANUAL")
                        .containsEntry("status", 2)
                        .containsEntry("userId", "system"));

        assertThat(service.stats(42L, 7, null, null))
                .containsEntry("total", 1L)
                .containsEntry("success", 1L)
                .containsEntry("failed", 0L)
                .containsEntry("paused", 0L)
                .containsEntry("successRate", "100.0%")
                .containsEntry("uniqueUsers", 1L);

        assertThat(service.funnel(42L))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("nodeId", "start")
                        .containsEntry("totalEntered", 1L)
                        .containsEntry("totalSuccess", 1L)
                        .containsEntry("avgDurationSec", 0.0));

        List<Map<String, Object>> trend = service.trend(42L, 3, "2026-06-12", "2026-06-14");
        assertThat(trend).extracting(row -> row.get("date"))
                .containsExactly("2026-06-12", "2026-06-13", "2026-06-14");
        assertThat(trend).extracting(row -> row.get("count"))
                .containsExactly(0L, 0L, 1L);

        assertThat(service.receipts(42L))
                .containsEntry("delivered", 1L)
                .containsEntry("failed", 0L);
        assertThat(service.attributionSummary(42L))
                .containsEntry("conversions", 0L)
                .containsEntry("conversionAmount", "0")
                .containsEntry("attributedSends", 0L)
                .containsEntry("model", "LAST_TOUCH")
                .containsEntry("models", "LAST_TOUCH");
    }

    @Test
    void rejectsInvalidStatsInputBeforeBuildingViews() {
        CanvasStatsFacade service = new CanvasStatsApplicationService();

        assertThatThrownBy(() -> service.trace(42L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executionId is required");
        assertThatThrownBy(() -> service.recentExecutions(42L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
        assertThatThrownBy(() -> service.stats(42L, 7, "2026-06-15", "2026-06-14"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("since must be on or before until");
        assertThatThrownBy(() -> service.trend(42L, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days must be between 1 and 365");
    }
}
