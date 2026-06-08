package org.chovy.canvas.domain.bi.dashboard;

/**
 * BiDashboardInteraction 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param interactionKey interactionKey 字段。
 * @param sourceWidgetKey sourceWidgetKey 字段。
 * @param targetWidgetKey targetWidgetKey 字段。
 * @param interactionType interactionType 字段。
 * @param fieldKey fieldKey 字段。
 * @param target target 字段。
 */
public record BiDashboardInteraction(
        String interactionKey,
        String sourceWidgetKey,
        String targetWidgetKey,
        String interactionType,
        String fieldKey,
        String target
) {
}
