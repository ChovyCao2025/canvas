package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

public final class MarketingBiDashboardPresetRegistry {

    private static final BiDashboardPreset CANVAS_EFFECT = new BiDashboardPreset(
            "canvas-effect",
            "画布效果分析",
            "面向营销画布运营的 QuickBI-like 预置看板，覆盖执行总览、趋势、排行、明细、联动、钻取、跳转、订阅和嵌入。",
            "canvas_daily_stats",
            List.of(
                    new BiDashboardWidget(
                            "kpi-total-executions",
                            "执行次数",
                            "KPI_CARD",
                            List.of(),
                            List.of("total_executions"),
                            0,
                            0,
                            6,
                            3,
                            "emphasis"),
                    new BiDashboardWidget(
                            "kpi-success-rate",
                            "执行成功率",
                            "KPI_CARD",
                            List.of(),
                            List.of("success_rate"),
                            6,
                            0,
                            6,
                            3,
                            "health"),
                    new BiDashboardWidget(
                            "trend-executions",
                            "执行趋势",
                            "LINE",
                            List.of("stat_date"),
                            List.of("total_executions", "success_count", "fail_count"),
                            0,
                            3,
                            12,
                            6,
                            "time-series"),
                    new BiDashboardWidget(
                            "rank-canvas",
                            "画布排行",
                            "BAR",
                            List.of("canvas_name"),
                            List.of("total_executions", "success_rate"),
                            12,
                            3,
                            8,
                            6,
                            "ranking"),
                    new BiDashboardWidget(
                            "detail-canvas",
                            "画布明细",
                            "TABLE",
                            List.of("stat_date", "canvas_name", "trigger_type"),
                            List.of("total_executions", "success_count", "fail_count", "unique_users", "avg_duration_ms"),
                            0,
                            9,
                            20,
                            7,
                            "detail")
            ),
            List.of(
                    new BiDashboardFilter("filter-stat-date", "stat_date", "统计日期", "DATE_RANGE", true, "LAST_7_DAYS"),
                    new BiDashboardFilter("filter-canvas", "canvas_name", "画布名称", "SEARCH_SELECT", false, null),
                    new BiDashboardFilter("filter-trigger-type", "trigger_type", "触发方式", "ENUM_MULTI_SELECT", false, null)
            ),
            List.of(
                    new BiDashboardInteraction(
                            "linkage-trend-to-detail",
                            "trend-executions",
                            "detail-canvas",
                            "FILTER_LINKAGE",
                            "stat_date",
                            null),
                    new BiDashboardInteraction(
                            "drill-rank-canvas",
                            "rank-canvas",
                            "detail-canvas",
                            "DRILL_DOWN",
                            "canvas_name",
                            "canvas_name"),
                    new BiDashboardInteraction(
                            "open-canvas-stats",
                            "detail-canvas",
                            null,
                            "HYPERLINK",
                            "canvas_id",
                            "/canvas/{canvas_id}/stats")
            ),
            List.of("EMAIL", "LARK", "WEBHOOK"),
            List.of("INTERNAL_CANVAS", "EXTERNAL_TICKET")
    );

    private MarketingBiDashboardPresetRegistry() {
    }

    public static List<BiDashboardPreset> presets() {
        return List.of(CANVAS_EFFECT);
    }

    public static BiDashboardPreset preset(String dashboardKey) {
        return presets().stream()
                .filter(preset -> preset.dashboardKey().equals(dashboardKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey));
    }
}
