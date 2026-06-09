package org.chovy.canvas.domain.marketing;

/**
 * GrowthActivityReadinessCheckView 承载 domain.marketing 场景中的不可变数据快照。
 * @param severity severity 字段。
 * @param itemType itemType 字段。
 * @param itemKey itemKey 字段。
 * @param title title 字段。
 * @param reason reason 字段。
 * @param route route 字段。
 */
public record GrowthActivityReadinessCheckView(
        String severity,
        String itemType,
        String itemKey,
        String title,
        String reason,
        String route) {
}
