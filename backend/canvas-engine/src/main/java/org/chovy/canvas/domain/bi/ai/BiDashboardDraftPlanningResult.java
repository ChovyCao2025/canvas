package org.chovy.canvas.domain.bi.ai;

public record BiDashboardDraftPlanningResult(
        String status,
        boolean fallbackUsed,
        BiDashboardDraftPlan plan
) {
    public BiDashboardDraftPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
