package org.chovy.canvas.bi.api;
/**
 * BiDatasourceConnectionTestResult 结果。
 */
public record BiDatasourceConnectionTestResult(
        /**
         * dataSourceConfigId 对应的标识。
         */
        Long dataSourceConfigId,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * success 对应的数据集合。
         */
        boolean success,
        /**
         * message 字段值。
         */
        String message,
        long latencyMs) {
}
