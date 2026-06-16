package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDashboardPresetView 视图。
 */
public record BiDashboardPresetView(
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
        List<BiDashboardPresetWidgetView> widgets,
        /**
         * 筛选条件。
         */
        List<BiDashboardPresetFilterView> filters,
        /**
         * interactions 对应的数据集合。
         */
        List<BiDashboardPresetInteractionView> interactions,
        /**
         * subscriptionChannels 对应的数据集合。
         */
        List<String> subscriptionChannels,
        List<String> embedScopes) {

    public BiDashboardPresetView {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        filters = filters == null ? List.of() : List.copyOf(filters);
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
        subscriptionChannels = subscriptionChannels == null ? List.of() : List.copyOf(subscriptionChannels);
        embedScopes = embedScopes == null ? List.of() : List.copyOf(embedScopes);
    }
}
