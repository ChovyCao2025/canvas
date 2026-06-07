package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorInferenceQuery(
        Long itemId,
        String sentimentLabel,
        String modelKey,
        String providerStatus,
        Boolean fallbackUsed,
        int limit) {
}
