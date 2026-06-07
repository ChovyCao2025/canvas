package org.chovy.canvas.domain.marketing;

import java.util.Map;

public interface MarketingIntegrationContractProbeClient {

    ProbeResult probe(ProbeTarget target);

    record ProbeTarget(
            Long id,
            Long tenantId,
            String contractKey,
            String displayName,
            String providerFamily,
            String apiRoot,
            String authMode,
            Integer timeoutMs,
            Map<String, Object> schemaContract,
            Map<String, Object> metadata) {
    }

    record ProbeResult(
            String status,
            Integer httpStatusCode,
            Long latencyMs,
            String problemTypeUri,
            String errorMessage,
            String summary,
            Map<String, Object> evidence) {
    }
}
