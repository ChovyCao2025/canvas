package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MarketingMonitorInferenceView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param itemId itemId 字段。
 * @param sourceId sourceId 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param providerStatus providerStatus 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param inputHash inputHash 字段。
 * @param promptHash promptHash 字段。
 * @param sentimentLabel sentimentLabel 字段。
 * @param sentimentScore sentimentScore 字段。
 * @param confidence confidence 字段。
 * @param entities entities 字段。
 * @param topics topics 字段。
 * @param riskFlags riskFlags 字段。
 * @param evidence evidence 字段。
 * @param latencyMs latencyMs 字段。
 * @param requestedBy requestedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorInferenceView(
        Long id,
        Long tenantId,
        Long itemId,
        Long sourceId,
        Long providerId,
        Long templateId,
        String modelKey,
        String modelVersion,
        String providerStatus,
        boolean fallbackUsed,
        String inputHash,
        String promptHash,
        String sentimentLabel,
        BigDecimal sentimentScore,
        BigDecimal confidence,
        List<Map<String, Object>> entities,
        List<String> topics,
        List<String> riskFlags,
        Map<String, Object> evidence,
        long latencyMs,
        String requestedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
