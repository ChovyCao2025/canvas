package org.chovy.canvas.domain.bi.ai;

public record BiAskDataPlanningResult(
        String status,
        boolean fallbackUsed,
        BiAskDataPlan plan
) {
    public BiAskDataPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
