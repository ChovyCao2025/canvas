package org.chovy.canvas.bi.api;
/**
 * BiDashboardPresetInteractionView 视图。
 */
public record BiDashboardPresetInteractionView(
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
