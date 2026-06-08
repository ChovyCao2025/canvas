package org.chovy.canvas.domain.bi.ai;

import java.util.List;

/**
 * BiReportResponse 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param title title 字段。
 * @param executiveSummary executiveSummary 字段。
 * @param sections sections 字段。
 * @param nextActions nextActions 字段。
 */
public record BiReportResponse(
        String status,
        boolean fallbackUsed,
        String title,
        String executiveSummary,
        List<BiReportSection> sections,
        List<String> nextActions
) {
    public BiReportResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
