package org.chovy.canvas.domain.bi.subscription;

/**
 * BiDeliveryAttachmentCleanupResult 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param checked checked 字段。
 * @param expired expired 字段。
 * @param filesDeleted filesDeleted 字段。
 * @param failed failed 字段。
 */
public record BiDeliveryAttachmentCleanupResult(
        int checked,
        int expired,
        int filesDeleted,
        int failed
) {
}
