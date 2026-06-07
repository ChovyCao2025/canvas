package org.chovy.canvas.domain.bi.ai;

import java.util.List;

public record BiReportPlan(
        String title,
        String executiveSummary,
        List<BiReportSection> sections,
        List<String> nextActions
) {
    public BiReportPlan {
        sections = sections == null ? List.of() : List.copyOf(sections);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
