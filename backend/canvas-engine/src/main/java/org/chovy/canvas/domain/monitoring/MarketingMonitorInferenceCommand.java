package org.chovy.canvas.domain.monitoring;

import java.util.Map;

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
