package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

/**
 * BiReportSectionInput 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param title title 字段。
 * @param query query 字段。
 * @param result result 字段。
 */
public record BiReportSectionInput(
        String title,
        BiQueryRequest query,
        BiQueryResult result
) {
}
