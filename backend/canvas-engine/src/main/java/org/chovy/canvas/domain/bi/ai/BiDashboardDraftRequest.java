package org.chovy.canvas.domain.bi.ai;

import java.util.Map;

public record BiDashboardDraftRequest(
        String prompt,
        String datasetKey,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiDashboardDraftRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
