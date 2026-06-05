package org.chovy.canvas.domain.bi.dashboard;

public record BiDashboardInteraction(
        String interactionKey,
        String sourceWidgetKey,
        String targetWidgetKey,
        String interactionType,
        String fieldKey,
        String target
) {
}
