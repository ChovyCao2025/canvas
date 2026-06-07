package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.Map;

public record BiInterpretationRequest(
        String question,
        String subjectType,
        String subjectKey,
        BiQueryRequest query,
        BiQueryResult result,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiInterpretationRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
