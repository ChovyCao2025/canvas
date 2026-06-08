package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

/**
 * BiAskDataResponse 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param question question 字段。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param explanation explanation 字段。
 * @param query query 字段。
 * @param result result 字段。
 */
public record BiAskDataResponse(
        String question,
        String status,
        boolean fallbackUsed,
        String explanation,
        BiQueryRequest query,
        BiQueryResult result
) {
}
