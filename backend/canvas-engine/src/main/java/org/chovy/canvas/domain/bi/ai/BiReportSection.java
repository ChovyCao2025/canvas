package org.chovy.canvas.domain.bi.ai;

/**
 * BiReportSection 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param title title 字段。
 * @param body body 字段。
 */
public record BiReportSection(
        String title,
        String body
) {
}
