package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorInferenceGenerationResult 承载 domain.monitoring 场景中的不可变数据快照。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param providerStatus providerStatus 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param sentimentScore sentimentScore 字段。
 * @param confidence confidence 字段。
 * @param entities entities 字段。
 * @param topics topics 字段。
 * @param riskFlags riskFlags 字段。
 * @param evidence evidence 字段。
 * @param latencyMs latencyMs 字段。
 */
public record MarketingMonitorInferenceGenerationResult(
        Long providerId,
        Long templateId,
        String modelKey,
        String modelVersion,
        String providerStatus,
        boolean fallbackUsed,
        String sentimentLabel,
        BigDecimal sentimentScore,
        BigDecimal confidence,
        List<Map<String, Object>> entities,
        List<String> topics,
        List<String> riskFlags,
        Map<String, Object> evidence,
        long latencyMs) {
}
