package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiQuickEngineTenantPoolPolicyView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param poolKey poolKey 字段。
 * @param maxConcurrentQueries maxConcurrentQueries 字段。
 * @param queueLimit queueLimit 字段。
 * @param queueTimeoutSeconds queueTimeoutSeconds 字段。
 * @param poolWeight poolWeight 字段。
 * @param updatedBy updatedBy 字段。
 * @param updatedAt updatedAt 字段。
 */
public record BiQuickEngineTenantPoolPolicyView(
        String poolKey,
        int maxConcurrentQueries,
        int queueLimit,
        int queueTimeoutSeconds,
        int poolWeight,
        String updatedBy,
        LocalDateTime updatedAt) {
}
