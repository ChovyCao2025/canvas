package org.chovy.canvas.domain.bi.ai;

/**
 * BiInsightPlanningResult 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param plan plan 字段。
 */
public record BiInsightPlanningResult(
        String status,
        boolean fallbackUsed,
        BiInsightPlan plan
) {
    public BiInsightPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
