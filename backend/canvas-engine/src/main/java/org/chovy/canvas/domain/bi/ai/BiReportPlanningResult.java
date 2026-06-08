package org.chovy.canvas.domain.bi.ai;

/**
 * BiReportPlanningResult 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param plan plan 字段。
 */
public record BiReportPlanningResult(
        String status,
        boolean fallbackUsed,
        BiReportPlan plan
) {
    public BiReportPlanningResult {
        status = status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }
}
