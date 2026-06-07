package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
