package org.chovy.canvas.domain.bi.ai;

public record BiReportPlanningResult(
        String status,
        boolean fallbackUsed,
        BiReportPlan plan
) {
    public BiReportPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
