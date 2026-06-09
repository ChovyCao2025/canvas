package org.chovy.canvas.domain.monitoring;

import java.util.Map;

/**
 * MarketingMonitorInferenceCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param itemId itemId 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param forceFallback forceFallback 字段。
 * @param params params 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorInferenceCommand(
        Long itemId,
        Long providerId,
        Long templateId,
        String modelKey,
        String modelVersion,
        Boolean forceFallback,
        Map<String, Object> params,
        Integer timeoutMs,
        Map<String, Object> metadata) {
}
