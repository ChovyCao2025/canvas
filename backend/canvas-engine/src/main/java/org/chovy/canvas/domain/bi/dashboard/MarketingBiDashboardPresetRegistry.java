package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;
import java.util.Map;

/**
 * MarketingBiDashboardPresetRegistry 编排 domain.bi.dashboard 场景的领域业务规则。
 */
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
                    new BiDashboardFilter(
                            "filter-canvas",
                            "canvas_name",
                            "画布名称",
                            "SEARCH_SELECT",
                            false,
                            null,
                            List.of(),
                            new BiDashboardFilterCascade(List.of("filter-stat-date"), Map.of(), "SAME_SOURCE"),
                            null,
                            null,
                            false),
                    new BiDashboardFilter(
                            "filter-trigger-type",
                            "trigger_type",
                            "触发方式",
                            "ENUM_MULTI_SELECT",
                            false,
                            null,
                            List.of(),
                            new BiDashboardFilterCascade(List.of("filter-stat-date", "filter-canvas"), Map.of(), "SAME_SOURCE"),
                            null,
                            null,
                            false)
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

    /**
     * 执行 MarketingBiDashboardPresetRegistry 流程，围绕 marketing bi dashboard preset registry 完成校验、计算或结果组装。
     */
    private MarketingBiDashboardPresetRegistry() {
    }

    /**
     * 返回平台内置的营销 BI 看板预置清单。
     *
     * <p>当前预置只包含画布效果分析看板，清单中的数据集 key、组件布局、筛选级联、交互跳转和订阅渠道
     * 都用于创建租户工作区的初始看板草稿；调用方仍需要在资源服务侧完成租户、权限和版本落库处理。</p>
     *
     * @return 不可变的内置看板预置列表
     */
    public static List<BiDashboardPreset> presets() {
        return List.of(CANVAS_EFFECT);
    }

    /**
     * 按看板 key 读取单个内置预置。
     *
     * @param dashboardKey 预置看板的稳定业务 key，例如 {@code canvas-effect}
     * @return 匹配的预置定义，包含默认组件、筛选器、联动和订阅能力
     * @throws IllegalArgumentException 当 key 不在内置注册表中时抛出，避免创建未知数据口径的看板
     */
    public static BiDashboardPreset preset(String dashboardKey) {
        return presets().stream()
                .filter(preset -> preset.dashboardKey().equals(dashboardKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey));
    }
}
