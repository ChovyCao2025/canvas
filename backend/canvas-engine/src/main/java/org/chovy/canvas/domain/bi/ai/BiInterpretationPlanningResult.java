package org.chovy.canvas.domain.bi.ai;

public record BiInterpretationPlanningResult(
        String status,
        boolean fallbackUsed,
        BiInterpretationPlan plan
) {
    public BiInterpretationPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
