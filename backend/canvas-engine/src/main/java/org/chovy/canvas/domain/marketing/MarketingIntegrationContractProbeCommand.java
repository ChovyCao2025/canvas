package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

public record MarketingIntegrationContractProbeCommand(
        String probeKey,
        String environment,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String errorType,
        String problemTypeUri,
        String problemTitle,
        String problemDetail,
        LocalDateTime observedAt,
        Map<String, Object> evidence) {
}
