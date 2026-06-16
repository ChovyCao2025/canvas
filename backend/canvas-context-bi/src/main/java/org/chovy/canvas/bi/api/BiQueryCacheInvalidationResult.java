package org.chovy.canvas.bi.api;
/**
 * BiQueryCacheInvalidationResult 结果。
 */
public record BiQueryCacheInvalidationResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * invalidated 字段值。
         */
        int invalidated,
        String status) {
}
