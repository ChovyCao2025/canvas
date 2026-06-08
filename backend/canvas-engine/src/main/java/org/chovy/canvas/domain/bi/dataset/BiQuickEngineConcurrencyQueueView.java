package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineConcurrencyQueueView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param runningQueries runningQueries 字段。
 * @param queuedQueries queuedQueries 字段。
 * @param blockedQueries blockedQueries 字段。
 * @param successfulQueries successfulQueries 字段。
 * @param failedQueries failedQueries 字段。
 * @param concurrencyUsagePercent concurrencyUsagePercent 字段。
 * @param queueUsagePercent queueUsagePercent 字段。
 * @param state state 字段。
 */
public record BiQuickEngineConcurrencyQueueView(
        int runningQueries,
        int queuedQueries,
        int blockedQueries,
        int successfulQueries,
        int failedQueries,
        double concurrencyUsagePercent,
        double queueUsagePercent,
        String state) {
}
