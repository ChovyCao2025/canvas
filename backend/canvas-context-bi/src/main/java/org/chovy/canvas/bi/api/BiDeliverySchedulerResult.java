package org.chovy.canvas.bi.api;
/**
 * BiDeliverySchedulerResult 结果。
 */
public record BiDeliverySchedulerResult(
        /**
         * subscriptionsChecked 字段值。
         */
        int subscriptionsChecked,
        /**
         * subscriptionsTriggered 字段值。
         */
        int subscriptionsTriggered,
        /**
         * alertsChecked 字段值。
         */
        int alertsChecked,
        /**
         * alertsTriggered 字段值。
         */
        int alertsTriggered,
        /**
         * skipped 字段值。
         */
        int skipped,
        int failed) {
}
