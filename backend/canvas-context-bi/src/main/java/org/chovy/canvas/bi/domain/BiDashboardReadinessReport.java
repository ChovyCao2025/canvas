package org.chovy.canvas.bi.domain;

import java.util.List;

public record BiDashboardReadinessReport(
        String status,
        boolean productionReady,
        int publishedChartCount,
        int draftDatasetCount,
        List<BiDashboardReadinessIssue> blockers,
        List<BiDashboardReadinessIssue> warnings
) {
    public BiDashboardReadinessReport {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
