package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiAiRequestCommand(
        String question,
        String prompt,
        String datasetKey,
        String resourceType,
        String resourceKey,
        String reportType,
        String title,
        Integer limit,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params,
        List<Map<String, Object>> sections,
        Map<String, Object> subject,
        Map<String, Object> result,
        Map<String, Object> metrics,
        Map<String, Object> context) {
}
