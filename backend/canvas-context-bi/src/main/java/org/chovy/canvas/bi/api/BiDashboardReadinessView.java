package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDashboardReadinessView(
        String status,
        boolean productionReady,
        int publishedChartCount,
        int draftDatasetCount,
        List<BiDashboardReadinessIssueView> blockers,
        List<BiDashboardReadinessIssueView> warnings
) {
    public BiDashboardReadinessView {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
