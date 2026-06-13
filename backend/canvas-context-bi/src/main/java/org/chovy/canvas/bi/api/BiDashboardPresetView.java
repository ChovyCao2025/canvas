package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDashboardPresetView(
        String dashboardKey,
        String title,
        String description,
        String datasetKey,
        List<BiDashboardPresetWidgetView> widgets,
        List<BiDashboardPresetFilterView> filters,
        List<BiDashboardPresetInteractionView> interactions,
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
