package org.chovy.canvas.domain.bi.ai;

import java.util.Map;

public record BiAskDataRequest(
        String question,
        String datasetKey,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        int limit,
        Map<String, Object> params
) {
    public BiAskDataRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
