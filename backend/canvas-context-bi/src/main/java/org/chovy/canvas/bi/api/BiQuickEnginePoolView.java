package org.chovy.canvas.bi.api;
/**
 * BiQuickEnginePoolView 视图。
 */
public record BiQuickEnginePoolView(
        /**
         * poolKey 对应的业务键。
         */
        String poolKey,
        /**
         * maxConcurrentQueries 对应的数据集合。
         */
        Integer maxConcurrentQueries,
        /**
         * queueLimit 字段值。
         */
        Integer queueLimit,
        /**
         * queueTimeoutSeconds 对应的数据集合。
         */
        Integer queueTimeoutSeconds,
        Integer poolWeight) {
}
