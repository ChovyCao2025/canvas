package org.chovy.canvas.bi.domain;

import java.util.List;
import java.util.Map;
/**
 * BiDashboardPresetCatalog 目录服务。
 */
public final class BiDashboardPresetCatalog {

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
                            "detail")),
            List.of(
                    new BiDashboardFilter("filter-stat-date", "stat_date", "统计日期", "DATE_RANGE", true,
                            "LAST_7_DAYS", List.of(), null, null, null, false),
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
                            new BiDashboardFilterCascade(List.of("filter-stat-date", "filter-canvas"), Map.of(),
                                    "SAME_SOURCE"),
                            null,
                            null,
                            false)),
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
                            "/canvas/{canvas_id}/stats")),
            List.of("EMAIL", "LARK", "WEBHOOK"),
            List.of("INTERNAL_CANVAS", "EXTERNAL_TICKET"));
    /**
     * 执行 presets 相关处理。
     */
    public List<BiDashboardPreset> presets(Long tenantId) {
        return List.of(CANVAS_EFFECT);
    }
    /**
     * 执行 preset 相关处理。
     */
    public BiDashboardPreset preset(Long tenantId, String dashboardKey) {
        return presets(tenantId).stream()
                .filter(preset -> preset.dashboardKey().equals(dashboardKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown BI dashboard preset: " + dashboardKey));
    }
    /**
     * BiDashboardPreset 不可变数据载体。
     */
    public record BiDashboardPreset(
            /**
             * 仪表盘键。
             */
            String dashboardKey,
            /**
             * 展示标题。
             */
            String title,
            /**
             * 说明文本。
             */
            String description,
            /**
             * 数据集键。
             */
            String datasetKey,
            /**
             * 组件列表。
             */
            List<BiDashboardWidget> widgets,
            /**
             * 筛选条件。
             */
            List<BiDashboardFilter> filters,
            /**
             * interactions 对应的数据集合。
             */
            List<BiDashboardInteraction> interactions,
            /**
             * subscriptionChannels 对应的数据集合。
             */
            List<String> subscriptionChannels,
            List<String> embedScopes) {

        public BiDashboardPreset {
            widgets = widgets == null ? List.of() : List.copyOf(widgets);
            filters = filters == null ? List.of() : List.copyOf(filters);
            interactions = interactions == null ? List.of() : List.copyOf(interactions);
            subscriptionChannels = subscriptionChannels == null ? List.of() : List.copyOf(subscriptionChannels);
            embedScopes = embedScopes == null ? List.of() : List.copyOf(embedScopes);
        }
    }
    /**
     * BiDashboardWidget 不可变数据载体。
     */
    public record BiDashboardWidget(
            /**
             * widgetKey 对应的业务键。
             */
            String widgetKey,
            /**
             * 展示标题。
             */
            String title,
            /**
             * chartType 字段值。
             */
            String chartType,
            /**
             * dimensions 对应的数据集合。
             */
            List<String> dimensions,
            /**
             * 指标列表。
             */
            List<String> metrics,
            /**
             * gridX 字段值。
             */
            int gridX,
            /**
             * gridY 字段值。
             */
            int gridY,
            /**
             * gridW 字段值。
             */
            int gridW,
            /**
             * gridH 字段值。
             */
            int gridH,
            String stylePreset) {

        public BiDashboardWidget {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            metrics = metrics == null ? List.of() : List.copyOf(metrics);
        }
    }
    /**
     * BiDashboardFilter 不可变数据载体。
     */
    public record BiDashboardFilter(
            /**
             * filterKey 对应的业务键。
             */
            String filterKey,
            /**
             * fieldKey 对应的业务键。
             */
            String fieldKey,
            /**
             * label 字段值。
             */
            String label,
            /**
             * controlType 字段值。
             */
            String controlType,
            /**
             * required 字段值。
             */
            boolean required,
            /**
             * defaultValue 字段值。
             */
            String defaultValue,
            /**
             * targetWidgetKeys 对应的数据集合。
             */
            List<String> targetWidgetKeys,
            /**
             * cascade 字段值。
             */
            BiDashboardFilterCascade cascade,
            /**
             * optionDatasetKey 对应的业务键。
             */
            String optionDatasetKey,
            /**
             * optionFieldKey 对应的业务键。
             */
            String optionFieldKey,
            boolean hidden) {

        public BiDashboardFilter {
            targetWidgetKeys = targetWidgetKeys == null ? List.of() : List.copyOf(targetWidgetKeys);
        }
    }
    /**
     * BiDashboardFilterCascade 不可变数据载体。
     */
    public record BiDashboardFilterCascade(
            /**
             * parentFilterKeys 对应的数据集合。
             */
            List<String> parentFilterKeys,
            /**
             * parentFieldMapping 字段值。
             */
            Map<String, String> parentFieldMapping,
            String mode) {

        public BiDashboardFilterCascade {
            parentFilterKeys = parentFilterKeys == null ? List.of() : List.copyOf(parentFilterKeys);
            parentFieldMapping = parentFieldMapping == null ? Map.of() : Map.copyOf(parentFieldMapping);
            mode = mode == null || mode.isBlank() ? "SAME_SOURCE" : mode;
        }
    }
    /**
     * BiDashboardInteraction 不可变数据载体。
     */
    public record BiDashboardInteraction(
            /**
             * interactionKey 对应的业务键。
             */
            String interactionKey,
            /**
             * sourceWidgetKey 对应的业务键。
             */
            String sourceWidgetKey,
            /**
             * targetWidgetKey 对应的业务键。
             */
            String targetWidgetKey,
            /**
             * interactionType 字段值。
             */
            String interactionType,
            /**
             * fieldKey 对应的业务键。
             */
            String fieldKey,
            String target) {
    }
}
