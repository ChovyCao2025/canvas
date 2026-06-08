package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryCancellationResult 承载 domain.bi.query 场景中的不可变数据快照。
 * @param sqlHash sqlHash 字段。
 * @param cancelled cancelled 字段。
 * @param message message 字段。
 */
public record BiQueryCancellationResult(
        String sqlHash,
        boolean cancelled,
        String message
) {
}
