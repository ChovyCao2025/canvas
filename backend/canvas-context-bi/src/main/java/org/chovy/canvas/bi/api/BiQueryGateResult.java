package org.chovy.canvas.bi.api;
/**
 * BiQueryGateResult 结果。
 */
public record BiQueryGateResult(
        /**
         * allowed 字段值。
         */
        boolean allowed,
        /**
         * 状态值。
         */
        String status,
        /**
         * reason 字段值。
         */
        String reason,
        BiQueryResultView queryResult) {
}
