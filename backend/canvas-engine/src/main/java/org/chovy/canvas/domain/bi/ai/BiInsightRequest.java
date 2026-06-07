package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.Map;

public record BiInsightRequest(
        String question,
        BiQueryRequest query,
        BiQueryResult currentResult,
        BiQueryResult baselineResult,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiInsightRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
