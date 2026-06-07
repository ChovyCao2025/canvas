package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record MarketingIntegrationContractProbeRunCommand(
        String probeKey,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String problemTypeUri,
        String errorMessage,
        String summary,
        Map<String, Object> evidence) {
}
