package org.chovy.canvas.domain.bi.ai;

import java.util.List;
import java.util.Map;

public record BiReportRequest(
        String reportType,
        String title,
        List<BiReportSectionInput> sections,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiReportRequest {
        sections = sections == null ? List.of() : List.copyOf(sections);
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
