package org.chovy.canvas.bi.api;
/**
 * BiDeliveryAttachmentCleanupResult 结果。
 */
public record BiDeliveryAttachmentCleanupResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * expired 字段值。
         */
        int expired,
        /**
         * filesDeleted 字段值。
         */
        int filesDeleted,
        int failed) {
}
