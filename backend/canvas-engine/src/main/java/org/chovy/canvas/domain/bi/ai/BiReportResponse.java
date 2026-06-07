package org.chovy.canvas.domain.bi.ai;

import java.util.List;

public record BiReportResponse(
        String status,
        boolean fallbackUsed,
        String title,
        String executiveSummary,
        List<BiReportSection> sections,
        List<String> nextActions
) {
    public BiReportResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
