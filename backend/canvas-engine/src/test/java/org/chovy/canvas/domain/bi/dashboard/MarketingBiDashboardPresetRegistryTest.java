package org.chovy.canvas.domain.bi.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingBiDashboardPresetRegistryTest {

    @Test
    void exposesCanvasEffectDashboardWithQuickBiLikeInteractions() {
        BiDashboardPreset preset = MarketingBiDashboardPresetRegistry.preset("canvas-effect");

        assertThat(preset.dashboardKey()).isEqualTo("canvas-effect");
        assertThat(preset.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(preset.widgets()).extracting(BiDashboardWidget::chartType)
                .contains("KPI_CARD", "LINE", "BAR", "TABLE");
        assertThat(preset.filters()).extracting(BiDashboardFilter::controlType)
                .contains("DATE_RANGE", "SEARCH_SELECT");
        assertThat(preset.filters()).filteredOn(filter -> filter.filterKey().equals("filter-canvas"))
                .singleElement()
                .extracting(filter -> filter.cascade().parentFilterKeys())
                .isEqualTo(java.util.List.of("filter-stat-date"));
        assertThat(preset.filters()).filteredOn(filter -> filter.filterKey().equals("filter-trigger-type"))
                .singleElement()
                .extracting(filter -> filter.cascade().parentFilterKeys())
                .isEqualTo(java.util.List.of("filter-stat-date", "filter-canvas"));
        assertThat(preset.interactions()).extracting(BiDashboardInteraction::interactionType)
                .contains("FILTER_LINKAGE", "DRILL_DOWN", "HYPERLINK");
        assertThat(preset.subscriptionChannels()).contains("EMAIL", "LARK", "WEBHOOK");
        assertThat(preset.embedScopes()).contains("INTERNAL_CANVAS", "EXTERNAL_TICKET");
    }
}
