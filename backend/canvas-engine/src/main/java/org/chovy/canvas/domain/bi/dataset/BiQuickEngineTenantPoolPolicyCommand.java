package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineTenantPoolPolicyCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param poolKey poolKey 字段。
 * @param maxConcurrentQueries maxConcurrentQueries 字段。
 * @param queueLimit queueLimit 字段。
 * @param queueTimeoutSeconds queueTimeoutSeconds 字段。
 * @param poolWeight poolWeight 字段。
 */
public record BiQuickEngineTenantPoolPolicyCommand(
        String poolKey,
        Integer maxConcurrentQueries,
        Integer queueLimit,
        Integer queueTimeoutSeconds,
        Integer poolWeight) {
}
