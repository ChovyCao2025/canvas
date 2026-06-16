package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiQuickEngineTenantPoolPolicyView 视图。
 */
public record BiQuickEngineTenantPoolPolicyView(
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
        /**
         * poolWeight 字段值。
         */
        Integer poolWeight,
        /**
         * updatedBy 字段值。
         */
        String updatedBy,
        LocalDateTime updatedAt) {
}
