package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

/**
 * BiDashboardPreset 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param dashboardKey dashboardKey 字段。
 * @param title title 字段。
 * @param description description 字段。
 * @param datasetKey datasetKey 字段。
 * @param widgets widgets 字段。
 * @param filters filters 字段。
 * @param interactions interactions 字段。
 * @param subscriptionChannels subscriptionChannels 字段。
 * @param embedScopes embedScopes 字段。
 */
public record BiDashboardPreset(
        String dashboardKey,
        String title,
        String description,
        String datasetKey,
        List<BiDashboardWidget> widgets,
        List<BiDashboardFilter> filters,
        List<BiDashboardInteraction> interactions,
        List<String> subscriptionChannels,
        List<String> embedScopes
) {
    public BiDashboardPreset {
        widgets = List.copyOf(widgets);
        filters = List.copyOf(filters);
        interactions = List.copyOf(interactions);
        subscriptionChannels = List.copyOf(subscriptionChannels);
        embedScopes = List.copyOf(embedScopes);
    }
}
