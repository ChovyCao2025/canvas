package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

public record MarketingIntegrationContractProbeRunView(
        Long id,
        Long tenantId,
        Long contractId,
        String contractKey,
        String providerFamily,
        String environment,
        String probeKey,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String problemTypeUri,
        String errorMessage,
        String summary,
        Map<String, Object> evidence,
        String observedAt,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
