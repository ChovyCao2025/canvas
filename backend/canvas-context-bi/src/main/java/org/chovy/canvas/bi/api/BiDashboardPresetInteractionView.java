package org.chovy.canvas.bi.api;

public record BiDashboardPresetInteractionView(
        String interactionKey,
        String sourceWidgetKey,
        String targetWidgetKey,
        String interactionType,
        String fieldKey,
        String target) {
}
