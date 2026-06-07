package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
