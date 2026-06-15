package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiAiResponseView(
        Long tenantId,
        String actor,
        String operation,
        String assistantRunId,
        String question,
        String status,
        Boolean fallbackUsed,
        String explanation,
        Map<String, Object> metadata,
        String summary,
        List<String> keyFindings,
        List<String> recommendations,
        String title,
        List<Map<String, Object>> sections,
        List<String> nextActions,
        Map<String, Object> dashboard,
        List<Map<String, Object>> charts,
        List<String> trends,
        List<String> anomalies,
        List<String> opportunities) {
}
