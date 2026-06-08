package org.chovy.canvas.domain.bi.subscription;

/**
 * BiDeliverySchedulerResult 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param subscriptionsChecked subscriptionsChecked 字段。
 * @param subscriptionsTriggered subscriptionsTriggered 字段。
 * @param alertsChecked alertsChecked 字段。
 * @param alertsTriggered alertsTriggered 字段。
 * @param skipped skipped 字段。
 * @param failed failed 字段。
 */
public record BiDeliverySchedulerResult(
        int subscriptionsChecked,
        int subscriptionsTriggered,
        int alertsChecked,
        int alertsTriggered,
        int skipped,
        int failed
) {
}
