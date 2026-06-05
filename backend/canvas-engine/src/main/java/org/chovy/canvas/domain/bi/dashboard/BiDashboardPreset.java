package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

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
