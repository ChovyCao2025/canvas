package org.chovy.canvas.bi.api;
/**
 * BiQueryCancelResult 结果。
 */
public record BiQueryCancelResult(
        /**
         * sqlHash 字段值。
         */
        String sqlHash,
        /**
         * cancelled 字段值。
         */
        boolean cancelled,
        String status) {
}
