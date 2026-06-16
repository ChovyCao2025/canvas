package org.chovy.canvas.bi.api;
/**
 * BiSelfServiceExportCleanupResult 结果。
 */
public record BiSelfServiceExportCleanupResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * removed 字段值。
         */
        int removed,
        int retained) {
}
