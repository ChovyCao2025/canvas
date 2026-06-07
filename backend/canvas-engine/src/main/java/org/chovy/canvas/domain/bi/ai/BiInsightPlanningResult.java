package org.chovy.canvas.domain.bi.ai;

public record BiInsightPlanningResult(
        String status,
        boolean fallbackUsed,
        BiInsightPlan plan
) {
    public BiInsightPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
