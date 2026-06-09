package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorInferenceQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param itemId itemId 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param modelKey modelKey 字段。
 * @param providerStatus providerStatus 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorInferenceQuery(
        Long itemId,
        String sentimentLabel,
        String modelKey,
        String providerStatus,
        Boolean fallbackUsed,
        int limit) {
}
